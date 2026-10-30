/**
 * 司机端业务规则配置（演示用，接后端后由后端判定，页面不依赖这里的常量）
 */

// 服务类型
export const SERVICE_NAMES = {
  repair: '小修',
  tow: '拖车',
  logistics: '物流'
}

// 拖车里程超过该值（公里）算长途
export const LONG_DISTANCE_KM = 100

// 认证步骤及解锁的业务，顺序即认证页展示顺序
export const AUTH_STEPS = [
  { key: 'personal', title: '个人信息', business: '小修业务' },
  { key: 'vehicle', title: '驾驶证、行驶证、车辆照片', business: '短途拖车业务' },
  { key: 'insurance', title: '责任险保单', business: '长途拖车业务、物流业务' }
]

/** 接某个订单需要完成的认证步骤 */
export function requiredSteps(order) {
  if (order.serviceType === 'repair') return ['personal']
  if (order.serviceType === 'tow' && (order.mileage || 0) <= LONG_DISTANCE_KM) return ['personal', 'vehicle']
  return ['personal', 'vehicle', 'insurance']
}

/** 订单所属业务名称，用于“未解锁”提示 */
export function businessName(order) {
  if (order.serviceType === 'repair') return '小修业务'
  if (order.serviceType === 'logistics') return '物流业务'
  return (order.mileage || 0) <= LONG_DISTANCE_KM ? '短途拖车业务' : '长途拖车业务'
}

// 运费审核时长：订单送达后经过该时间（毫秒）自动审核通过，计入钱包
export const AUDIT_DELAY = 30 * 1000

// 订单池每隔该时间（毫秒）新增一条附近订单，最多新增 SPAWN_MAX 条，便于演示刷新
export const SPAWN_INTERVAL = 90 * 1000
export const SPAWN_MAX = 5

// 演示验证码
export const DEMO_SMS_CODE = '123456'
