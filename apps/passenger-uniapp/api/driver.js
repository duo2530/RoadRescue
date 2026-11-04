import request from '@/utils/request'

/**
 * 附近在线的救援车辆（即开启接单的司机位置）
 * GET /driver/nearby?latitude=&longitude=  =>  [{ latitude, longitude, distance (公里) }]，按距离升序
 */
export function listNearbyTrucks(latitude, longitude) {
  return request({ url: '/driver/nearby', method: 'get', params: { latitude, longitude }, toast: false })
}
