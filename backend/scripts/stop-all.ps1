# 停止本项目启动的全部 Spring Boot 进程
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-all.ps1
param([string]$PidFile = (Join-Path $PSScriptRoot '..\logs\pids.csv'))
$ErrorActionPreference = 'Continue'
$root   = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$parent = Join-Path $root 'daijia-parent'

# 1) 按 pids.csv 停止
if (Test-Path $PidFile) {
    Import-Csv $PidFile | ForEach-Object {
        $p = Get-Process -Id $_.pid -ErrorAction SilentlyContinue
        if ($p) { Write-Host "停止 $($_.name) pid=$($_.pid)"; Stop-Process -Id $_.pid -Force }
    }
    Remove-Item $PidFile -Force -ErrorAction SilentlyContinue
}

# 2) 再按命令行兜底，清掉分批启动/残留的 java 进程
$left = Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
        Where-Object { $_.CommandLine -like "*$parent*" }
foreach ($p in $left) {
    Write-Host "停止残留 java pid=$($p.ProcessId)"
    Stop-Process -Id $p.ProcessId -Force -ErrorAction SilentlyContinue
}
Write-Host '完成。'
