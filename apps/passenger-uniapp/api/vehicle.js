import request from '@/utils/request'

/** GET /vehicle/list  =>  [{ id, plate }] */
export function listVehicle() {
  return request({ url: '/vehicle/list', method: 'get' })
}

/** POST /vehicle  body: { plate }  =>  { id, plate } */
export function addVehicle(plate) {
  return request({ url: '/vehicle', method: 'post', data: { plate } })
}

/** DELETE /vehicle/:id */
export function delVehicle(id) {
  return request({ url: '/vehicle/' + id, method: 'delete' })
}
