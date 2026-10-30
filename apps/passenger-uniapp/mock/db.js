/**
 * 前端演示数据库：所有表存放在 uni storage 的 zd_mock_db 下。
 * 设置页「退出登录」不会清数据；清空浏览器 localStorage 即恢复初始数据。
 */
const DB_KEY = 'zd_mock_db'
const DB_VERSION = 1

const DAY = 24 * 3600 * 1000

function seed() {
  const now = Date.now()
  return {
    version: DB_VERSION,
    user: {
      userId: 10001,
      phone: '13000000000',
      avatar: ''
    },
    vehicles: [
      { id: 1, plate: '沪AD12345' },
      { id: 2, plate: '沪B8K6T9' }
    ],
    orders: [
      {
        id: 'ZD' + (now - 12 * DAY),
        serviceType: 'tow',
        status: 'finished',
        createTime: now - 12 * DAY,
        startAddress: { name: '芈展服务园', address: '上海市浦东新区唐镇达秀路151号', latitude: 31.238096, longitude: 121.653299 },
        endAddress: { name: '上海国际汽车城', address: '上海市嘉定区安亭镇墨玉南路888号', latitude: 31.288, longitude: 121.17 },
        distance: 52.4,
        carType: '小型轿车',
        position: '地面',
        plate: '沪AD12345',
        phone: '13000000000',
        amount: 2088,
        discount: 0,
        payAmount: 2088,
        driver: { name: '王师傅', phone: '13800001234', plate: '沪D·T8821' }
      },
      {
        id: 'ZD' + (now - 40 * DAY),
        serviceType: 'battery',
        status: 'finished',
        createTime: now - 40 * DAY,
        startAddress: { name: '张江高科技园区', address: '上海市浦东新区祖冲之路', latitude: 31.2035, longitude: 121.5905 },
        endAddress: null,
        distance: null,
        carType: '小型轿车',
        position: '地库',
        plate: '沪AD12345',
        phone: '13000000000',
        amount: 139,
        discount: 0,
        payAmount: 139,
        driver: { name: '李师傅', phone: '13900005678', plate: '沪D·R2210' }
      }
    ],
    coupons: [],
    seq: { vehicle: 2, coupon: 0 }
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
