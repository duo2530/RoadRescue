/**
 * 前端演示数据库：所有表存放在 uni storage 的 zd_driver_mock_db 下。
 * 清空浏览器 localStorage（或「我的资料」页的「重置演示数据」）即恢复初始数据。
 */
import { PLACES } from './regions'
import { straightDistance } from '@/utils/qqmap'

const DB_KEY = 'zd_driver_mock_db'
const DB_VERSION = 1

const MIN = 60 * 1000
const HOUR = 60 * MIN
const DAY = 24 * HOUR

// 演示账号：与后端演示数据中司机 ID 1 的手机号一致，用它登录即为已认证的演示司机
export const DEMO_PHONE = '13800000002'

// 里程按直线距离 × 1.3 估算（与用户端路线规划失败时的降级口径一致）
export function estimateMileage(from, to) {
  if (!to) return null
  return Math.max(1, Math.round(straightDistance(from, to) * 1.3))
}

let orderSeq = 0
function makeOrder(now, o) {
  const start = PLACES[o.start]
  const end = o.end ? PLACES[o.end] : null
  orderSeq++
  return {
    id: 'DD' + (now - o.ago) + String(orderSeq).padStart(2, '0'),
    serviceType: o.type,
    plateColor: o.plateColor || '蓝牌',
    towType: o.towType || '斜板',
    carModel: o.carModel || '小型轿车',
    start: { ...start },
    end: end ? { ...end } : null,
    mileage: estimateMileage(start, end),
    price: o.price,
    instant: o.instant !== false,
    publishTime: now - o.ago,
    customerName: o.customerName || '王先生',
    customerPhone: o.customerPhone || '13900139000',
    remark: o.remark || '',
    // open 可抢 / taken 已被其他司机抢走（列表显示“已安排”）/ mine 被当前司机抢到
    poolStatus: o.taken ? 'taken' : 'open'
  }
}

// 演示订单池：附近订单（上海浦东一带）+ 全国订单
const POOL = [
  { start: 'pudongZhangjiang', end: 'wuxi', type: 'logistics', price: 650, ago: 6 * MIN, taken: true, customerName: '李女士' },
  { start: 'pudongHuamu', end: 'pudongTangzhen', type: 'tow', price: 400, ago: 12 * MIN, taken: true },
  { start: 'pudongJinqiao', end: null, type: 'repair', price: 80, ago: 15 * MIN, customerName: '赵先生', remark: '电瓶亏电，需要搭电' },
  { start: 'pudongLujiazui', end: 'xuhui', type: 'tow', price: 260, ago: 25 * MIN, customerName: '周女士' },
  { start: 'pudongTangzhen', end: 'jiading', type: 'tow', price: 420, ago: 40 * MIN, plateColor: '绿牌', carModel: '新能源轿车' },
  { start: 'pudongTangzhen', end: 'hangzhou', type: 'tow', price: 1680, ago: 55 * MIN, customerName: '吴先生' },
  { start: 'xuhui', end: null, type: 'repair', price: 60, ago: 70 * MIN, remark: '更换备胎' },
  { start: 'minhang', end: 'suzhou', type: 'logistics', price: 900, ago: 2 * HOUR, plateColor: '黄牌', towType: '平板', carModel: '轻型货车' },
  { start: 'baoshan', end: 'songjiang', type: 'tow', price: 380, ago: 3 * HOUR, taken: true },
  { start: 'meizhou', end: null, type: 'repair', price: 50, ago: 5 * HOUR, taken: true },
  { start: 'loudi', end: null, type: 'repair', price: 500, ago: 8 * HOUR, taken: true },
  { start: 'tonghua', end: null, type: 'repair', price: 50, ago: 20 * HOUR, taken: true },
  { start: 'hefei', end: 'ningbo', type: 'logistics', price: 1350, ago: 1.5 * DAY },
  { start: 'jinan', end: 'jinan', type: 'tow', price: 220, ago: 2.5 * DAY, taken: true },
  { start: 'chengdu', end: null, type: 'repair', price: 70, ago: 4 * DAY },
  { start: 'wuxi', end: 'pudongJinqiao', type: 'tow', price: 820, ago: 6 * DAY }
]

// 定时新增的附近订单模板（见 rules.SPAWN_INTERVAL）
export const SPAWN_TEMPLATES = [
  { start: 'pudongHuamu', end: null, type: 'repair', price: 90, remark: '车辆无法启动' },
  { start: 'pudongJinqiao', end: 'baoshan', type: 'tow', price: 350 },
  { start: 'pudongLujiazui', end: 'suzhou', type: 'logistics', price: 980 },
  { start: 'pudongTangzhen', end: null, type: 'repair', price: 60, remark: '轮胎漏气' },
  { start: 'pudongZhangjiang', end: 'minhang', type: 'tow', price: 300 }
]

export function spawnOrder(now, index) {
  const t = SPAWN_TEMPLATES[index % SPAWN_TEMPLATES.length]
  return makeOrder(now, { ...t, ago: 0 })
}

function seed() {
  const now = Date.now()
  orderSeq = 0
  return {
    version: DB_VERSION,
    createTime: now,
    driver: {
      driverId: 20001,
      nickname: 'driver_odTAe5f1wi',
      phone: DEMO_PHONE,
      avatar: ''
    },
    // 各认证步骤提交的资料，null 表示未提交；演示版提交即审核通过
    auth: { personal: null, vehicle: null, insurance: null },
    orders: POOL.map(o => makeOrder(now, o)),
    // 当前司机抢到的订单：{ orderId, status, grabTime, departTime, finishTime, audited }
    myOrders: [],
    wallet: { freight: 0, deposit: 0 },
    flows: [],
    spawned: 0,
    seq: { flow: 0 }
  }
}

let cache = null

export function getDb() {
  if (cache) return cache
  const stored = uni.getStorageSync(DB_KEY)
  cache = stored && stored.version === DB_VERSION ? stored : seed()
  saveDb()
  return cache
}

export function saveDb() {
  uni.setStorageSync(DB_KEY, cache)
}

export function resetDb() {
  cache = seed()
  saveDb()
  return cache
}
