# 中道救援 overnight brief（调研 + 实施计划）

## 目标
面试作品：道路救援（中道救援）全栈项目，落在 D:\Projects\IDEAProjects\zhongdao-rescue。

## 素材（只读参考，勿改原仓作为交付）
1. UI Mock：D:\Projects\ClaudeProjects\中道救援mock
   - 用户端 uni-app/（H5 默认 9090，mock localStorage）
   - 司机端 driver-uni-app/（大厅/认证/订单/我的）
2. 业务后端：D:\Projects\IDEAProjects\atguigu-daijia
   - daijia-parent 微服务（gateway + web-customer/driver/mgr + service-*）
   - infra/ docker-compose、scripts/、daijia.sql
   - 小程序 mp-weixin-customer / mp-weixin-driver **仅作 API 对接参考，UI 以 Mock 为准**
3. 重要发现：web-mgr 已有管理端 BFF（订单/乘客/司机/系统用户角色菜单/登录），但 **没有 PC 管理前端**。网关路径 /mgr-api/**，端口 web-mgr=8603。

## 行业管理端模块（调研摘要）
参考道路救援 SaaS（救援宝、算数科技道路救援）与 Vue3 Element Admin：
1. 工作台/看板：成单量、响应时效、在线司机、异常单
2. 工单/订单：列表、详情、状态流转、轨迹
3. 调度：派单池、就近司机、人工改派（可先接现有 dispatch）
4. 司机管理：认证、出车状态、车辆
5. 用户/乘客管理
6. 财务：订单金额、对账简表（可先读 payment/order 字段）
7. 系统：用户/角色/菜单（web-mgr + service-system 已有）

技术选型：**Vue3 + Vite + Element Plus** 管理端；移动端继续 **uni-app**。

## 推荐目录
`
zhongdao-rescue/
  README.md
  LICENSE
  docs/overnight-brief.md          # 本文件，可增补
  backend/                         # 从 atguigu-daijia 拷贝适配（daijia-parent/infra/scripts/sql）
  apps/
    passenger-uniapp/              # 来自 mock uni-app，对接真实 API
    driver-uniapp/                 # 来自 mock driver-uni-app
    admin-web/                     # 新建 Vue3+Element Plus，对接 /mgr-api
  evidence/agent-browser/          # 验收截图 PNG
`

## 实施优先级（今晚必须尽量完成）
P0 脚手架：复制后端与两端 UI 进本仓；gitignore 密钥；README 说明面试卖点与启动
P0 管理端：登录 + 仪表盘 + 订单列表/详情 + 司机列表 + 乘客列表 + 系统用户（对接 web-mgr）
P1 乘客/司机端：把 mock 的 useMock 切到可对接真实 backend 的配置；关键页保留 Mock UI
P1 本地能起：复用/改编 start-all；至少 gateway+web-mgr+依赖能支撑 admin 演示；完整 14 服务若内存紧可文档说明
P2 agent-browser：admin 登录后关键页、乘客 H5、司机 H5 截图到 evidence/agent-browser/（清掉登录过期弹窗）
P2 gh repo create **private** 推送（账号 duo2530 已登录）

## 硬约束
- 不要改写原 mock / atguigu-daijia 作为交付；只在 zhongdao-rescue 内改
- 密钥进 .env.example，不进 git（.env、私钥、WX 商户密钥）
- 不要 AskUserQuestion；合理默认
- 卡住满 3 次：提交已有进度并 private push
- 全程复用同一 Claude session
