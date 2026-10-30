import config from '@/config'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { toast, tansParams } from '@/utils/common'
import mockRequest from '@/mock'
import realRequest from '@/mock/real-adapter'

let timeout = 10000
const baseUrl = config.baseUrl

/**
 * 统一请求入口。后端约定返回 { code, msg, data }，code === 200 为成功。
 * config.useMock 为 true 时交给 /mock 在前端处理，页面与 api 层无需区分。
 */
const request = options => {
  options.header = options.header || {}
  if (getToken() && (options.headers || {}).isToken !== false) {
    options.header['Authorization'] = 'Bearer ' + getToken()
  }
  // get请求映射params参数
  if (options.params) {
    let url = options.url + '?' + tansParams(options.params)
    url = url.slice(0, -1)
    options.url = url
  }
  // mock：前端模拟；real：经 real-adapter 翻译到乐尚代驾后端（未覆盖的接口回落到 mock）；
  // raw：页面按后端原始约定直连 baseUrl
  const send = config.apiMode === 'real' ? realRequest(options)
    : config.useMock ? mockRequest(options)
    : uni.request({
      method: options.method || 'get',
      timeout: options.timeout || timeout,
      url: options.baseUrl || baseUrl + options.url,
      data: options.data,
      header: options.header,
      dataType: 'json'
    }).then(res => res.data)

  return new Promise((resolve, reject) => {
    send.then(res => {
      const code = res.code || 200
      const msg = errorCode[code] || res.msg || errorCode['default']
      if (code !== 200) {
        if (options.toast !== false) toast(msg)
        reject(res)
        return
      }
      resolve(res.data)
    }).catch(error => {
      let message = (error && error.errMsg) || String(error)
      if (message.includes('timeout')) {
        message = '系统接口请求超时'
      } else if (message.includes('request:fail')) {
        message = '后端接口连接异常'
      }
      toast(message)
      reject(error)
    })
  })
}

export default request
