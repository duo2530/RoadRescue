// 应用全局配置
// apiMode：
//   mock —— 接口由 /mock 在前端模拟，数据存在本地 storage（离线演示）
//   real —— 订单/登录/优惠券对接乐尚代驾后端（经 server-gateway 的 /customer-api），
//           后端没有的能力（车辆管理、服务项目）仍走本地 mock，见 mock/real-adapter.js
// 构建时可用 .env 的 VITE_API_MODE / VITE_API_BASE 覆盖
const apiMode = import.meta.env.VITE_API_MODE || 'mock'

export default {
  apiMode,
  useMock: apiMode === 'mock',
  // H5 开发时走 vite 代理（见 vite.config.js），真机/小程序填网关地址
  baseUrl: import.meta.env.VITE_API_BASE || '/customer-api',
  // 腾讯地图 WebService key，构建时从 .env 的 TECENT_MAP_KEY 注入
  qqMapKey: import.meta.env.TECENT_MAP_KEY || '',
  // 应用信息
  appInfo: {
    name: '中道救援',
    version: '1.0.0',
    // 客服电话
    servicePhone: '021-66898194'
  }
}
