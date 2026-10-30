import { getDb, saveDb } from './db'
import { SERVICES, getService, calcPrice } from './services'

/**
 * 前端 mock 路由。入参与 uni.request 一致，返回 { code, msg, data }。
 * handler 的入参/出参即后端接口约定，详见 /api 下各函数注释。
 */
const routes = []
function route(method, path, handler) {
  // 支持 /order/:id 形式的路径参数
  const keys = []
  const pattern = path.replace(/:(\w+)/g, (_, k) => {
    keys.push(k)
    return '([^/]+)'
  })
  routes.push({ method, reg: new RegExp('^' + pattern + '$'), keys, handler })
}

const ok = data => ({ code: 200, msg: 'success', data })
const fail = (msg, code = 500) => ({ code, msg })

const ONGOING = ['waiting', 'dispatched', 'rescuing']

// 订单状态按下单后经过的时间自动推进，便于演示
const STATUS_FLOW = [
  { status: 'waiting', after: 0 },
  { status: 'dispatched', after: 20 * 1000 },
  { status: 'rescuing', after: 60 * 1000 },
  { status: 'finished', after: 180 * 1000 }
]
const DRIVERS = [
  { name: '张师傅', phone: '13761234567', plate: '沪D·T6618' },
  { name: '陈师傅', phone: '13661239876', plate: '沪D·R5502' },
  { name: '刘师傅', phone: '13501112233', plate: '沪D·W3307' }
]

function refreshStatus(order) {
  if (!ONGOING.includes(order.status)) return order
  const elapsed = Date.now() - order.createTime
  STATUS_FLOW.forEach(s => {
    if (elapsed >= s.after) order.status = s.status
  })
  if (order.status !== 'waiting' && !order.driver) {
    order.driver = DRIVERS[order.createTime % DRIVERS.length]
  }
  return order
}

function couponStatus(coupon) {
  return coupon.status === 'unused' && coupon.expireTime < Date.now() ? 'expired' : coupon.status
}

// 自动选择可用的最大面额优惠券
function bestCoupon(db, amount) {
  return db.coupons
    .filter(c => couponStatus(c) === 'unused' && amount >= c.threshold)
    .sort((a, b) => b.value - a.value)[0]
}

function estimate(db, serviceType, distance) {
  const amount = calcPrice(serviceType, distance)
  if (amount === null) return { amount: null, discount: 0, payAmount: null, coupon: null }
  const coupon = bestCoupon(db, amount)
  const discount = coupon ? Math.min(coupon.value, amount) : 0
  return { amount, discount, payAmount: amount - discount, coupon: coupon || null }
}

// ---------- 用户 ----------
route('POST', '/login', () => ok({ token: 'mock-token-' + Date.now() }))
route('GET', '/user/info', () => ok(getDb().user))
route('POST', '/logout', () => ok(null))

// ---------- 车辆 ----------
route('GET', '/vehicle/list', () => ok(getDb().vehicles))
route('POST', '/vehicle', ({ data }) => {
  const db = getDb()
  const plate = (data.plate || '').replace(/[\s·]/g, '').toUpperCase()
  if (!/^[一-龥][A-Z][A-Z0-9]{5,6}$/.test(plate)) return fail('请输入正确的车牌号')
  if (db.vehicles.some(v => v.plate === plate)) return fail('该车牌已存在')
  const vehicle = { id: ++db.seq.vehicle, plate }
  db.vehicles.push(vehicle)
  saveDb()
  return ok(vehicle)
})
route('DELETE', '/vehicle/:id', ({ params }) => {
  const db = getDb()
  db.vehicles = db.vehicles.filter(v => String(v.id) !== params.id)
  saveDb()
  return ok(null)
})

// ---------- 服务与计价 ----------
route('GET', '/service/list', () => ok(SERVICES))
route('POST', '/order/estimate', ({ data }) => {
  const { coupon, ...rest } = estimate(getDb(), data.serviceType, data.distance)
  return ok({ ...rest, couponName: coupon ? coupon.name : '' })
})

// ---------- 订单 ----------
route('POST', '/order', ({ data }) => {
  const db = getDb()
  const service = getService(data.serviceType)
  if (!service) return fail('服务类型错误')
  if (!data.startAddress) return fail('请选择救援地点')
  if (service.needDestination && !data.endAddress) return fail('请选择拖车终点')
  if (!data.plate) return fail('请选择车牌号')
  if (!/^1\d{10}$/.test(data.phone || '')) return fail('请输入正确的联系电话')
  if (db.orders.some(o => ONGOING.includes(refreshStatus(o).status))) {
    return fail('您有进行中的救援服务，请勿重复下单')
  }
  const { amount, discount, payAmount, coupon } = estimate(db, data.serviceType, data.distance)
  if (coupon) {
    coupon.status = 'used'
    coupon.useTime = Date.now()
  }
  const order = {
    id: 'ZD' + Date.now(),
    serviceType: data.serviceType,
    status: 'waiting',
    createTime: Date.now(),
    startAddress: data.startAddress,
    endAddress: service.needDestination ? data.endAddress : null,
    distance: service.needDestination ? data.distance : null,
    carType: data.carType,
    position: data.position,
    plate: data.plate,
    phone: data.phone,
    amount,
    discount,
    payAmount
  }
  db.orders.unshift(order)
  saveDb()
  return ok(order)
})
route('GET', '/order/list', ({ query }) => {
  const db = getDb()
  db.orders.forEach(refreshStatus)
  saveDb()
  const history = query.type === 'history'
  return ok(db.orders.filter(o => ONGOING.includes(o.status) !== history))
})
route('GET', '/order/:id', ({ params }) => {
  const db = getDb()
  const order = db.orders.find(o => o.id === params.id)
  if (!order) return fail('订单不存在', 404)
  refreshStatus(order)
  saveDb()
  return ok(order)
})
route('POST', '/order/:id/cancel', ({ params }) => {
  const db = getDb()
  const order = db.orders.find(o => o.id === params.id)
  if (!order) return fail('订单不存在', 404)
  refreshStatus(order)
  if (order.status === 'rescuing') return fail('救援进行中，请联系客服取消')
  if (!ONGOING.includes(order.status)) return fail('订单已结束')
  order.status = 'cancelled'
  // 取消后退回优惠券
  if (order.discount) {
    const used = db.coupons.find(c => c.status === 'used' && c.useTime >= order.createTime)
    if (used) used.status = 'unused'
  }
  saveDb()
  return ok(order)
})

// ---------- 优惠券 ----------
// 演示兑换码
const REDEEM_CODES = {
  ZD2026: { name: '救援无门槛券', value: 20, threshold: 0, days: 30 },
  ZD50: { name: '拖车满减券', value: 50, threshold: 200, days: 30 },
  OLD2025: { name: '体验券', value: 10, threshold: 0, days: -1 }
}
route('GET', '/coupon/list', ({ query }) => {
  const status = query.status || 'unused'
  const list = getDb().coupons.map(c => ({ ...c, status: couponStatus(c) }))
  return ok(list.filter(c => c.status === status))
})
route('POST', '/coupon/redeem', ({ data }) => {
  const db = getDb()
  const code = (data.code || '').trim().toUpperCase()
  if (!code) return fail('请输入兑换码')
  const tpl = REDEEM_CODES[code]
  if (!tpl) return fail('兑换码无效')
  if (db.coupons.some(c => c.code === code)) return fail('该兑换码已兑换')
  const coupon = {
    id: ++db.seq.coupon,
    code,
    name: tpl.name,
    value: tpl.value,
    threshold: tpl.threshold,
    status: 'unused',
    receiveTime: Date.now(),
    expireTime: Date.now() + tpl.days * 24 * 3600 * 1000
  }
  db.coupons.unshift(coupon)
  saveDb()
  return ok(coupon)
})

function parseQuery(qs) {
  const query = {}
  ;(qs || '').split('&').filter(Boolean).forEach(kv => {
    const [k, v = ''] = kv.split('=')
    query[decodeURIComponent(k)] = decodeURIComponent(v)
  })
  return query
}

export default function mockRequest(options) {
  const method = (options.method || 'GET').toUpperCase()
  const [path, qs] = options.url.split('?')
  const query = { ...parseQuery(qs), ...(method === 'GET' ? options.data : {}) }
  return new Promise(resolve => {
    // 模拟网络延迟
    setTimeout(() => {
      for (const r of routes) {
        const m = r.method === method && path.match(r.reg)
        if (m) {
          const params = {}
          r.keys.forEach((k, i) => {
            params[k] = decodeURIComponent(m[i + 1])
          })
          resolve(r.handler({ data: options.data || {}, query, params }))
          return
        }
      }
      resolve(fail('mock 未实现：' + method + ' ' + path, 404))
    }, 150)
  })
}
