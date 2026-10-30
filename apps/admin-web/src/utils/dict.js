// 与后端 OrderStatus 枚举一致，文案按道路救援场景表述
export const ORDER_STATUS = {
  1: { label: '待接单', type: 'warning' },
  2: { label: '已派单', type: 'primary' },
  3: { label: '师傅已到达', type: 'primary' },
  4: { label: '已确认车辆', type: 'primary' },
  5: { label: '救援中', type: 'success' },
  6: { label: '救援结束', type: 'info' },
  7: { label: '待付款', type: 'danger' },
  8: { label: '已付款', type: 'success' },
  9: { label: '已完成', type: 'success' },
  '-1': { label: '已取消', type: 'info' }
}

// driver_info.auth_status
export const AUTH_STATUS = {
  0: { label: '未认证', type: 'info' },
  1: { label: '审核中', type: 'warning' },
  2: { label: '已认证', type: 'success' },
  '-1': { label: '未通过', type: 'danger' }
}

export const statusOf = (dict, v) => dict[v] || { label: v ?? '-', type: 'info' }
