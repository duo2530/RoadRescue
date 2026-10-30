# 启动全部依赖服务（后台常驻，保留数据卷）
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..\..'))
docker compose -f infra\docker-compose.yml up -d
docker compose -f infra\docker-compose.yml ps
