import request from '@/utils/request'

/**
 * 地点 Place: { province, label（卡片展示的“省-市”）, address, latitude, longitude }
 * 订单 Order: {
 *   id, serviceType（repair 小修 / tow 拖车 / logistics 物流）, plateColor, towType, carModel,
 *   start: Place, end: Place | null（小修无终点）, mileage（公里，小修为 null）, price（运费，元）,
 *   instant 是否即时订单, publishTime, customerName, customerPhone, remark,
 *   poolStatus（open 可抢 / taken 已安排 / mine 我已抢到）, distance（距我，公里）
 * }
 * 我的订单在 Order 基础上增加: myStatus（waiting 待发车 / shipping 运输中 / finished 已完成）,
 *   grabTime, departTime, finishTime, audited（运费是否已审核入账）
 */

/**
 * 抢单大厅
 * GET /hall/orders?scope=nearby&latitude=&longitude=&radius=10
 * GET /hall/orders?scope=all&latitude=&longitude=&startProvince=&endProvince=&publishWithin=（小时）
 * =>  [Order]
 */
export function listHallOrders(params) {
  return request({ url: '/hall/orders', method: 'get', params })
}

/**
 * 抢单。未完成所需认证时返回 code 4001，data: { missing: [步骤 key] }
 * POST /hall/orders/:id/grab  =>  Order
 */
export function grabOrder(id) {
  return request({ url: '/hall/orders/' + id + '/grab', method: 'post', toast: false })
}

/** GET /my/orders?status=waiting|shipping|finished  =>  [Order] */
export function listMyOrders(status) {
  return request({ url: '/my/orders', method: 'get', params: status ? { status } : {} })
}

/** GET /my/orders/:id  =>  Order（非我的订单时 myStatus 为 null） */
export function getOrder(id) {
  return request({ url: '/my/orders/' + id, method: 'get' })
}

/** 确认发车（待发车 -> 运输中）  POST /my/orders/:id/depart */
export function departOrder(id) {
  return request({ url: '/my/orders/' + id + '/depart', method: 'post' })
}

/** 确认送达（运输中 -> 已完成，进入运费审核）  POST /my/orders/:id/finish */
export function finishOrder(id) {
  return request({ url: '/my/orders/' + id + '/finish', method: 'post' })
}

/** 取消接单（仅待发车，订单退回大厅）  POST /my/orders/:id/cancel */
export function cancelOrder(id) {
  return request({ url: '/my/orders/' + id + '/cancel', method: 'post' })
}
