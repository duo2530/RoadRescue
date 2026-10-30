import request from '@/utils/request'

/**
 * 地址对象 Address: { name, address, latitude, longitude }
 * 订单状态 status: waiting 待接单 / dispatched 已派车 / rescuing 救援中 / finished 已完成 / cancelled 已取消
 */

/**
 * 费用预估（自动使用可用的最大面额优惠券）
 * POST /order/estimate  body: { serviceType, distance? (公里) }
 * =>  { amount, discount, payAmount, couponName }，拖车未选终点时 amount 为 null
 */
export function estimate(data) {
  return request({ url: '/order/estimate', method: 'post', data })
}

/**
 * 提交救援
 * POST /order  body: { serviceType, startAddress, endAddress?, distance?, carType, position, plate, phone }
 * =>  Order
 */
export function createOrder(data) {
  return request({ url: '/order', method: 'post', data })
}

/** GET /order/list?type=ongoing|history  =>  [Order] */
export function listOrder(type) {
  return request({ url: '/order/list', method: 'get', params: { type } })
}

/** GET /order/:id  =>  Order（含 driver: { name, phone, plate }） */
export function getOrder(id) {
  return request({ url: '/order/' + id, method: 'get' })
}

/** POST /order/:id/cancel */
export function cancelOrder(id) {
  return request({ url: '/order/' + id + '/cancel', method: 'post' })
}
