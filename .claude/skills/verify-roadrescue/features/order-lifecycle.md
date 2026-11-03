# 救援下单到抢单

乘客提交救援申请后，订单进入待接单；司机在大厅抢单成功后订单变为已接单，乘客侧状态同步变化。

## Sub-features

- `order-expect` 按起终点预估里程和费用。
- `order-submit` 乘客提交救援订单。
- `order-rob` 司机抢单。
- `order-status` 乘客查询订单状态，司机查询当前订单。

## How to get to it (user POV)

- 乘客端 `http://localhost:9090` 首页选择服务类型，填写救援地点、车型、车牌、联系电话，点“同意协议并救援”。
- 司机端 `http://localhost:9091` 大厅的“附近订单”列表里点某单的“抢单”。
- 接口层：乘客 `POST /customer-api/order/submitOrder`，司机 `GET /driver-api/order/robNewOrder/{orderId}`。

## Driving it with curl + the repo smoke test

Preconditions:

- `doctor.sh` 通过。
- `backend/.env` 里有可用的腾讯地图 key（预估接口依赖它）。
- XXL-JOB 里的派单任务已启动，否则订单不会进入司机的待抢队列。

- **Run the chain.** `powershell -NoProfile -ExecutionPolicy Bypass -File backend/scripts/smoke-test.ps1`。输出依次是网关登录态、预估（里程、时长、金额）、下单（`orderId=...`）、状态 `1`（等待接单）、抢单成功、状态 `2`（已接单），末行“结果：通过 N 项，失败 0 项”，退出码 0。
- **Read back.** `docker exec roadrescue-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "select id,order_no,status,driver_id from daijia_order.order_info order by id desc limit 3"'`。最新一行的 `status` 为 2 且 `driver_id` 为 1。
- **Show in admin.** 在管理端“救援工单”用该工单号查询，状态与库里一致。

## Gotchas

- 冒烟脚本会用伪造 token 写入 Redis（`user:login:smoke-*`），不是真实用户登录路径。要证明 H5 登录路径，用[H5 演示登录](./demo-login.md)。
- 脚本每次运行都会新增一张真实订单，验证后订单留在库里。
- 预估或下单报地图错误时，先查 `backend/.env` 的 `TECENT_MAP_KEY`（注意拼写就是 `TECENT`），再重启 `service-map`。
- 抢单返回成功但乘客状态不变，多半是 Redisson 锁或 MQ 未就绪，先看 `logs/service-order.err.log`。
- 乘客端 UI 下单依赖浏览器定位和地图选点，H5 里默认地址是演示地址，不要把“页面提交成功”当作后端证明，要回读库。
