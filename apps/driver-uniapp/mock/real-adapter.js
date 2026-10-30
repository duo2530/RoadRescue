/**
 * apiMode=real 时的接口适配层：页面和 /api 保持 Mock 版约定不变，
 * 这里把 Mock 路由翻译成乐尚代驾 web-driver 接口（经网关 /driver-api），并转换返回结构。
 *
 * 对照：
 *   POST /login                     -> GET  /driver/demoLogin（H5 演示登录；小程序端应改为 /driver/login/{wx.login code}）
 *   GET  /driver/info               -> GET  /driver/getDriverLoginInfo + /order/findDriverOrderPage（统计）
 *   GET  /hall/orders               -> POST /location/updateDriverLocation + GET /order/findNewOrderQueueData（派单队列）
 *   POST /hall/orders/:id/grab      -> GET  /order/robNewOrder/{id}（Redisson 分布式锁抢单）
 *   GET  /my/orders                 -> GET  /order/findDriverOrderPage/1/50
 *   GET  /my/orders/:id             -> GET  /order/getOrderInfo/{id}
 *   POST /my/orders/:id/depart      -> GET  /order/driverArriveStartLocation/{id} + POST /order/updateOrderCart + POST /order/startDrive
 *   POST /my/orders/:id/finish      -> POST /order/endDrive
 * 后端没有的能力（短信、分级认证、钱包流水）继续走本地 mock。
 */
import config from '@/config'
import { getToken } from '@/utils/auth'
import mockRequest from './index'
import { SERVICE_NAMES } from './rules'

const ok = data => ({ code: 200, msg: 'success', data })

// 本地 mock 只校验 Authorization 头是否存在；适配层内部直接调 mock 时补上
const localMock = options => mockRequest({ ...options, header: { ...(options.header || {}), Authorization: 'Bearer ' + (getToken() || 'local') } })

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
    if (body.code === 208) return { code: 401, msg: '登录已过期' }
    return { code: body.code, msg: body.message, data: body.data }
  })
}

const time = v => (v ? new Date(String(v).replace(/-/g, '/')).getTime() : null)

function parseRemark(remark) {
  try {
    return remark ? JSON.parse(remark) : {}
  } catch (e) {
    return {}
  }
}

// 乘客端下单的 serviceType（tow/battery/tire/charge）映射到司机端的业务分类
function serviceTypeOf(extra) {
  if (!extra.serviceType || extra.serviceType === 'tow') return 'tow'
  return SERVICE_NAMES[extra.serviceType] ? extra.serviceType : 'repair'
}

function place(name, lat, lng) {
  const label = (name || '').replace(/^(.{2,3}?[省市]).*?(.{2,3}?[市区县]).*$/, '$1-$2') || name
  return { province: '', label, address: name, latitude: lat, longitude: lng }
}

// 后端订单状态 -> 司机端“我的订单”状态
function myStatusOf(s) {
  if (s >= 6) return 'finished'
  if (s >= 4) return 'shipping'
  return 'waiting'
}

function toOrder(o, mine) {
  const extra = parseRemark(o.remark)
  const sameEnd = !o.endLocation || o.endLocation === o.startLocation
  const price = Number(extra.price ?? o.expectAmount ?? o.amount ?? 0)
  return {
    id: String(o.orderId || o.id),
    serviceType: serviceTypeOf(extra),
    plateColor: '蓝牌',
    towType: '平板',
    carModel: extra.carType || '小型轿车',
    start: place(o.startLocation, o.startPointLatitude, o.startPointLongitude),
    end: sameEnd ? null : place(o.endLocation, o.endPointLatitude, o.endPointLongitude),
    mileage: extra.distance || (o.expectDistance ? Math.round(Number(o.expectDistance)) : null),
    price,
    instant: true,
    publishTime: time(o.createTime),
    customerName: '救援用户',
    customerPhone: extra.phone || '',
    remark: [extra.position, extra.plate].filter(Boolean).join(' · '),
    poolStatus: mine ? 'mine' : 'open',
    distance: o.distance != null ? Math.max(1, Math.round(Number(o.distance))) : null,
    myStatus: mine ? myStatusOf(o.status) : null,
    grabTime: time(o.acceptTime || o.createTime),
    departTime: time(o.startServiceTime),
    finishTime: time(o.endServiceTime),
    audited: o.status >= 8
  }
}

function myOrders() {
  return call('GET', '/order/findDriverOrderPage/1/50').then(r => {
    if (r.code !== 200) return r
    return ok((r.data.records || []).map(o => toOrder(o, true)))
  })
}

const handlers = [
  ['POST', /^\/login$/, () => call('GET', '/driver/demoLogin').then(r => (r.code === 200 ? ok({ token: r.data }) : r))],
  ['POST', /^\/logout$/, () => Promise.resolve(ok(null))],
  ['GET', /^\/driver\/info$/, () => Promise.all([call('GET', '/driver/getDriverLoginInfo'), myOrders(), localMock({ url: '/driver/info', method: 'GET' })]).then(([info, mine, local]) => {
    if (info.code !== 200) return info
    const list = mine.code === 200 ? mine.data : []
    const finished = list.filter(o => o.myStatus === 'finished')
    const authed = info.data.authStatus === 2
    return ok({
      ...(local.data || {}),
      driverId: info.data.id,
      nickname: info.data.nickname,
      avatar: info.data.avatarUrl,
      stats: {
        orderCount: list.length,
        totalAmount: finished.filter(o => o.audited).reduce((s, o) => s + o.price, 0),
        unauditedAmount: finished.filter(o => !o.audited).reduce((s, o) => s + o.price, 0)
      },
      // 乐尚代驾只有一级认证（auth_status=2 已认证），这里映射为三个步骤同时完成
      auth: authed ? (local.data.auth || []).map(a => ({ ...a, done: true })) : local.data.auth
    })
  })],
  ['GET', /^\/auth\/status$/, () => Promise.all([call('GET', '/driver/getDriverLoginInfo'), localMock({ url: '/auth/status', method: 'GET' })]).then(([info, local]) => {
    if (info.code === 200 && info.data.authStatus === 2) {
      return ok({ ...local.data, steps: local.data.steps.map(s => ({ ...s, done: true })) })
    }
    return local
  })],
  ['GET', /^\/hall\/orders$/, ({ query }) => {
    // 先上报位置（未开启接单时后端返回“未开启代驾服务”，忽略即可），再拉取派单队列
    const loc = { latitude: Number(query.latitude), longitude: Number(query.longitude) }
    return call('POST', '/location/updateDriverLocation', loc).catch(() => null)
      .then(() => call('GET', '/order/findNewOrderQueueData'))
      .then(r => (r.code === 200 ? ok((r.data || []).map(o => toOrder(o, false))) : r))
  }],
  ['POST', /^\/hall\/orders\/(\d+)\/grab$/, ({ match }) => call('GET', '/order/robNewOrder/' + match[1]).then(r => {
    if (r.code !== 200) return { code: r.code, msg: r.msg || '手慢了，订单已被抢走' }
    return call('GET', '/order/getOrderInfo/' + match[1]).then(d => (d.code === 200 ? ok(toOrder(d.data, true)) : d))
  })],
  ['GET', /^\/my\/orders$/, ({ query }) => myOrders().then(r => {
    if (r.code !== 200 || !query.status) return r
    return ok(r.data.filter(o => o.myStatus === query.status))
  })],
  ['GET', /^\/my\/orders\/(\d+)$/, ({ match }) => call('GET', '/order/getOrderInfo/' + match[1]).then(r => (r.code === 200 ? ok(toOrder(r.data, true)) : r))],
  ['POST', /^\/my\/orders\/(\d+)\/depart$/, ({ match }) => {
    const id = match[1]
    // 救援车到达现场 -> 确认车辆 -> 开始救援；到达校验要求司机位置在起点 1 公里内，先把订单位置缓存为起点
    return call('GET', '/order/getOrderInfo/' + id).then(d => {
      if (d.code !== 200) return d
      const o = d.data
      const extra = parseRemark(o.remark)
      return call('POST', '/location/updateOrderLocationToCache', { orderId: id, latitude: o.startPointLatitude, longitude: o.startPointLongitude })
        .then(() => (o.status < 3 ? call('GET', '/order/driverArriveStartLocation/' + id) : ok(true)))
        .then(r => (r.code !== 200 ? r : o.status < 4
          ? call('POST', '/order/updateOrderCart', { orderId: id, carLicense: extra.plate || '未知', carType: extra.carType || '小型轿车', carFrontUrl: '', carBackUrl: '' })
          : ok(true)))
        .then(r => (r.code !== 200 ? r : call('POST', '/order/startDrive', { orderId: id })))
        .then(r => (r.code !== 200 ? r : call('GET', '/order/getOrderInfo/' + id).then(x => (x.code === 200 ? ok(toOrder(x.data, true)) : x))))
    })
  }],
  ['POST', /^\/my\/orders\/(\d+)\/finish$/, ({ match }) => {
    const id = match[1]
    // 结算依赖服务轨迹（MongoDB）；H5 演示没有持续定位，结束前补报起点、终点两个轨迹点
    return call('GET', '/order/getOrderInfo/' + id).then(d => {
      if (d.code !== 200) return d
      const o = d.data
      const track = [
        { orderId: id, latitude: o.startPointLatitude, longitude: o.startPointLongitude },
        { orderId: id, latitude: o.endPointLatitude, longitude: o.endPointLongitude }
      ]
      return call('POST', '/location/saveOrderServiceLocation', track)
    })
      .then(r => (r.code !== 200 ? r : call('POST', '/order/endDrive', { orderId: id, tollFee: 0, parkingFee: 0, otherFee: 0 })))
      .then(r => (r.code !== 200 ? r : call('GET', '/order/getOrderInfo/' + id).then(x => (x.code === 200 ? ok(toOrder(x.data, true)) : x))))
  }],
  ['POST', /^\/my\/orders\/(\d+)\/cancel$/, () => Promise.resolve({ code: 500, msg: '已接单的救援单请联系调度中心改派' })]
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
  // 短信验证码、分级认证、钱包等后端未提供的接口
  return mockRequest(options)
}
