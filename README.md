# 中道救援 (Zhongdao Rescue)

道路救援全栈面试作品：乘客端 + 救援司机端（uni-app）+ PC 调度管理端（Vue 3），后端基于乐尚代驾 Spring Cloud 微服务改造。

- 乘客端、司机端沿用「中道救援」Mock 版 UI。`VITE_API_MODE=real` 时经适配层接入真实后端，后端没有的能力回落到本地 mock。
- 管理端是新写的，对接 `web-mgr`（`/mgr-api`），包含登录、数据看板、订单、司机审核、乘客、系统用户/角色/菜单。
- 后端链路已用真实数据跑通：乘客下单 → XXL-JOB 派单到附近司机 → 司机抢单（Redisson 锁）→ 到达 → 验车 → 开始救援 → 上传轨迹（MongoDB）→ 结束救援，Drools 计费与分账。

## 目录

```
backend/
  daijia-parent/      Spring Cloud 微服务（gateway 8600、web-customer/driver/mgr、service-*）
    DEFAULT_GROUP/    Nacos 配置（scripts/nacos-import.ps1 导入）
  infra/              docker-compose：MySQL、Redis、Nacos、RabbitMQ、MongoDB、XXL-JOB、MinIO
  scripts/            start-all / stop-all / nacos-import / smoke-test / test-data.sql
  sql/                daijia.sql（业务库）、tables_xxl_job.sql、test-data.sql
  .env.example        微信、腾讯云密钥模板（复制为 .env，已 gitignore）
apps/
  admin-web/          Vue 3 + Vite + Element Plus + Pinia，端口 9527
  passenger-uniapp/   乘客端，H5 端口 9090
  driver-uniapp/      司机端，H5 端口 9091
evidence/agent-browser/  关键页截图
```

## 启动

前置：JDK 17、Maven 3.9、Node 18+、Docker Desktop。

```powershell
# 1. 中间件（首次启动自动导入 sql/daijia.sql 与 tables_xxl_job.sql）
docker compose -f backend/infra/docker-compose.yml up -d

# 2. 密钥与 Nacos 配置
copy backend\.env.example backend\.env      # 填入腾讯地图 key 等；不填则地图/人脸相关接口不可用
powershell backend/scripts/nacos-import.ps1

# 3. 演示数据（司机认证、接单设置、当日人脸记录）
docker exec -i daijia-mysql mysql -uroot -p<密码> < backend/sql/test-data.sql

# 4. 编译并启动全部服务
cd backend/daijia-parent; mvn -DskipTests install; cd ../..
powershell backend/scripts/start-all.ps1

# 5. 前端
cd apps/admin-web; npm i; npm run dev                 # http://localhost:9527
cd apps/passenger-uniapp; npm i; npm run dev:h5       # http://localhost:9090
cd apps/driver-uniapp; npm i; npm run dev:h5          # http://localhost:9091
```

uni-app 的 `.env` 里设置 `VITE_API_MODE=real`（默认 `mock`，不需要后端也能演示），`VITE_GATEWAY` 默认是 `http://127.0.0.1:8600`。

XXL-JOB 控制台 http://localhost:8080/xxl-job-admin：需要确认派单任务（新订单派单 job）已启动。

## 演示账号

| 端 | 方式 |
| --- | --- |
| 管理端 | admin / 111111 |
| 乘客端 | 打开即自动演示登录（`/customer/demoLogin` → customer_id=1） |
| 司机端 | 登录页点「演示账号」填入后登录（real 模式走 `/driver/demoLogin` → driver_id=1） |

演示登录只在 Nacos 配置 `zhongdao.demo-login.enabled=true` 时注册，用来替代 H5 无法调用的 `wx.login`。

## 前端 → 后端接口对照

| 页面约定（Mock） | 真实接口 |
| --- | --- |
| 乘客 `POST /order` | `POST /customer-api/order/submitOrder`（救援信息存 `remark` JSON） |
| 乘客 `GET /order/list`、`/order/:id` | `findCustomerOrderPage`、`getOrderInfo` |
| 乘客 `POST /order/:id/cancel` | `GET /order/cancelOrder/{id}`（新增，仅待接单可取消） |
| 乘客 `GET /coupon/list` | `findNoUsePage` / `findUsedPage` |
| 司机 `GET /hall/orders` | `updateDriverLocation` + `findNewOrderQueueData` |
| 司机 `POST /hall/orders/:id/grab` | `robNewOrder/{id}` |
| 司机 `POST /my/orders/:id/depart` | `updateOrderLocationToCache` → `driverArriveStartLocation` → `updateOrderCart` → `startDrive` |
| 司机 `POST /my/orders/:id/finish` | `saveOrderServiceLocation` → `endDrive` |

适配代码：`apps/*/mock/real-adapter.js`。

## 后端改动（相对乐尚代驾）

- 管理端接口：`web-mgr` 原来的订单/司机/乘客控制器是空的，这次补齐分页、详情、看板统计、司机审核、乘客启停。下游在 service-order / service-driver / service-customer 新增 `*MgrController` 和对应的 Feign Client。
- 乘客取消订单：`/order/cancelOrder/{orderId}`，校验订单归属，且只允许取消待接单的单。
- 救援信息透传：`SubmitOrderForm`、`OrderInfoVo`、`OrderListVo` 增加 `remark`，Mapper 查询带出。
- Bug 修复：`LocationServiceImpl.calculateOrderRealDistance` 把米当成公里计费（18667 公里 → 9 万多元），改为换算成公里并保留两位小数。
- 派单：演示司机的 `order_distance=0`（不限里程）、`accept_distance=50`。救援单通常比代驾单远，原来的 5 公里限制会把订单过滤掉。
- 字符集：`daijia.sql` 开头补 `SET NAMES utf8mb4`，避免导入时中文被双重编码。
- 密钥：`common-account.yaml` 改成从环境变量读取，删除支付私钥 pem；`scripts/load-env.cmd` 在启动时加载 `backend/.env`。

## 截图

`evidence/agent-browser/`：

- 管理端：`admin-01-login` … `admin-09-order-detail`
- 乘客端（real 模式）：`passenger-01-home`、`02-orders`、`03-order-detail`、`04-mine`
- 司机端（real 模式）：`driver-01-login`、`02-hall`（派单队列中的真实订单）、`03-grab`、`04-my-orders`、`05-order-detail`、`06-mine`

## 已知缺口

- 微信登录、微信支付没有接入，H5 用演示登录代替；支付流程未测试。
- 费用预估在前端按救援一口价和里程计算，后端 Drools 仍按代驾规则生成 `expectAmount`，两套口径不同。
- `order_info` 没有救援专用字段（服务类型、车型、车牌），暂存在 `remark` JSON 里。
- 乘客端的车辆、服务项目、兑换码，以及司机端的短信、分级认证、钱包流水，仍走本地 mock。
- 司机端「取消接单」没有对应后端接口，real 模式下提示联系调度改派。
- 腾讯云人脸识别未接入，`test-data.sql` 预置了当天的人脸记录。

## License

MIT
