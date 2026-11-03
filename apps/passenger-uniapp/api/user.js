import request from '@/utils/request'

/** 发送短信验证码  POST /sms/code  body: { phone }  =>  { hint } */
export function sendSmsCode(phone) {
  return request({ url: '/sms/code', method: 'post', data: { phone }, headers: { isToken: false } })
}

/**
 * 手机号验证码登录，未注册的手机号自动注册
 * POST /login  body: { phone, code }  =>  { token }
 */
export function login(data) {
  return request({ url: '/login', method: 'post', data, headers: { isToken: false } })
}

/** GET /user/info  =>  { userId, phone, avatar } */
export function getUserInfo(opts = {}) {
  return request({ url: '/user/info', method: 'get', toast: opts.silent401 ? false : undefined })
}

/** POST /logout */
export function logout() {
  return request({ url: '/logout', method: 'post' })
}
