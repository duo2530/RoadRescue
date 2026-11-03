import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, getUserInfo, logout } from '@/api/user'
import { getToken, removeToken, setToken } from '@/utils/auth'
import defAva from '@/static/images/rescue/avatar.png'

export const useUserStore = defineStore('user', () => {
  const token = ref(getToken())
  const phone = ref('')
  const avatar = ref(defAva)

  let pending = null

  const clearSession = () => {
    token.value = ''
    phone.value = ''
    pending = null
    removeToken()
  }

  const toLogin = () => {
    const pages = getCurrentPages()
    const current = pages.length ? pages[pages.length - 1].route : ''
    if (current !== 'pages/login') uni.reLaunch({ url: '/pages/login' })
  }

  const fetchUser = () => getUserInfo({ silent401: true }).then(user => {
    phone.value = user.phone
    avatar.value = user.avatar || defAva
    return user
  })

  const loginBy = data => login(data).then(res => {
    setToken(res.token)
    token.value = res.token
    pending = null
    return fetchUser()
  })

  // 页面进入时调用：没有登录态或登录已过期则跳转登录页
  const ensureLogin = () => {
    if (!token.value) {
      toLogin()
      return Promise.reject(new Error('not login'))
    }
    if (!pending) {
      pending = fetchUser().catch(err => {
        pending = null
        if (err && err.code === 401) {
          clearSession()
          toLogin()
        }
        throw err
      })
    }
    return pending
  }

  const logOut = () => logout().catch(() => {}).finally(clearSession)

  return { token, phone, avatar, loginBy, ensureLogin, logOut }
})
