import config from '@/config'

/**
 * 腾讯位置服务 WebService 封装（逆地址解析、驾车路线）。
 * H5 端接口不支持跨域，使用 JSONP；小程序端用 uni.request（需配置合法域名）。
 */
const BASE = 'https://apis.map.qq.com/ws'

let jsonpSeq = 0
function jsonp(url) {
  return new Promise((resolve, reject) => {
    const cb = '__qqmap_cb_' + Date.now() + '_' + (jsonpSeq++)
    const script = document.createElement('script')
    const timer = setTimeout(() => cleanup(new Error('timeout')), 10000)
    function cleanup(err, data) {
      clearTimeout(timer)
      delete window[cb]
      script.remove()
      err ? reject(err) : resolve(data)
    }
    window[cb] = data => cleanup(null, data)
    script.onerror = () => cleanup(new Error('network'))
    script.src = url + '&output=jsonp&callback=' + cb
    document.body.appendChild(script)
  })
}

function call(path, params) {
  const query = Object.keys(params).map(k => k + '=' + encodeURIComponent(params[k])).join('&')
  const url = BASE + path + '?' + query + '&key=' + config.qqMapKey
  let p
  // #ifdef H5
  p = jsonp(url)
  // #endif
  // #ifndef H5
  p = uni.request({ url }).then(res => res.data)
  // #endif
  return p.then(res => {
    if (res.status !== 0) throw new Error(res.message)
    return res.result
  })
}

/** 逆地址解析：坐标 -> { name, address, latitude, longitude } */
export function reverseGeocode(latitude, longitude) {
  return call('/geocoder/v1/', { location: latitude + ',' + longitude, get_poi: 1, poi_options: 'policy=2' })
    .then(result => {
      const poi = (result.pois || [])[0]
      const recommend = result.formatted_addresses && result.formatted_addresses.recommend
      return {
        name: (poi && poi.title) || recommend || result.address,
        address: result.address,
        latitude,
        longitude
      }
    })
}

/** 驾车路线：返回 { distance(公里), points: [{latitude, longitude}] } */
export function drivingRoute(from, to) {
  return call('/direction/v1/driving/', {
    from: from.latitude + ',' + from.longitude,
    to: to.latitude + ',' + to.longitude
  }).then(result => {
    const route = result.routes[0]
    // polyline 为前向差分压缩：从第 3 个元素起，每个值 = 前第 2 个值 + 当前值 / 1e6
    const coors = route.polyline.slice()
    for (let i = 2; i < coors.length; i++) {
      coors[i] = coors[i - 2] + coors[i] / 1000000
    }
    const points = []
    for (let i = 0; i < coors.length; i += 2) {
      points.push({ latitude: coors[i], longitude: coors[i + 1] })
    }
    return { distance: Math.round(route.distance / 10) / 100, points }
  })
}

/** 直线距离（公里），路线规划失败时用于估算 */
export function straightDistance(a, b) {
  const rad = d => d * Math.PI / 180
  const dLat = rad(b.latitude - a.latitude)
  const dLng = rad(b.longitude - a.longitude)
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.latitude)) * Math.cos(rad(b.latitude)) * Math.sin(dLng / 2) ** 2
  return 6371 * 2 * Math.asin(Math.sqrt(h))
}
