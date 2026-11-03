# 管理端登录与调度看板

调度人员用管理员账号登录 PC 管理端，通过侧栏查看看板和救援工单，并打开单个工单的详情。

## Sub-features

- `admin-login` 用账号密码登录并进入调度看板。
- `admin-nav` 侧栏在看板、工单、师傅、用户之间切换。
- `admin-orders` 工单列表按工单号和状态查询。
- `admin-order-detail` 打开工单详情。

## How to get to it (user POV)

- 浏览器打开 `http://localhost:9527`，未登录会跳到 `#/login`。
- 登录后默认进入 `#/dashboard`；侧栏点“救援工单”进入 `#/orders`。

## Driving it with agent-browser

Preconditions:

- `doctor.sh` 通过，尤其是管理端登录接口和 `localhost:9527`。
- 使用独占会话：`export AGENT_BROWSER_SESSION=rr-verify-$RUN_ID`。
- 会话第一条命令不接管道。

- **Open login.** `agent-browser open http://localhost:9527 >/dev/null 2>&1 </dev/null`，再 `agent-browser set viewport 1280 900`。`agent-browser snapshot -i` 显示 `heading "道路救援"`、`textbox "用户名"`、`textbox "密码"`、`button "登 录"`。
- **Log in.** 表单预填 admin / 111111。执行 `agent-browser find role button click --name "登 录"`。`agent-browser get url` 返回 `http://localhost:9527/#/dashboard`，快照出现 `menuitem "调度看板"`。
- **Navigate to orders.** `agent-browser find role menuitem click --name "救援工单"`。URL 变为 `#/orders`，快照出现 `columnheader "工单号"`、`columnheader "救援地点"`、`columnheader "状态"`。
- **Read rows.** `agent-browser eval 'document.querySelectorAll(".el-table__body tr").length'` 大于 0 时表示有工单。
- **Open detail.** 点击某行的 `详情` 按钮：`agent-browser find text "详情" click --first`。弹出的抽屉或对话框里有“工单号”“状态”“救援地点”等描述项。
- **Proof.** `agent-browser snapshot -i > logs/verify/$RUN_ID/admin-orders/orders.aria.txt`，`agent-browser screenshot logs/verify/$RUN_ID/admin-orders/orders.png`。回读：`curl` 带管理端 token 请求工单分页接口，条数与页面一致。

## Gotchas

- 登录按钮的名称是 `登 录`，中间有空格，写成 `登录` 找不到。
- 管理端 token 存在 `localStorage` 的 `zd_admin_token`。换账号或重来前，先 `agent-browser eval 'localStorage.clear()'`，否则访问 `/login` 会被重定向回看板。
- 工单列表为空不代表页面坏了：全新数据库没有订单。先用[救援下单到抢单](./order-lifecycle.md)造一单，再验证列表。
- 只测列表加载不够，要再打开一次详情，确认二次请求也成功。
