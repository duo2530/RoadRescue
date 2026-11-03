---
name: verify-roadrescue
description: 驱动并验证「道路救援服务平台」的真实运行效果：Spring Cloud 后端、管理端（Vue 3，9527）、乘客端和司机端（uni-app H5，9090/9091）。改了后端接口、前端页面、下单/派单/抢单链路，或需要截图和接口证据证明功能可用时使用。
---

# verify-roadrescue

所有命令从仓库根目录执行，用 Git Bash，路径写正斜杠。证据放在 `logs/verify/<RUN_ID>/`（已被 .gitignore 忽略，清理不会删它）。`RUN_ID` 取 `$(date +%Y%m%d-%H%M%S)`。

## Launch

依赖：Docker Desktop、JDK 17（`D:\Develop\Java\jdk-17`）、Maven、Node。三个前端的 `.env` 已是 `VITE_API_MODE=real`。

1. **中间件**（六个容器，前缀 `roadrescue-`）：
   `docker compose -f backend/infra/docker-compose.yml up -d`，直到 `docker ps --filter name=roadrescue- --filter health=healthy` 有 6 行。
   - `backend/infra/artifacts/xxl-job-admin.jar` 必须存在（不入库）。缺失时从 `D:\Projects\IDEAProjects\atguigu-daijia\infra\artifacts\xxl-job-admin.jar` 复制。
   - 不要执行 `down -v`，会删数据卷。
2. **Nacos 配置**（全新 Nacos 卷才需要）：`powershell -NoProfile -ExecutionPolicy Bypass -File backend/scripts/nacos-import.ps1`，末行应是“全部 15 个配置已导入”。
3. **演示数据**（全新 MySQL 卷才需要）：
   `docker cp backend/sql/test-data.sql roadrescue-mysql:/tmp/t.sql`
   `docker exec roadrescue-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/t.sql'`
4. **后端 jar**：改过 Java 代码或 `roadrescue.*` 配置前缀后必须重编，旧 jar 读不到新配置（曾因此 demoLogin 返回 404）。先停服务再编，否则 Windows 下 repackage 因 jar 被占用失败：
   `bash -c 'cd backend/daijia-parent && JAVA_HOME=/d/Develop/Java/jdk-17 PATH=/d/Develop/Java/jdk-17/bin:$PATH mvn -q -DskipTests -o install'`
5. **启动后端**：`powershell -NoProfile -ExecutionPolicy Bypass -File backend/scripts/start-all.ps1`，约 60 到 90 秒。就绪标准是 `curl http://127.0.0.1:8600/customer-api/customer/demoLogin` 返回 `"code":200`。返回 503 表示网关已起、服务还没注册完。
6. **前端**：`bash .claude/skills/verify-roadrescue/scripts/up-frontends.sh`，三个端口都返回 200 才算就绪。必须用 `localhost`，`127.0.0.1` 不通（Vite 绑在 IPv6）。

## Doctor

`bash .claude/skills/verify-roadrescue/scripts/doctor.sh`，只读，退出码 0 为全部通过。它检查六个容器 healthy、管理端登录接口、两个 demoLogin、三个前端首页，并列出 9090/9091/9527 的监听进程。`tailscaled` 也会监听这三个端口的 Tailscale 地址，属于正常现象，不要杀。任一项 FAIL 先修再驱动。

## Drive

用 `agent-browser`（先运行 `agent-browser skills get core`）。会话独占：`export AGENT_BROWSER_SESSION=rr-verify-$RUN_ID`。每个会话的第一条命令不要接管道，用 `agent-browser open <url> >/dev/null 2>&1 </dev/null`，否则会卡住。

- **管理端（9527）**：Element Plus，可访问性名称完整。
  - 登录页：`textbox "用户名"`、`textbox "密码"`、`button "登 录"`（“登”和“录”之间有空格），表单预填 admin / 111111。
  - 侧栏：`menuitem "调度看板"`、`"救援工单"`、`"救援师傅"`、`"求助用户"`、`"系统管理"`。
  - 列表表头如 `columnheader "工单号"`。
- **乘客端（9090）和司机端（9091）**：uni-app 的 `view` 没有角色，`snapshot -i` 基本是空的。先 `agent-browser set viewport 430 932`。用 `agent-browser eval 'document.body.innerText'` 读文字做断言，点击按可见文案。
- **接口层**（验证后端时优先用，比 UI 稳）：网关 `http://127.0.0.1:8600`。
  - 乘客 token：`GET /customer-api/customer/demoLogin`。
  - 司机 token：`GET /driver-api/driver/demoLogin`。
  - 管理端 token：`POST /mgr-api/securityLogin/login`，body `{"username":"admin","password":"111111"}`。
  - 之后每个请求带请求头 `token: <token>`。
  - 完整链路冒烟：`powershell -NoProfile -ExecutionPolicy Bypass -File backend/scripts/smoke-test.ps1`，会真实下一单并抢单。
- 各功能的具体步骤见 `features/README.md`。

## Evidence

每个功能一个目录：`logs/verify/$RUN_ID/<feature-id>/`。

- 截图：`agent-browser screenshot <path>.png`，画面里要能看到“道路救援”品牌或页面标题。
- 页面文字：管理端用 `agent-browser snapshot -i > xxx.aria.txt`，H5 端用 `eval 'document.body.innerText' > xxx.txt`。
- 接口响应体：`curl ... > xxx.json`。
- 数据库回读：
  `docker exec roadrescue-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "select id,order_no,status,driver_id from daijia_order.order_info order by id desc limit 3"'`

证明标准：

- 走真实用户路径（UI 点击，或前端实际调用的网关接口），不调内部 setter。
- 记录动作和结果，不只截最后一屏。
- 有写入就用第二个视角回读，例如 UI 下单后查库。
- uni-app 在 real 模式下，车辆、兑换码、短信、钱包流水仍走本地 mock。这些页面显示成功不算后端证明，证据里要标注。

## Cleanup

`bash .claude/skills/verify-roadrescue/scripts/cleanup.sh`：关闭 agent-browser 会话，停止监听 9090/9091/9527 的 node 进程（按端口属主判断，`tailscaled` 被排除），运行 `backend/scripts/stop-all.ps1`，再 `docker compose stop`（不是 down，不删卷）。不会删除 `logs/verify/`。不要按进程名批量杀 node 或 java，本机还有其他项目的容器和进程。

验证期间下的订单会留在 `daijia_order.order_info`。需要还原演示数据时重跑 `backend/sql/test-data.sql`，不要清空整库。

## Helpers

- `scripts/doctor.sh`：只读体检，无参数。
- `scripts/up-frontends.sh`：后台启动三个前端并等待就绪，无参数，已在运行的端口会跳过。日志在 `logs/verify/<app>.log`。
- `scripts/cleanup.sh`：见上，无参数。
