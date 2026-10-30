import request from '@/utils/request'

/**
 * 发送登录验证码
 * POST /sms/code  body: { phone }  =>  { hint }（演示版返回固定验证码提示）
 */
export function sendSmsCode(phone) {
  return request({ url: '/sms/code', method: 'post', data: { phone }, headers: { isToken: false } })
}

/**
 * 手机号验证码登录（后端可替换为微信手机号快捷登录）
 * POST /login  body: { phone, code }  =>  { token }
 */
export function login(data) {
  return request({ url: '/login', method: 'post', data, headers: { isToken: false } })
}

/**
 * 司机信息（个人中心）
 * GET /driver/info  =>  {
 *   driverId, nickname, phone, avatar,
 *   stats: { orderCount 总计订单, totalAmount 已审核运费, unauditedAmount 未审核运费 },
 *   wallet: { balance 余额, freight 运费, deposit 保证金 },
 *   auth: [{ key, title, business, done }]
 * }
 */
export function getDriverInfo() {
  return request({ url: '/driver/info', method: 'get' })
}

/** POST /logout */
export function logout() {
  return request({ url: '/logout', method: 'post' })
}

/** 演示专用：清空 mock 数据库恢复初始数据（真实后端无此接口） */
export function resetMock() {
  return request({ url: '/mock/reset', method: 'post' })
}
