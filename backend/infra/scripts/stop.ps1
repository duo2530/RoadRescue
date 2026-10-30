# 停止全部依赖服务（保留数据卷；如需清数据用 docker compose -f infra\docker-compose.yml down -v）
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..\..'))
docker compose -f infra\docker-compose.yml stop
