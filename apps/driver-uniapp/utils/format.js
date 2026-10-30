// 展示用格式化工具
import { SERVICE_NAMES } from '@/mock/rules'

export function serviceName(type) {
  return SERVICE_NAMES[type] || type
}

export const MY_STATUS_TEXT = {
  waiting: '待发车',
  shipping: '运输中',
  finished: '已完成'
}

const pad = n => String(n).padStart(2, '0')

export function formatTime(ts) {
  if (!ts) return '--'
  const d = new Date(ts)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 发布时间：刚刚 / x分钟前 / x小时前 / 日期 */
export function fromNow(ts) {
  const diff = Date.now() - ts
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 3600 * 1000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 24 * 3600 * 1000) return Math.floor(diff / 3600000) + '小时前'
  return formatTime(ts).slice(5, 10)
}

/** 金额：整数不带小数，否则保留两位 */
export function money(v) {
  const n = Number(v) || 0
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}
