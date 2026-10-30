import request from '@/utils/request'

/** 钱包流水  GET /wallet/flows  =>  [{ id, title, amount（入账为正、支出为负）, remark, time }] */
export function listFlows() {
  return request({ url: '/wallet/flows', method: 'get' })
}

/** 充值保证金  POST /wallet/recharge  body: { amount } */
export function recharge(amount) {
  return request({ url: '/wallet/recharge', method: 'post', data: { amount } })
}

/** 提现（仅运费部分可提现）  POST /wallet/withdraw  body: { amount } */
export function withdraw(amount) {
  return request({ url: '/wallet/withdraw', method: 'post', data: { amount } })
}
