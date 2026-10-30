import request from '@/utils/request'

/**
 * GET /coupon/list?status=unused|used|expired
 * =>  [{ id, name, value, threshold, status, receiveTime, expireTime }]
 */
export function listCoupon(status) {
  return request({ url: '/coupon/list', method: 'get', params: { status } })
}

/** POST /coupon/redeem  body: { code }  =>  Coupon */
export function redeemCoupon(code) {
  return request({ url: '/coupon/redeem', method: 'post', data: { code } })
}
