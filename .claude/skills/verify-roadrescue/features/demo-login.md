# H5 演示登录

乘客端、司机端的 H5 无法调用 `wx.login`，所以后端提供受开关控制的演示登录：访问接口即得到演示用户的 token。

## Sub-features

- `demo-customer` 乘客演示 token，对应 customer_id=1。
- `phone-login-customer` 乘客端手机号验证码登录（登录页），未注册手机号自动注册。
- `demo-driver` 司机演示 token，对应 driver_id=1。
- `demo-driver-ui` 司机端登录页用演示账号登录。

## How to get to it (user POV)

- 乘客端：打开 `http://localhost:9090`，没有 token 会跳到 `#/pages/login`。点“登录/注册”展开输入框，点“演示账号”行填入手机号、验证码 123456并勾选协议，点“手机号快捷登录”，落到首页。设置 → 退出登录 → 确定，回到登录页。
- 司机端：打开 `http://localhost:9091/#/pages/login`，点页面底部“演示账号”一行填入手机号和验证码，勾选协议，点“登录”。
- 接口：`GET /customer-api/customer/demoLogin`、`GET /driver-api/driver/demoLogin`（GET，不是 POST）。

## Driving it with curl + agent-browser

Preconditions:

- `doctor.sh` 通过，其中两个 demoLogin 返回 `"code":200`。
- 开关打开：Nacos 配置里 `roadrescue.demo-login.enabled: true`，且运行的 jar 是用新前缀编译的。

- **Customer token.** `curl -s http://127.0.0.1:8600/customer-api/customer/demoLogin`，响应 `{"code":200,"message":"成功","data":"<32 位 token>"}`。
- **Use the token.** `curl -s http://127.0.0.1:8600/customer-api/customer/getCustomerLoginInfo -H "token: <token>"`，`code` 为 200 且带昵称。不带或用错 token 返回 `code 208 未登陆`。
- **Customer phone login.** 先 `POST /customer-api/customer/sms/send`，body `{"phone":"<11位手机号>"}`，返回“演示验证码：123456”；再 `POST /customer-api/customer/loginByPhone`，body `{"phone":"…","code":"123456"}`，返回 token。60 秒内重发 code 201，错误验证码 215，连错 5 次验证码作废。中文 body 在 Windows 用 `python -X utf8` 发送，不要用 curl 直发。
- **Customer login page.** `agent-browser open http://localhost:9090/#/pages/login`，`eval` 读文字包含“登录/注册”“手机号快捷登录”。退出按钮的确认弹窗里“确定”不是 ARIA 元素，用 `eval` 找叶子节点文字为“确定”的元素 `click()`。
- **Driver token.** `curl -s http://127.0.0.1:8600/driver-api/driver/demoLogin`，格式同上。
- **Driver login page.** `agent-browser open http://localhost:9091/#/pages/login`，`eval 'document.body.innerText'` 包含“道路救援司机端”“订单又多又好，接单就上道路救援”“登录”“演示账号”。

## Gotchas

- 接口返回 404（`path: /customer/demoLogin`）表示开关没生效：要么 Nacos 配置没重新导入，要么 jar 是改名前编译的，仍在读旧前缀。重编并重启后端。
- 司机登录页的复选框 `.agree` 里含“服务协议”“隐私协议”链接，直接点整行常常点中链接并跳到文本页。点击要对准复选框 `.check`，点中后它的类名带 `on`。
- 登录按钮是无角色的 `view`，按文字“登录”可能匹配到标题区域，被 `.hero` 覆盖；用 CSS 类定位并确认未被遮挡。H5 端的完整 UI 登录路径本次没有走通，先用接口层证明登录，UI 登录需要再试一次。
- 乘客手机号登录的验证码来自 `roadrescue.sms.demo-code`，改了 Nacos 配置要重新运行 `nacos-import.ps1` 并重启 web-customer。
- 演示登录只用于本地，生产环境把开关关掉。
