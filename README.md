# 道路救援 (Road Rescue)

道路救援全栈面试作品：乘客端 + 救援司机端（uni-app）+ PC 调度管理端（Vue 3），后端基于乐尚代驾 Spring Cloud 微服务改造。

使用步骤见 [操作手册](docs/操作手册.md)，实拍截图位于 `docs/image/`；支付配置见 [支付宝沙箱支付](docs/alipay-sandbox.md)。

- 乘客端、司机端沿用「道路救援」Mock 版 UI。`VITE_API_MODE=real` 时经适配层接入真实后端，后端没有的能力回落到本地 mock。
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
docker exec -i roadrescue-mysql mysql -uroot -p<密码> < backend/sql/test-data.sql

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
| 乘客端 | 登录页点「登录/注册」展开，点「演示账号」行填入手机号和验证码 123456，勾选协议后登录（real 模式走 `/customer/sms/send`、`/customer/loginByPhone`，手机号对应 customer_id=1） |
| 司机端 | 登录页点「演示账号」填入后登录（real 模式走 `/driver/demoLogin` → driver_id=1） |

乘客端的验证码固定为 Nacos `web-customer-dev.yaml` 里的 `roadrescue.sms.demo-code`，尚未接短信服务商，上生产前必须清空。`/customer/demoLogin` 仍保留，供接口层取 token。

演示登录只在 Nacos 配置 `roadrescue.demo-login.enabled=true` 时注册，用来替代 H5 无法调用的 `wx.login`。

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
- 派单：平台搜索范围由 `roadrescue.dispatch.search-radius-km` 配置，默认 100 公里，再按司机的 `order_distance` 和 `accept_distance` 筛选。演示司机不限订单里程、接单距离为 50 公里；大厅“附近订单”还会按所选 10/30/50/100 公里范围过滤。
- 司机大厅每 5 秒刷新派单快照；查询不会清空待接单订单，已接单或取消的订单会移除。队列有效期与派单去重记录均为 16 分钟，返回数据包含救援类型、价格和坐标。
- 司机详情允许收到派单的司机预览待抢单订单，联系方式在接单后返回；其他司机已接订单仍禁止访问。详情的 `mine` 字段区分待抢单与当前司机已接订单。
- 抢单按订单独立加锁，并以数据库“待接单且未分配司机”的条件更新确认归属；Redis 接单标记按订单隔离。司机接口会透传抢单失败，成功后详情立即切换为“待发车”。
- 字符集：`daijia.sql` 开头补 `SET NAMES utf8mb4`，避免导入时中文被双重编码。
- 密钥：`common-account.yaml` 改成从环境变量读取，删除支付私钥 pem；`scripts/load-env.cmd` 在启动时加载 `backend/.env`。

## 截图

`evidence/agent-browser/`：

- 管理端：`admin-01-login` … `admin-09-order-detail`
- 乘客端（real 模式）：`passenger-01-home`、`02-orders`、`03-order-detail`、`04-mine`
- 司机端（real 模式）：`driver-01-login`、`02-hall`（派单队列中的真实订单）、`03-grab`、`04-my-orders`、`05-order-detail`、`06-mine`

## 已知缺口

- H5 已接入支付宝网页沙箱支付；配置及联调见 [支付宝沙箱支付](docs/alipay-sandbox.md)。小程序真实微信支付接口预留且关闭，H5 登录仍使用演示账号。
- 救援报价和结算统一由服务端计算；结束服务进入待付款，支付宝通知验签或服务端主动查询确认后才更新为已付款。支付宝分账尚未接入。
- `order_info` 没有救援专用字段（服务类型、车型、车牌），暂存在 `remark` JSON 里。
- 乘客端的车辆、服务项目、兑换码，以及司机端的短信、分级认证、钱包流水，仍走本地 mock。
- 司机端「取消接单」没有对应后端接口，real 模式下提示联系调度改派。
- 腾讯云人脸识别未接入，`test-data.sql` 预置了当天的人脸记录。

## License

MIT
