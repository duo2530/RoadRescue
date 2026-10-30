# 端到端冒烟测试：伪造登录态 -> 预估 -> 下单 -> 司机抢单 -> 查状态
# 前置：14 个服务已启动、Nacos 配置已导入、scripts/test-data.sql 已执行
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
param(
    [string]$Gateway = 'http://127.0.0.1:8600',
    [string]$RedisContainer = '',
    [long]$CustomerId = 1,
    [long]$DriverId = 1
)
$ErrorActionPreference = 'Stop'
$script:pass = 0; $script:fail = 0

function Step($name, $scriptblock) {
    try {
        & $scriptblock
        Write-Host ("  [PASS] " + $name) -ForegroundColor Green
        $script:pass++
    } catch {
        Write-Host ("  [FAIL] " + $name + " -> " + $_.Exception.Message) -ForegroundColor Red
        $script:fail++
    }
}
# 统一解码 UTF-8 响应，避免 PowerShell 5.1 把中文显示成乱码
function ReadJson($resp) {
    $ms = New-Object IO.MemoryStream
    $resp.RawContentStream.Position = 0
    $resp.RawContentStream.CopyTo($ms)
    [Text.Encoding]::UTF8.GetString($ms.ToArray()) | ConvertFrom-Json
}
function CallApi($method, $url, $token, $bodyObj) {
    $headers = @{}
    if ($token) { $headers['token'] = $token }
    $p = @{ Method = $method; Uri = $url; Headers = $headers; TimeoutSec = 120; UseBasicParsing = $true }
    if ($null -ne $bodyObj) {
        $json = if ($bodyObj -is [string]) { $bodyObj } else { $bodyObj | ConvertTo-Json -Depth 8 -Compress }
        $p['Body'] = [Text.Encoding]::UTF8.GetBytes($json)
        $p['ContentType'] = 'application/json; charset=utf-8'
    }
    $resp = Invoke-WebRequest @p
    ReadJson $resp
}

# ---------- 0. 定位 redis 容器并伪造登录态 ----------
# 说明：项目的 RedisTemplate value 用 GenericJackson2JsonRedisSerializer，
#      所以 token 对应的 userId 必须存成 JSON 字符串（带双引号），否则会被反序列化成 Integer 而导致 ClassCastException
$rc = $RedisContainer
if (-not $rc) {
    $rc = (docker ps --format '{{.Names}}|{{.Image}}' | Where-Object { $_ -match '\|redis:' } |
           ForEach-Object { ($_ -split '\|')[0] } | Select-Object -First 1)
}
if (-not $rc) { throw '找不到 redis 容器，请用 -RedisContainer 指定' }
$custToken = 'smoke-customer-token'
$drvToken  = 'smoke-driver-token'
foreach ($pair in @(@($custToken, $CustomerId), @($drvToken, $DriverId))) {
    $key = "user:login:$($pair[0])"
    ('"' + $pair[1] + '"') | docker exec -i $rc redis-cli -x SET $key | Out-Null
    docker exec $rc redis-cli EXPIRE $key 86400 | Out-Null
}
Write-Host "Redis 登录态已伪造（$rc, customerId=$CustomerId, driverId=$DriverId）"

# ---------- 1. 网关与登录态 ----------
Write-Host "`n[1] 网关 + 登录态"
Step 'GET /customer-api/customer/getCustomerLoginInfo' {
    $r = CallApi 'GET' "$Gateway/customer-api/customer/getCustomerLoginInfo" $custToken $null
    if ($r.code -ne 200) { throw "code=$($r.code) msg=$($r.message)" }
    Write-Host ("         -> 乘客: " + $r.data.nickname + " (id=" + $CustomerId + ")")
}

# ---------- 2. 预估（service-map -> 腾讯地图, service-rules -> Drools） ----------
$coords = @{
    startPointLongitude = 116.307490; startPointLatitude = 39.984154
    endPointLongitude   = 116.315520; endPointLatitude   = 39.976970
}
Write-Host "`n[2] 预估订单（service-map + service-rules/Drools）"
Step 'POST /customer-api/order/expectOrder' {
    $r = CallApi 'POST' "$Gateway/customer-api/order/expectOrder" $custToken $coords
    if ($r.code -ne 200) { throw "code=$($r.code) msg=$($r.message)" }
    Write-Host ("         -> 距离 " + $r.data.drivingLineVo.distance + " km, 时长 " + $r.data.drivingLineVo.duration +
                " 分钟, 预估金额 " + $r.data.feeRuleResponseVo.totalAmount + " 元")
}

# ---------- 3. 下单（service-order + service-dispatch + XXL-JOB） ----------
Write-Host "`n[3] 下单"
$script:orderId = $null
Step 'POST /customer-api/order/submitOrder' {
    $body = @{
        customerId          = $CustomerId
        startLocation       = '北京市海淀区中关村'
        startPointLongitude = 116.307490
        startPointLatitude  = 39.984154
        endLocation         = '北京市海淀区五道口'
        endPointLongitude   = 116.315520
        endPointLatitude    = 39.976970
        favourFee           = 0
    }
    $r = CallApi 'POST' "$Gateway/customer-api/order/submitOrder" $custToken $body
    if ($r.code -ne 200) { throw "code=$($r.code) msg=$($r.message)" }
    $script:orderId = $r.data
    Write-Host ("         -> orderId=" + $r.data)
}

if ($script:orderId) {
    Write-Host "`n[4] 订单状态与抢单"
    Step "GET /customer-api/order/getOrderStatus/$($script:orderId) == 1(等待接单)" {
        $r = CallApi 'GET' "$Gateway/customer-api/order/getOrderStatus/$($script:orderId)" $custToken $null
        if ($r.data -ne 1) { throw "期望 1，实际 $($r.data)" }
    }
    Step "GET /driver-api/order/robNewOrder/$($script:orderId)" {
        $r = CallApi 'GET' "$Gateway/driver-api/order/robNewOrder/$($script:orderId)" $drvToken $null
        if ($r.code -ne 200 -or -not $r.data) { throw "code=$($r.code) msg=$($r.message) data=$($r.data)" }
    }
    Step "GET /customer-api/order/getOrderStatus/$($script:orderId) == 2(已接单)" {
        $r = CallApi 'GET' "$Gateway/customer-api/order/getOrderStatus/$($script:orderId)" $custToken $null
        if ($r.data -ne 2) { throw "期望 2，实际 $($r.data)" }
    }
    Step 'GET /driver-api/order/searchDriverCurrentOrder' {
        $r = CallApi 'GET' "$Gateway/driver-api/order/searchDriverCurrentOrder" $drvToken $null
        if ($r.code -ne 200) { throw "code=$($r.code) msg=$($r.message)" }
        Write-Host ("         -> 司机当前订单 orderId=" + $r.data.orderId + ", status=" + $r.data.status)
    }
    Step 'GET /customer-api/order/getOrderInfo/{orderId}' {
        $r = CallApi 'GET' "$Gateway/customer-api/order/getOrderInfo/$($script:orderId)" $custToken $null
        if ($r.code -ne 200) { throw "code=$($r.code) msg=$($r.message)" }
        Write-Host ("         -> 订单状态=" + $r.data.status + ", 司机id=" + $r.data.driverId)
    }
}

Write-Host ("`n结果：通过 $($script:pass) 项，失败 $($script:fail) 项") -ForegroundColor $(if ($script:fail -eq 0) { 'Green' } else { 'Red' })
if ($script:fail -gt 0) { exit 1 }
