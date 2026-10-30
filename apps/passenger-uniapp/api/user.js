import request from '@/utils/request'

/**
 * 登录（演示版自动登录；后端可替换为微信 code / 手机号验证码登录）
 * POST /login  body: { phone?, code? }  =>  { token }
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
