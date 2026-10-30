import request from '@/utils/request'

/**
 * 认证步骤 key：personal 个人信息 / vehicle 驾驶证、行驶证、车辆照片 / insurance 责任险保单
 * 必须按顺序提交；完成 personal 解锁小修，vehicle 解锁短途拖车，insurance 解锁长途拖车和物流
 */

/** GET /auth/status  =>  { steps: [{ key, title, business, done }], data: { personal, vehicle, insurance } } */
export function getAuthStatus() {
  return request({ url: '/auth/status', method: 'get' })
}

/**
 * 提交认证资料（图片字段为上传后的 url）
 * POST /auth/personal   body: { avatar, idFront, idBack, phone, name, idCard }
 * POST /auth/vehicle    body: { license, vehicleLicense, vehiclePhoto, plate, towType }
 * POST /auth/insurance  body: { policyPhoto, policyNo, expireDate }
 * =>  { steps }
 */
export function submitAuth(step, data) {
  return request({ url: '/auth/' + step, method: 'post', data })
}

/** 身份证人像面识别  POST /ocr/idcard  body: { image }  =>  { name, idCard } */
export function ocrIdCard(image) {
  return request({ url: '/ocr/idcard', method: 'post', data: { image } })
}
