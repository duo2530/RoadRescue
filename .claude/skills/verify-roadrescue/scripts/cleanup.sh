#!/usr/bin/env bash
# 关闭本次验证起的东西。不删证据 logs/verify/，不 down -v，不按进程名批量杀。
cd "$(dirname "$0")/../../../.." || exit 1

agent-browser close --all >/dev/null 2>&1 </dev/null

# 前端：只停「监听 9090/9091/9527 且进程名是 node.exe」的进程。
# uni.js 的命令行是相对路径，不能靠路径匹配；tailscaled 也监听这三个端口，必须排除。
powershell -NoProfile -Command '$ids = Get-NetTCPConnection -State Listen -LocalPort 9090,9091,9527 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique; foreach($id in $ids){ $p = Get-Process -Id $id -ErrorAction SilentlyContinue; if($p -and $p.ProcessName -eq "node"){ "停止前端 node pid=$id"; Stop-Process -Id $id -Force -ErrorAction SilentlyContinue } }'

powershell -NoProfile -ExecutionPolicy Bypass -File backend/scripts/stop-all.ps1
docker compose -f backend/infra/docker-compose.yml stop
echo "cleanup done; evidence kept in logs/verify/"
