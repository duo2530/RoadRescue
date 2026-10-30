import request from '@/utils/request'

/**
 * GET /service/list
 * =>  [{ type, name, tip, needDestination?, price?, rule?: { basePrice, baseKm, perKm } }]
 */
export function listService() {
  return request({ url: '/service/list', method: 'get' })
}
