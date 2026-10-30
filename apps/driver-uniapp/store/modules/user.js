import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getDriverInfo, logout } from '@/api/user'
import { getToken, removeToken, setToken } from '@/utils/auth'
import defAva from '@/static/images/driver/avatar.png'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const info = ref(null)
  const avatar = ref(defAva)

  const loginBy = data => login(data).then(res => {
    setToken(res.token)
    token.value = res.token
    return fetchInfo()
  })

  const fetchInfo = () => getDriverInfo().then(data => {
    info.value = data
    avatar.value = data.avatar || defAva
    return data
  })

  // 页面进入时调用：未登录跳登录页
  const ensureLogin = () => {
    if (!token.value) {
      uni.reLaunch({ url: '/pages/login' })
      return Promise.reject(new Error('not login'))
    }
    return info.value ? Promise.resolve(info.value) : fetchInfo()
  }

  const logOut = () => {
    return logout().finally(() => {
      token.value = ''
      info.value = null
      avatar.value = defAva
      removeToken()
    })
  }

  return { token, info, avatar, loginBy, fetchInfo, ensureLogin, logOut }
})
