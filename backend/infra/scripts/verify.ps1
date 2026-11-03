# 验收脚本：检查全部依赖服务健康状态与连接能力
# 用法：pwsh -File infra\scripts\verify.ps1   （或 powershell -File ...）
$ErrorActionPreference = 'Continue'
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
Set-Location $root
$compose = 'infra\docker-compose.yml'
$fail = 0
function Section($t) { Write-Host "`n=== $t ===" }

Section 'docker compose ps'
docker compose -f $compose ps

Section 'MySQL：业务库 + xxl_job 表数量'
docker exec roadrescue-mysql mysql -uroot -pshangguigu@QY123 -N -e "SELECT table_schema, COUNT(*) AS tables FROM information_schema.tables WHERE table_schema LIKE 'daijia%' OR table_schema='xxl_job' GROUP BY table_schema ORDER BY table_schema;" 2>$null

Section 'Redis：PING'
docker exec roadrescue-redis redis-cli ping

Section 'RabbitMQ：延迟插件 + 管理页登录（admin/admin123）'
docker exec roadrescue-rabbitmq rabbitmq-plugins list -e | Select-String 'delayed'
$auth = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes('admin:admin123'))
try {
    (Invoke-RestMethod -Uri 'http://localhost:15672/api/whoami' -Headers @{Authorization = $auth} -TimeoutSec 10) | ConvertTo-Json -Compress
} catch {
    Write-Host "FAIL rabbitmq: $($_.Exception.Message)"; $fail++
}

Section 'MongoDB：ping'
docker exec roadrescue-mongo mongosh --quiet --eval "db.runCommand({ping:1})" 2>$null

Section 'Nacos：readiness'
try {
    $r = Invoke-WebRequest -Uri 'http://127.0.0.1:8848/nacos/v1/console/health/readiness' -UseBasicParsing -TimeoutSec 10
    Write-Host "HTTP $($r.StatusCode) $($r.Content)"
} catch {
    Write-Host "FAIL nacos: $($_.Exception.Message)"; $fail++
}

Section 'XXL-JOB admin：登录页 200'
try {
    $x = Invoke-WebRequest -Uri 'http://127.0.0.1:8080/xxl-job-admin/toLogin' -UseBasicParsing -TimeoutSec 20
    Write-Host "HTTP $($x.StatusCode) content-length=$($x.Content.Length)"
} catch {
    Write-Host "FAIL xxl-job: $($_.Exception.Message)"; $fail++
}

Write-Host ''
if ($fail -gt 0) { Write-Host "检查失败项: $fail"; exit 1 } else { Write-Host '全部检查通过' }
