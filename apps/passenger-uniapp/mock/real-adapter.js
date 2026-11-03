/**
 * apiMode=real 时的接口适配层：页面和 /api 保持 Mock 版约定不变，
 * 这里把 Mock 路由翻译成乐尚代驾 web-customer 接口（经网关 /customer-api），并把返回值转换成页面需要的结构。
 *
 * 对照：
 *   POST /sms/code         -> POST /customer/sms/send
 *   POST /login            -> POST /customer/loginByPhone（手机号 + 验证码，未注册自动注册）
 *   GET  /user/info        -> GET  /customer/getCustomerLoginInfo
 *   POST /order            -> POST /order/submitOrder（落库 + XXL-JOB 派单）
 *   GET  /order/list       -> GET  /order/findCustomerOrderPage/1/50
 *   GET  /order/:id        -> GET  /order/getOrderInfo/{id}
 *   POST /order/:id/cancel -> GET  /order/cancelOrder/{id}（道路救援新增，仅待接单可取消）
 *   GET  /coupon/list      -> GET  /coupon/findNoUsePage | findUsedPage
 * 后端没有的能力（车辆、服务项目、兑换码）继续走本地 mock；
 * 费用预估按道路救援一口价/里程规则在前端计算（乐尚代驾 Drools 规则是代驾计费，口径不同），
 * 下单后后端仍会用 Drools 生成 expectAmount，救援价存于 remark.price。
 */
import config from '@/config'
import { getToken } from '@/utils/auth'
import mockRequest from './index'
import { getService, calcPrice } from './services'
import { validateOrder } from '../utils/order-validation'

const ok = data => ({ code: 200, msg: 'success', data })

// 后端统一返回 { code, message, data }
function call(method, url, data) {
  const header = {}
  const token = getToken()
  if (token) header.token = token
  return uni.request({
    method,
    url: config.baseUrl + url,
    data,
    header,
    timeout: 30000,
    dataType: 'json'
  }).then(res => {
    const body = res.data || {}
    // 208 未登录：返回 401 交给 request.js 处理
    if (body.code === 208) return { code: 401, msg: '登录已过期' }
    return { code: body.code, msg: body.message, data: body.data }
  })
}

// 后端订单状态 -> 页面状态
function mapStatus(s) {
  if (s === 1) return 'waiting'
  if (s === 2 || s === 3) return 'dispatched'
  if (s === 4 || s === 5) return 'rescuing'
  if (s === 6) return 'settling'
  if (s === 7) return 'unpaid'
  if (s === 8 || s === 9) return 'finished'
  return 'cancelled'
}

function parseRemark(remark) {
  try {
    return remark ? JSON.parse(remark) : {}
  } catch (e) {
    return {}
  }
}

function toOrder(o) {
  const extra = parseRemark(o.remark)
  const amount = o.orderBillVo ? Number(o.orderBillVo.totalAmount)
    : Number(o.status >= 6 ? (o.amount ?? extra.price ?? 0) : (extra.price ?? o.amount ?? o.expectAmount ?? 0))
  const payAmount = o.orderBillVo ? Number(o.orderBillVo.payAmount) : Number(o.payAmount ?? amount)
  const driver = o.driverInfoVo
  return {
    id: String(o.orderId || o.id),
    orderNo: o.orderNo,
    serviceType: extra.serviceType || 'tow',
    status: mapStatus(o.status),
    createTime: new Date(String(o.createTime).replace(/-/g, '/')).getTime(),
    startAddress: { name: o.startLocation, address: o.startLocation, latitude: o.startPointLatitude, longitude: o.startPointLongitude },
    endAddress: o.endLocation && o.endLocation !== o.startLocation
      ? { name: o.endLocation, address: o.endLocation, latitude: o.endPointLatitude, longitude: o.endPointLongitude }
      : null,
    distance: extra.distance || null,
    carType: extra.carType || '-',
    position: extra.position || '-',
    plate: extra.plate || '-',
    phone: extra.phone || '-',
    amount,
    discount: Math.max(0, amount - payAmount),
    payAmount,
    driver: driver ? { name: driver.name || '救援师傅', phone: driver.phone || '', plate: o.carLicense || '' } : null
  }
}

// 只需要一个点的服务（搭电、换胎、充电）用起点作为终点，满足后端算路必填
function points(data) {
  const s = data.startAddress
  const e = data.endAddress || s
  return {
    startPointLongitude: s.longitude,
    startPointLatitude: s.latitude,
    endPointLongitude: e.longitude,
    endPointLatitude: e.latitude
  }
}

const handlers = [
  ['POST', /^\/sms\/code$/, ({ data }) => call('POST', '/customer/sms/send', { phone: data.phone }).then(r => (r.code === 200 ? ok({ hint: r.data }) : r))],
  ['POST', /^\/login$/, ({ data }) => call('POST', '/customer/loginByPhone', { phone: data.phone, code: data.code }).then(r => (r.code === 200 ? ok({ token: r.data }) : r))],
  ['GET', /^\/user\/info$/, () => call('GET', '/customer/getCustomerLoginInfo').then(r =>
    r.code === 200 ? ok({ userId: null, phone: r.data.phone || '', nickname: r.data.nickname, avatar: r.data.avatarUrl }) : r)],
  ['POST', /^\/logout$/, () => Promise.resolve(ok(null))],
  ['POST', /^\/order$/, ({ data }) => {
    const service = getService(data.serviceType)
    const error = validateOrder(data, service)
    if (error) return Promise.resolve({ code: 500, msg: error })
    const remark = JSON.stringify({
      serviceType: data.serviceType,
      carType: data.carType,
      position: data.position,
      plate: data.plate,
      phone: data.phone,
      distance: data.distance || null,
      price: calcPrice(data.serviceType, data.distance)
    })
    const body = {
      ...points(data),
      startLocation: data.startAddress.name,
      endLocation: (data.endAddress || data.startAddress).name,
      favourFee: 0,
      remark
    }
    return call('POST', '/order/submitOrder', body).then(r => (r.code === 200 ? ok({ id: String(r.data) }) : r))
  }],
  ['GET', /^\/order\/list$/, ({ query }) => call('GET', '/order/findCustomerOrderPage/1/50').then(r => {
    if (r.code !== 200) return r
    const history = query.type === 'history'
    const list = (r.data.records || []).map(toOrder)
    return ok(list.filter(o => ['waiting', 'dispatched', 'rescuing', 'settling', 'unpaid'].includes(o.status) !== history))
  })],
  ['GET', /^\/order\/(\d+)$/, ({ match }) => call('GET', '/order/getOrderInfo/' + match[1]).then(r => (r.code === 200 ? ok(toOrder(r.data)) : r))],
  ['POST', /^\/payment\/create$/, ({ data }) => call('POST', '/order/payment/create', data)],
  ['GET', /^\/payment\/status\/([A-Za-z0-9_-]+)$/, ({ match }) => call('GET', '/order/payment/status/' + match[1])],
  ['POST', /^\/order\/(\d+)\/cancel$/, ({ match }) => call('GET', '/order/cancelOrder/' + match[1]).then(r => {
    if (r.code !== 200) return r
    if (!r.data) return { code: 500, msg: '师傅已接单，请联系客服取消' }
    return call('GET', '/order/getOrderInfo/' + match[1]).then(d => (d.code === 200 ? ok(toOrder(d.data)) : d))
  })],
  ['GET', /^\/coupon\/list$/, ({ query }) => {
    const status = query.status || 'unused'
    if (status === 'expired') return Promise.resolve(ok([]))
    const url = status === 'used' ? '/coupon/findUsedPage/1/50' : '/coupon/findNoUsePage/1/50'
    return call('GET', url).then(r => {
      if (r.code !== 200) return r
      return ok((r.data.records || []).map(c => ({
        id: c.id,
        name: c.name,
        value: Number(c.amount || 0),
        threshold: Number(c.conditionAmount || 0),
        status,
        receiveTime: new Date(String(c.receiveTime || c.createTime).replace(/-/g, '/')).getTime(),
        expireTime: c.expireTime ? new Date(String(c.expireTime).replace(/-/g, '/')).getTime() : Date.now() + 30 * 864e5
      })))
    })
  }]
]

function parseQuery(qs) {
  const query = {}
  ;(qs || '').split('&').filter(Boolean).forEach(kv => {
    const [k, v = ''] = kv.split('=')
    query[decodeURIComponent(k)] = decodeURIComponent(v)
  })
  return query
}

export default function realRequest(options) {
  const method = (options.method || 'GET').toUpperCase()
  const [path, qs] = options.url.split('?')
  const query = { ...parseQuery(qs), ...(method === 'GET' ? options.data : {}) }
  for (const [m, reg, handler] of handlers) {
    const match = m === method && path.match(reg)
    if (match) return handler({ data: options.data || {}, query, match })
  }
  // 车辆、服务项目、兑换码等后端未提供的接口
  return mockRequest(options)
}
