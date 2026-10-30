/**
 * 服务项目与计价规则（演示用）。后端接入后由 GET /service/list 下发、POST /order/estimate 计算。
 */
export const SERVICES = [
  {
    type: 'tow',
    name: '拖车',
    tip: '车辆在道路行驶过程中抛锚或出现故障，且无法继续行驶，中道救援安排拖车将申请人车辆拖至客户指定地点。',
    needDestination: true,
    // 起步价含 10 公里，超出部分按公里计费
    rule: { basePrice: 180, baseKm: 10, perKm: 45 }
  },
  { type: 'battery', name: '搭电', tip: '车辆因蓄电池亏电等情况造成车辆无法启动，中道救援提供蓄电池搭电服务。', price: 139 },
  {
    type: 'tire',
    name: '换胎',
    tip: '车辆在道路行驶过程中因发生爆胎而无法行驶的，中道救援提供更换车胎服务，备胎需由客户自行提供，配件或其他材料的供应和更换均不涵盖其中。',
    price: 199
  },
  {
    type: 'charge',
    name: '新能源车紧急充电',
    tip: '车辆为新能源车且在行驶过程中发生电量耗尽时，中道救援将安排移动充电车为客户紧急充电，确保客户车辆可以继续行驶10公里以上。',
    price: 350
  }
]

export function getService(type) {
  return SERVICES.find(s => s.type === type)
}

/** 计算原价：拖车按里程（公里），未选终点时返回 null；其余一口价 */
export function calcPrice(type, distance) {
  const s = getService(type)
  if (!s) return null
  if (!s.rule) return s.price
  if (!distance) return null
  const { basePrice, baseKm, perKm } = s.rule
  return Math.round(basePrice + Math.max(0, distance - baseKm) * perKm)
}
