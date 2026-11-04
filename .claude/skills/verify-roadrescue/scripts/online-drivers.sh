#!/usr/bin/env bash
# 让若干演示司机走真实接口上线：手机号验证码登录 -> 开启接单 -> 上报位置。
# 用法：online-drivers.sh <中心纬度> <中心经度>，默认乘客端默认定位（芈展服务园）。
# 本地没有腾讯云人脸核身，所以先在库里把这些司机标为已认证、今日已人脸识别；其余全部走网关。
set -euo pipefail
LAT=${1:-31.238096}
LNG=${2:-121.653299}
G=http://127.0.0.1:8600
# 手机号后缀 + 相对中心的纬度/经度偏移（约 0.3 到 1 公里），手机号为 1390000xxxx
DRIVERS=(
  "1001 0.0031 -0.0043"
  "1002 -0.0042 0.0028"
  "1003 0.0058 0.0047"
  "1004 -0.0019 -0.0081"
)

json() { python -X utf8 -c "import sys,json;print(json.load(sys.stdin)['$1'])"; }
sql() { docker exec roadrescue-mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" -N daijia_driver -e \"$1\"" 2>/dev/null; }

for row in "${DRIVERS[@]}"; do
  read -r suffix dlat dlng <<<"$row"
  phone="1390000$suffix"
  curl -s -H 'Content-Type: application/json' -d "{\"phone\":\"$phone\"}" $G/driver-api/driver/sms/send >/dev/null
  token=$(curl -s -H 'Content-Type: application/json' -d "{\"phone\":\"$phone\",\"code\":\"123456\"}" $G/driver-api/driver/loginByPhone | json data)
  id=$(sql "select id from driver_info where phone='$phone'")
  sql "update driver_info set auth_status=2 where id=$id; insert into driver_face_recognition (driver_id, face_date) select $id, CURDATE() from dual where not exists (select 1 from driver_face_recognition where driver_id=$id and face_date=CURDATE())"
  start=$(curl -s -H "token: $token" $G/driver-api/driver/startService)
  lat=$(python -c "print(round($LAT + $dlat, 6))"); lng=$(python -c "print(round($LNG + $dlng, 6))")
  loc=$(curl -s -H "token: $token" -H 'Content-Type: application/json' -d "{\"latitude\":$lat,\"longitude\":$lng}" $G/driver-api/location/updateDriverLocation)
  echo "driver $id ($phone) at $lat,$lng startService=$start updateDriverLocation=$loc"
done
