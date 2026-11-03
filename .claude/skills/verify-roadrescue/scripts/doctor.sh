#!/usr/bin/env bash
# 只读体检：这个实例值不值得驱动。退出码 0 = 全通过。
fail=0
ok()  { echo "[ OK ] $1"; }
bad() { echo "[FAIL] $1"; fail=1; }
code() { curl -s -o /dev/null -w '%{http_code}' --max-time 8 "$@"; }

n=$(docker ps --filter name=roadrescue- --filter health=healthy --format '{{.Names}}' 2>/dev/null | wc -l)
if [ "$n" -ge 6 ]; then ok "中间件容器 healthy=$n"; else bad "中间件容器 healthy=$n (期望 6)：docker compose -f backend/infra/docker-compose.yml up -d"; fi

c=$(curl -s --max-time 8 -X POST http://127.0.0.1:8600/mgr-api/securityLogin/login \
    -H 'Content-Type: application/json' -d '{"username":"admin","password":"111111"}')
if echo "$c" | grep -q '"code":200'; then ok "网关 + web-mgr 登录"; else bad "管理端登录接口：$(echo "$c" | head -c 120)"; fi

for u in customer-api/customer/demoLogin driver-api/driver/demoLogin; do
  c=$(curl -s --max-time 8 "http://127.0.0.1:8600/$u")
  if echo "$c" | grep -q '"code":200'; then ok "$u"; else bad "$u：$(echo "$c" | head -c 120)（404 = jar 仍是旧配置前缀，需要重编）"; fi
done

for p in 9527:管理端 9090:乘客端 9091:司机端; do
  port=${p%%:*}; name=${p#*:}
  if [ "$(code "http://localhost:$port/")" = "200" ]; then ok "$name localhost:$port"; else bad "$name localhost:$port 不通：bash .claude/skills/verify-roadrescue/scripts/up-frontends.sh"; fi
done

# 端口属主：应为 node。tailscaled 会监听 Tailscale 地址的同号端口，属正常，忽略。
powershell -NoProfile -Command 'foreach($p in 9090,9091,9527){ Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue | ForEach-Object { $n=(Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName; "  port $p <- $n ($($_.LocalAddress))" } }'
exit $fail
