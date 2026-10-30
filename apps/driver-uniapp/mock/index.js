import { getDb, saveDb, resetDb, spawnOrder, DEMO_PHONE } from './db'
import { AUTH_STEPS, requiredSteps, businessName, AUDIT_DELAY, SPAWN_INTERVAL, SPAWN_MAX, DEMO_SMS_CODE, LONG_DISTANCE_KM } from './rules'
import { straightDistance } from '@/utils/qqmap'

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
const fail = (msg, code = 500, data) => ({ code, msg, data })

// 抢单未解锁业务时的业务码，页面据此弹出“去认证”
const CODE_AUTH_REQUIRED = 4001

// ---------- 状态推进 ----------

// 订单池按时间新增附近订单，便于演示“刷新”
function spawnOrders(db) {
  const due = Math.min(SPAWN_MAX, Math.floor((Date.now() - db.createTime) / SPAWN_INTERVAL))
  while (db.spawned < due) {
    db.orders.unshift(spawnOrder(db.createTime + (db.spawned + 1) * SPAWN_INTERVAL, db.spawned))
    db.spawned++
  }
}

// 已送达订单经过审核时长后运费入账
function auditOrders(db) {
  db.myOrders.forEach(m => {
    if (m.status === 'finished' && !m.audited && Date.now() - m.finishTime >= AUDIT_DELAY) {
      m.audited = true
      const order = findOrder(db, m.orderId)
      db.wallet.freight += order.price
      addFlow(db, '运费入账', order.price, '订单 ' + order.id)
    }
  })
}

function refresh(db) {
  spawnOrders(db)
  auditOrders(db)
  saveDb()
}

function findOrder(db, id) {
  return db.orders.find(o => o.id === id)
}

function addFlow(db, title, amount, remark) {
  db.flows.unshift({ id: ++db.seq.flow, title, amount, remark, time: Date.now() })
}

function authStatus(db) {
  return AUTH_STEPS.map(s => ({ ...s, done: !!db.auth[s.key] }))
}

// 订单池列表项：附带距离和当前司机视角的按钮状态
function poolItem(order, loc) {
  return {
    ...order,
    distance: loc ? Math.max(1, Math.round(straightDistance(loc, order.start))) : null
  }
}

// 我的订单项：订单信息 + 我的状态
function myItem(db, m) {
  return { ...findOrder(db, m.orderId), myStatus: m.status, grabTime: m.grabTime, departTime: m.departTime, finishTime: m.finishTime, audited: m.audited }
}

function parseLoc(query) {
  const lat = parseFloat(query.latitude)
  const lng = parseFloat(query.longitude)
  return isNaN(lat) || isNaN(lng) ? null : { latitude: lat, longitude: lng }
}

// ---------- 登录 ----------
route('POST', '/sms/code', ({ data }) => {
  if (!/^1\d{10}$/.test(data.phone || '')) return fail('请输入正确的手机号')
  // 演示版不发短信，固定验证码
  return ok({ hint: '演示验证码：' + DEMO_SMS_CODE })
})
route('POST', '/login', ({ data }) => {
  if (!/^1\d{10}$/.test(data.phone || '')) return fail('请输入正确的手机号')
  if (data.code !== DEMO_SMS_CODE) return fail('验证码错误')
  const db = getDb()
  db.driver.phone = data.phone
  saveDb()
  return ok({ token: 'mock-driver-token-' + Date.now() })
})
route('POST', '/logout', () => ok(null))
route('GET', '/driver/info', () => {
  const db = getDb()
  refresh(db)
  const mine = db.myOrders.map(m => myItem(db, m))
  const finished = mine.filter(o => o.myStatus === 'finished')
  return ok({
    ...db.driver,
    stats: {
      orderCount: mine.length,
      totalAmount: finished.filter(o => o.audited).reduce((s, o) => s + o.price, 0),
      unauditedAmount: finished.filter(o => !o.audited).reduce((s, o) => s + o.price, 0)
    },
    wallet: { balance: db.wallet.freight + db.wallet.deposit, ...db.wallet },
    auth: authStatus(db)
  })
})
route('POST', '/mock/reset', () => {
  resetDb()
  return ok(null)
})

// ---------- 认证 ----------
route('GET', '/auth/status', () => {
  const db = getDb()
  return ok({ steps: authStatus(db), data: db.auth })
})
route('POST', '/auth/:step', ({ params, data }) => {
  const db = getDb()
  const step = AUTH_STEPS.find(s => s.key === params.step)
  if (!step) return fail('认证步骤不存在', 404)
  const index = AUTH_STEPS.indexOf(step)
  const prev = AUTH_STEPS.slice(0, index).find(s => !db.auth[s.key])
  if (prev) return fail('请先完成「' + prev.title + '」')
  if (params.step === 'personal') {
    if (!data.avatar) return fail('请上传个人头像')
    if (!data.idFront || !data.idBack) return fail('请上传身份证正反面')
    if (!/^1\d{10}$/.test(data.phone || '')) return fail('请输入正确的手机号')
    if (!data.name) return fail('请填写姓名')
    if (!/^\d{17}[\dXx]$/.test(data.idCard || '')) return fail('请填写正确的身份证号')
  } else if (params.step === 'vehicle') {
    if (!data.license) return fail('请上传驾驶证')
    if (!data.vehicleLicense) return fail('请上传行驶证')
    if (!data.vehiclePhoto) return fail('请上传人车合影')
    if (!/^[一-龥][A-Z][A-Z0-9]{5,6}$/.test((data.plate || '').toUpperCase())) return fail('请填写正确的车牌号')
  } else if (params.step === 'insurance') {
    if (!data.policyPhoto) return fail('请上传保单照片')
    if (!data.policyNo) return fail('请填写保单号')
    if (!data.expireDate) return fail('请选择保单到期日')
  }
  // 演示版：提交即审核通过
  db.auth[params.step] = { ...data, submitTime: Date.now() }
  if (params.step === 'personal' && data.avatar) db.driver.avatar = data.avatar
  saveDb()
  return ok({ steps: authStatus(db) })
})
// 身份证 OCR（演示：返回固定数据）
// 演示用虚构身份证号（只满足 18 位格式）
route('POST', '/ocr/idcard', () => ok({ name: '张三', idCard: '31011519900101000X' }))

// ---------- 抢单大厅 ----------
/**
 * scope=nearby：按 radius（公里）筛选，按距离升序
 * scope=all：外地订单（起点距我超过 LONG_DISTANCE_KM；按出发地筛选时不限距离），
 *   可选 startProvince / endProvince / publishWithin（小时），按发布时间倒序
 */
route('GET', '/hall/orders', ({ query }) => {
  const db = getDb()
  refresh(db)
  const loc = parseLoc(query)
  let list = db.orders.filter(o => o.poolStatus !== 'mine').map(o => poolItem(o, loc))
  if (query.scope === 'nearby') {
    const radius = parseFloat(query.radius) || 10
    list = list.filter(o => o.distance !== null && o.distance <= radius).sort((a, b) => a.distance - b.distance)
  } else {
    if (query.startProvince) list = list.filter(o => o.start.province === query.startProvince)
    else list = list.filter(o => o.distance === null || o.distance > LONG_DISTANCE_KM)
    if (query.endProvince) list = list.filter(o => o.end && o.end.province === query.endProvince)
    if (query.publishWithin) list = list.filter(o => Date.now() - o.publishTime <= parseFloat(query.publishWithin) * 3600 * 1000)
    list.sort((a, b) => b.publishTime - a.publishTime)
  }
  return ok(list)
})
route('POST', '/hall/orders/:id/grab', ({ params }) => {
  const db = getDb()
  const order = findOrder(db, params.id)
  if (!order) return fail('订单不存在', 404)
  if (order.poolStatus !== 'open') return fail('手慢了，订单已被抢走')
  const missing = requiredSteps(order).filter(k => !db.auth[k])
  if (missing.length) {
    return fail('完善认证后可接「' + businessName(order) + '」订单', CODE_AUTH_REQUIRED, { missing })
  }
  order.poolStatus = 'mine'
  db.myOrders.unshift({ orderId: order.id, status: 'waiting', grabTime: Date.now() })
  saveDb()
  return ok(myItem(db, db.myOrders[0]))
})

// ---------- 我的订单 ----------
// status: waiting 待发车 / shipping 运输中 / finished 已完成；不传为全部
route('GET', '/my/orders', ({ query }) => {
  const db = getDb()
  refresh(db)
  let list = db.myOrders.map(m => myItem(db, m))
  if (query.status) list = list.filter(o => o.myStatus === query.status)
  return ok(list)
})
route('GET', '/my/orders/:id', ({ params }) => {
  const db = getDb()
  refresh(db)
  const m = db.myOrders.find(x => x.orderId === params.id)
  if (m) return ok(myItem(db, m))
  // 大厅里的订单也可查看详情
  const order = findOrder(db, params.id)
  return order ? ok({ ...order, myStatus: null }) : fail('订单不存在', 404)
})
route('POST', '/my/orders/:id/depart', ({ params }) => {
  const db = getDb()
  const m = db.myOrders.find(x => x.orderId === params.id)
  if (!m) return fail('订单不存在', 404)
  if (m.status !== 'waiting') return fail('订单状态已变化，请刷新')
  m.status = 'shipping'
  m.departTime = Date.now()
  saveDb()
  return ok(myItem(db, m))
})
route('POST', '/my/orders/:id/finish', ({ params }) => {
  const db = getDb()
  const m = db.myOrders.find(x => x.orderId === params.id)
  if (!m) return fail('订单不存在', 404)
  if (m.status !== 'shipping') return fail('订单状态已变化，请刷新')
  m.status = 'finished'
  m.finishTime = Date.now()
  m.audited = false
  saveDb()
  return ok(myItem(db, m))
})
route('POST', '/my/orders/:id/cancel', ({ params }) => {
  const db = getDb()
  const m = db.myOrders.find(x => x.orderId === params.id)
  if (!m) return fail('订单不存在', 404)
  if (m.status !== 'waiting') return fail('已发车的订单不能取消，请联系客服')
  db.myOrders = db.myOrders.filter(x => x !== m)
  findOrder(db, params.id).poolStatus = 'open'
  saveDb()
  return ok(null)
})

// ---------- 钱包 ----------
route('GET', '/wallet/flows', () => {
  const db = getDb()
  refresh(db)
  return ok(db.flows)
})
route('POST', '/wallet/recharge', ({ data }) => {
  const db = getDb()
  const amount = Math.round(parseFloat(data.amount) * 100) / 100
  if (!(amount > 0)) return fail('请输入正确的金额')
  if (amount > 50000) return fail('单笔充值不能超过 50000 元')
  db.wallet.deposit += amount
  addFlow(db, '保证金充值', amount, '演示充值')
  saveDb()
  return ok(null)
})
route('POST', '/wallet/withdraw', ({ data }) => {
  const db = getDb()
  const amount = Math.round(parseFloat(data.amount) * 100) / 100
  if (!(amount > 0)) return fail('请输入正确的金额')
  // 只有运费可提现，保证金不可提现
  if (amount > db.wallet.freight) return fail('可提现运费不足（¥' + db.wallet.freight + '）')
  db.wallet.freight -= amount
  addFlow(db, '运费提现', -amount, '提现到银行卡')
  saveDb()
  return ok(null)
})

function parseQuery(qs) {
  const query = {}
  ;(qs || '').split('&').filter(Boolean).forEach(kv => {
    const [k, v = ''] = kv.split('=')
    query[decodeURIComponent(k)] = decodeURIComponent(v)
  })
  return query
}

// 不需要登录的接口
const PUBLIC = ['/sms/code', '/login']

export default function mockRequest(options) {
  const method = (options.method || 'GET').toUpperCase()
  const [path, qs] = options.url.split('?')
  const query = { ...parseQuery(qs), ...(method === 'GET' ? options.data : {}) }
  return new Promise(resolve => {
    // 模拟网络延迟
    setTimeout(() => {
      if (!PUBLIC.includes(path) && !(options.header || {}).Authorization) {
        resolve(fail('请先登录', 401))
        return
      }
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

export { CODE_AUTH_REQUIRED, DEMO_PHONE }
