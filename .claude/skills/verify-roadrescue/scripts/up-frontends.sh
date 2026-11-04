#!/usr/bin/env bash
# 后台启动三个前端。端口已返回 200 的视为别人起的，跳过且不记录。
cd "$(dirname "$0")/../../../.." || exit 1
mkdir -p logs/verify

start() { # dir script port
  if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:$3/")" = "200" ]; then
    echo "port $3 已在运行，跳过"; return
  fi
  (cd "apps/$1" && nohup npm run "$2" > "../../logs/verify/$1.log" 2>&1 </dev/null &)
}

start admin-web dev 9527
start passenger-uniapp dev:h5 9090
start driver-uniapp dev:h5 9091

rc=0
for p in 9527 9090 9091; do
  ready=0
  for i in $(seq 1 45); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:$p/")" = "200" ]; then ready=1; break; fi
    sleep 2
  done
  if [ $ready = 1 ]; then echo "port $p ready"; else echo "port $p NOT ready，见 logs/verify/*.log"; rc=1; fi
done
exit $rc
