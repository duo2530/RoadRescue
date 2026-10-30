import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

export const TOKEN_KEY = 'zd_admin_token'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/mgr-api',
  timeout: 15000
})

// web-mgr 的 TokenAuthenticationFilter 从 header "token" 读取登录令牌
service.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.token = token
  return config
})

// 后端统一返回 Result{code,message,data}；200 成功，208/209 未登录或无权限
service.interceptors.response.use(
  res => {
    const { code, message, data } = res.data || {}
    if (code === 200) return data
    if (code === 208 || code === 209) {
      localStorage.removeItem(TOKEN_KEY)
      if (router.currentRoute.value.path !== '/login') router.replace('/login')
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || 'error'))
  },
  err => {
    ElMessage.error(err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default service
