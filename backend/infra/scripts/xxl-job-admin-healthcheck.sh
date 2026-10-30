#!/bin/bash
# xxl-job-admin 容器内健康检查：
# 镜像基于 JRE，没有 curl/wget，因此用 bash 的 /dev/tcp 直接发一个 HTTP/1.0 请求。
# 注意：直接请求 /xxl-job-admin/ 会 302 跳转到登录页，所以要请求 /xxl-job-admin/toLogin，
# 登录页返回 200 才认为健康。
exec 3<>/dev/tcp/127.0.0.1/8080 || exit 1
printf 'GET /xxl-job-admin/toLogin HTTP/1.0\r\nHost: 127.0.0.1\r\n\r\n' >&3
IFS= read -r -t 5 line <&3 || exit 1
case "$line" in
  *" 200 "*) exit 0 ;;
  *) exit 1 ;;
esac
