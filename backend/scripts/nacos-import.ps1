# 把 daijia-parent\DEFAULT_GROUP 下的配置推送到本地 Nacos
# 用法：pwsh -File scripts\nacos-import.ps1
param(
    [string]$NacosAddr = 'http://127.0.0.1:8848',
    [string]$ConfigDir = (Join-Path $PSScriptRoot '..\daijia-parent\DEFAULT_GROUP'),
    [string]$Group     = 'DEFAULT_GROUP'
)
$ErrorActionPreference = 'Stop'
$ConfigDir = (Resolve-Path $ConfigDir).Path

# 等 Nacos 就绪
$ok = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        $r = curl.exe -s -m 3 "$NacosAddr/nacos/v1/console/health/readiness" 2>$null
        if ($r -match 'ok|UP|true') { $ok = $true; break }
    } catch { }
    Start-Sleep -Seconds 2
}
if (-not $ok) { throw "Nacos 未就绪：$NacosAddr" }
Write-Host "Nacos 已就绪：$NacosAddr"

$files = Get-ChildItem $ConfigDir -Filter *.yaml | Sort-Object Name
$failed = @()
foreach ($f in $files) {
    $resp = curl.exe -s -X POST "$NacosAddr/nacos/v1/cs/configs" `
        --data-urlencode "dataId=$($f.Name)" `
        --data-urlencode "group=$Group" `
        --data-urlencode "type=yaml" `
        --data-urlencode "content@$($f.FullName)"
    if ($resp -ne 'true') { $failed += "$($f.Name) -> $resp" }
    Write-Host ("{0,-30} {1}" -f $f.Name, $resp)
}

if ($failed.Count -gt 0) {
    Write-Host "推送失败：" -ForegroundColor Red
    $failed | ForEach-Object { Write-Host "  $_" -ForegroundColor Red }
    exit 1
}

# 回读校验
Write-Host "`n回读校验："
foreach ($f in $files) {
    $body = curl.exe -s "$NacosAddr/nacos/v1/cs/configs?dataId=$($f.Name)&group=$Group"
    $len = if ($body) { $body.Length } else { 0 }
    Write-Host ("{0,-30} {1} 字符" -f $f.Name, $len)
}
Write-Host "`n全部 $($files.Count) 个配置已导入 Nacos。" -ForegroundColor Green
