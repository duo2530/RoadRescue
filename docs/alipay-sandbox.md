# uni-app H5 支付宝沙箱支付

H5 使用官方 Java SDK 的 `alipay.trade.wap.pay` 跳转支付宝网页收银台。这里只启用沙箱网关，不进行真实资金交易。小程序保留 `WECHAT_MINI` 渠道，当前明确返回未启用，不调用微信支付。

## 使用

1. 启动 `service-order`、`service-payment`、`web-customer`、`web-driver` 和网关，乘客端使用 `VITE_API_MODE=real`。
2. 司机确认送达后，订单进入“待付款”；旧的“结束服务”订单显示“待结算”，首次支付时核对并发布账单。
3. 乘客在“服务查询 → 进行中服务 → 订单详情”点击“支付宝沙箱支付”，使用该沙箱应用的沙箱买家账号完成支付。
4. 支付宝回跳订单详情，前端向后端查询交易。服务端核对支付宝的交易状态、订单号、交易号和金额后，才把订单改为已付款；取消支付或关闭收银台保持待付款。也可点击“我已完成支付，查询结果”。

## 本地配置

将配置填入被 Git 忽略的 `backend/.env`，格式见 `backend/.env.example`。私钥和公钥保持单行 Base64；不要提交密钥。启动脚本会把这些值传给 Java 进程。

| 变量 | 用途 |
| --- | --- |
| `ALIPAY_SANDBOX_ENABLED` | `true` 启用沙箱 |
| `ALIPAY_GATEWAY_URL` | `https://openapi-sandbox.dl.alipaydev.com/gateway.do` |
| `ALIPAY_APP_ID` | 沙箱应用 APPID |
| `ALIPAY_PRIVATE_KEY` | 沙箱应用私钥，与应用公钥配对 |
| `ALIPAY_PUBLIC_KEY` | 支付宝沙箱公钥，用于验签 |
| `ALIPAY_SELLER_ID` | 可选，配置后额外校验通知收款方 |
| `ALIPAY_NOTIFY_URL` | 可选，当前项目公网 HTTPS 通知地址 |
| `ALIPAY_H5_RETURN_URL` | 默认 `http://localhost:9090/#/pages/order/detail` |
| `ALIPAY_ALLOWED_RETURN_ORIGINS` | 允许的 H5 来源，逗号分隔，默认允许 localhost/127.0.0.1 的 9090 端口 |

本地无需公网地址即可通过服务端主动查询确认付款。配置异步通知时，地址必须能从支付宝服务器访问，例如 `https://你的域名/payment/alipay/notify`，并路由到当前项目网关 8600。不能直接复用另一个项目的通知地址。没有公网通知时，关闭页面后需要重新进入详情或手动查询才能同步支付结果。

使用手机访问局域网 IP 或部署到域名时，同时把该 H5 来源加入 `ALIPAY_ALLOWED_RETURN_ORIGINS`。前端只提交来源；服务端白名单校验后构造固定的订单详情回跳路径，不接受任意跳转 URL。

## 接口与数据

- `POST /customer-api/order/payment/create`：`{ orderNo, channel: "ALIPAY_WAP_SANDBOX", returnOrigin }`，返回沙箱 `redirectUrl`。金额和乘客身份均由服务端决定。
- `GET /customer-api/order/payment/status/{orderNo}`：校验当前乘客归属，并主动向支付宝查询。仅返回已确认的支付结果。
- `POST /payment/alipay/notify`：公开异步通知入口，RSA2 验签，并校验 APPID、金额、订单号和交易号。重复通知幂等。
- 创建/查询支付的微服务内部接口位于 `/internal/alipay/**`，不通过网关的 `/payment/**` 路由公开。
- `WECHAT_MINI` 请求返回 501，旧微信创建/查询入口同样关闭。未来启用微信支付时应单独完成商户配置、真实金额计费与回调验签；当前历史微信代码中的测试金额不能直接用于上线。

订单状态：6 结束服务（旧订单待结算）、7 待付款、8 已付款、9 已完成。6/7 均属于乘客“进行中服务”，只有确认付款后才显示“实付金额”。支付记录使用现有 `payment_info` 表，`pay_way=2`（兼容原有 tinyint 列）；订单号唯一，无需新建表。

救援价格统一由 `RescuePricing` 计算：拖车 180 元含 10 公里，超出 45 元/公里；搭电 139 元、换胎 199 元、紧急充电 350 元，另加账单中的路桥费、停车费等。客户端提交的 `price` 不作为结算依据。旧的代驾账单仅在首次发布付款账单时校正；已发布的账单金额保持稳定。支付宝支付目前只确认收款与订单付款状态，不执行历史微信分账流程。

## 验证

```powershell
# 乘客端：支付状态、H5 跳转、服务端结果核验、微信入口关闭
cd apps/passenger-uniapp
npm run test:payment
npm run build:h5
npm run build:mp-weixin

# 后端：签名/验签、错误金额、错误应用、重复通知、查询确认、身份隔离、旧账单校正
cd ../../backend/daijia-parent
mvn -pl service/service-payment,service/service-order,web/web-customer -am test '-DskipTests=false' '-Dtest=AlipayPaymentServiceTest,AlipayGatewayTest,RescueBillTest,PaymentIdentityTest' '-Dsurefire.failIfNoSpecifiedTests=false'
```

单元测试使用独立生成的 RSA 密钥及模拟支付宝响应验证成功/失败分支，不代表已完成外部沙箱买家扣款。完整外部联调需要在收银台使用沙箱买家账号付款，再确认回跳后的订单和支付记录均为已付款。

参考：[支付宝官方 Java SDK](https://github.com/alipay/alipay-sdk-java-all/tree/master/v2)、[官方网页支付接口说明](https://developer.alibaba.com/docs/api.htm?apiId=1056&docType=4)。
