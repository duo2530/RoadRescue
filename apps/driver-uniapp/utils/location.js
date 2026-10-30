import config from '@/config'
import { reverseCity } from '@/utils/qqmap'

/**
 * 获取司机当前位置 { latitude, longitude, city }。
 * 定位失败（H5 未授权、非 https 等）时使用 config.defaultLocation；
 * 城市名用腾讯地图逆地址解析，配额用完或无 key 时沿用默认城市。
 */
let cached = null

export function getLocation() {
  if (cached) return Promise.resolve(cached)
  const fallback = { ...config.defaultLocation }
  return new Promise(resolve => {
    uni.getLocation({
      type: 'gcj02',
      success: res => resolve({ latitude: res.latitude, longitude: res.longitude, city: '' }),
      fail: () => resolve(fallback)
    })
  }).then(loc => {
    if (loc.city || !config.qqMapKey) return { ...loc, city: loc.city || fallback.city }
    return reverseCity(loc.latitude, loc.longitude)
      .then(city => ({ ...loc, city }))
      .catch(() => ({ ...loc, city: fallback.city }))
  }).then(loc => {
    cached = loc
    return loc
  })
}
