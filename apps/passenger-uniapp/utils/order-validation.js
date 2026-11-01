const hasText = value => typeof value === 'string' && value.trim().length > 0

function validAddress(address) {
  return address && hasText(address.name)
    && Number.isFinite(address.latitude) && Math.abs(address.latitude) <= 90
    && Number.isFinite(address.longitude) && Math.abs(address.longitude) <= 180
}

// 页面、Mock 和真实接口共用规则，避免切换接口模式后漏校验。
export function validateOrder(data, service) {
  if (!service) return '服务类型错误'
  if (!validAddress(data.startAddress)) return '请选择救援地点'
  if (service.needDestination && !validAddress(data.endAddress)) return '请选择拖车终点'
  if (!hasText(data.carType)) return '请选择车型'
  if (!hasText(data.position)) return '请选择车辆当前位置'
  if (!hasText(data.plate)) return '请选择车牌号'
  if (!/^[一-龥][A-Z][A-Z0-9]{5,6}$/.test(data.plate)) return '请输入正确的车牌号'
  if (!hasText(data.phone)) return '请输入联系电话'
  if (!/^1\d{10}$/.test(data.phone)) return '请输入正确的联系电话'
  return ''
}
