# 启动 daijia-parent 全部 14 个 Spring Boot 进程（本地化运行，带堆内存上限）
# 用法（Windows PowerShell 5.1）：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-all.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Only service-order,web-customer
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-all.ps1 -HeapMb 512   # 覆盖未单独配置的模块
#
# 说明：进程通过 WMI(Win32_Process.Create) 拉起，而不是 Start-Process，
#       目的是让 java 进程脱离当前 shell/作业对象——关掉终端、杀掉调用方都不会带走服务。
param(
    [string]$JdkHome = 'D:\Develop\Java\jdk-17',
    [string[]]$Only  = @(),
    [switch]$SkipDispatch,
    [int]$HeapMb     = 256
)
$ErrorActionPreference = 'Stop'
$root   = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$parent = Join-Path $root 'daijia-parent'
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$env:JAVA_HOME = $JdkHome
$java = Join-Path $JdkHome 'bin\java.exe'
if (-not (Test-Path $java)) { throw "找不到 JDK17: $java" }

# 仅校验根目录 .env（已被 .gitignore 忽略）里有没有填 APP_ID / APP_SECRET，只打印长度，不打印值。
# 真正把它们传给 java 的是下面的 scripts\load-env.cmd（WMI 拉起的进程不继承本脚本的环境变量）。
$envFile = Join-Path $root '.env'
if (Test-Path $envFile) {
    $kv = @{}
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*?)\s*$') { $kv[$Matches[1]] = $Matches[2].Trim('"').Trim("'") }
    }
    Write-Host ("已读取 .env：APP_ID 长度={0}，APP_SECRET 长度={1}" -f "$($kv['APP_ID'])".Length, "$($kv['APP_SECRET'])".Length)
} else {
    Write-Host '未找到 .env，小程序登录将不可用（WX_APP_ID/WX_APP_SECRET 为空）' -ForegroundColor Yellow
}

# 每个模块的堆上限（MB）：按"最小够用"原则分配，合计约 4.0GB
$heapTable = @{
    'service-system'   = 320
    'service-customer' = 256
    'service-driver'   = 320
    'service-map'      = 256
    'service-rules'    = 384   # Drools 编译规则较吃内存
    'service-coupon'   = 256
    'service-order'    = 384
    'service-payment'  = 320
    'service-dispatch' = 320
    'service-mq'       = 192
    'web-customer'     = 256
    'web-driver'       = 320
    'web-mgr'          = 256
    'server-gateway'   = 192
}

# 启动顺序：基础服务 -> 业务服务 -> web 层 -> 网关
$modules = @(
    @{ name = 'service-system';   dir = 'service\service-system';   port = 8510 },
    @{ name = 'service-customer'; dir = 'service\service-customer'; port = 8501 },
    @{ name = 'service-driver';   dir = 'service\service-driver';   port = 8502 },
    @{ name = 'service-map';      dir = 'service\service-map';      port = 8503 },
    @{ name = 'service-rules';    dir = 'service\service-rules';    port = 8504 },
    @{ name = 'service-coupon';   dir = 'service\service-coupon';   port = 8511 },
    @{ name = 'service-order';    dir = 'service\service-order';    port = 8505 },
    @{ name = 'service-payment';  dir = 'service\service-payment';  port = 8506 },
    @{ name = 'service-dispatch'; dir = 'service\service-dispatch'; port = 8509 },
    @{ name = 'service-mq';       dir = 'service\service-mq';       port = 8282 },
    @{ name = 'web-customer';     dir = 'web\web-customer';         port = 8601 },
    @{ name = 'web-driver';       dir = 'web\web-driver';           port = 8602 },
    @{ name = 'web-mgr';          dir = 'web\web-mgr';              port = 8603 },
    @{ name = 'server-gateway';   dir = 'server-gateway';           port = 8600 }
)

# -Only 从命令行传入时是 "a,b,c" 单串，这里拆开
if ($Only.Count -eq 1 -and $Only[0] -match ',') { $Only = $Only[0] -split ',' }
if ($Only.Count -gt 0) { $modules = @($modules | Where-Object { $Only -contains $_.name }) }
if ($SkipDispatch)     { $modules = @($modules | Where-Object { $_.name -ne 'service-dispatch' }) }

# ShowWindow=0 (SW_HIDE)：cmd 控制台窗口隐藏，避免每个服务弹一个黑框；日志已重定向到 logs\
$startup = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ ShowWindow = [uint16]0 }

$started = @()
foreach ($m in $modules) {
    $jar = Join-Path $parent "$($m.dir)\target\$($m.name).jar"
    if (-not (Test-Path $jar)) {
        Write-Host "缺少 jar: $jar（先执行构建）" -ForegroundColor Red
        continue
    }
    $xmx = if ($heapTable.ContainsKey($m.name)) { $heapTable[$m.name] } else { $HeapMb }
    $log = Join-Path $logDir "$($m.name).log"
    $err = Join-Path $logDir "$($m.name).err.log"

    $opts = @(
        '-Xms64m', "-Xmx${xmx}m",
        '-XX:MaxMetaspaceSize=160m',
        '-XX:ReservedCodeCacheSize=64m',
        '-Xss512k',
        '-XX:+ExitOnOutOfMemoryError',
        '-Dfile.encoding=UTF-8',
        '-Duser.timezone=Asia/Shanghai',
        '-jar', $jar
    ) -join ' '

    # cmd 包裹用于重定向输出；整体通过 WMI 创建，脱离当前作业对象。
    # WMI 创建的进程不继承本脚本的环境变量，所以由 load-env.cmd 在 java 启动前读 .env 并设置 WX_APP_ID/WX_APP_SECRET，
    # 密钥因此不会出现在进程命令行里。
    $loader = Join-Path $PSScriptRoot 'load-env.cmd'
    $cmdLine = 'cmd.exe /c "call "{4}" && "{0}" {1} >> "{2}" 2>> "{3}""' -f $java, $opts, $log, $err, $loader
    $r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
        CommandLine             = $cmdLine
        CurrentDirectory        = $parent
        ProcessStartupInformation = $startup
    }
    if ($r.ReturnValue -ne 0) {
        Write-Host ("启动失败 {0}: WMI ReturnValue={1}" -f $m.name, $r.ReturnValue) -ForegroundColor Red
        continue
    }
    Write-Host ("启动 {0,-18} pid={1,-7} port={2} -Xmx{3}m" -f $m.name, $r.ProcessId, $m.port, $xmx)
    $started += [pscustomobject]@{ name = $m.name; port = $m.port; pid = $r.ProcessId; xmx_mb = $xmx }
    Start-Sleep -Milliseconds 500
}

if ($started.Count -gt 0) {
    $started | Export-Csv -Path (Join-Path $logDir 'pids.csv') -NoTypeInformation -Encoding UTF8
}
$totalHeap = ($started | Measure-Object -Property xmx_mb -Sum).Sum
Write-Host "`n已启动 $($started.Count) 个进程，堆上限合计 ${totalHeap}MB，pid 记录：logs\pids.csv" -ForegroundColor Green
Write-Host "等待注册到 Nacos（约 60-90 秒）后可访问网关 http://127.0.0.1:8600" -ForegroundColor Yellow
