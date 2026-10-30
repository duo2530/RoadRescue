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

  // 演示版无登录页：没有 token 时自动登录，再拉取用户信息
  const ensureLogin = () => {
    if (!pending) {
      const doLogin = () => login({}).then(res => {
        setToken(res.token)
        token.value = res.token
      })
      // 本地 token 已过期（401）时清掉并重新登录一次，避免停在“登录已过期”
      const loadUser = () => getUserInfo({ silent401: true }).catch(err => {
        if (!err || err.code !== 401) throw err
        removeToken()
        token.value = ''
        return doLogin().then(() => getUserInfo())
      })
      pending = (token.value ? Promise.resolve() : doLogin()).then(loadUser).then(user => {
        phone.value = user.phone
        avatar.value = user.avatar || defAva
        return user
      }).catch(err => {
        pending = null
        throw err
      })
    }
    return pending
  }

  const logOut = () => {
    return logout().finally(() => {
      token.value = ''
      phone.value = ''
      pending = null
      removeToken()
    })
  }

  return { token, phone, avatar, ensureLogin, logOut }
})
