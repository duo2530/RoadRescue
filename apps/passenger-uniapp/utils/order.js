// 订单相关展示工具
const SERVICE_NAMES = {
  tow: '拖车',
  battery: '搭电',
  tire: '换胎',
  charge: '新能源车紧急充电'
}

export function serviceName(type) {
  return SERVICE_NAMES[type] || type
}

export const statusText = {
  waiting: '待接单',
  dispatched: '已派车',
  rescuing: '救援中',
  finished: '已完成',
  cancelled: '已取消'
}

export const STATUS_STEPS = ['waiting', 'dispatched', 'rescuing', 'finished']

const pad = n => String(n).padStart(2, '0')

export function formatTime(ts) {
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function formatDate(ts) {
  const d = new Date(ts)
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}
