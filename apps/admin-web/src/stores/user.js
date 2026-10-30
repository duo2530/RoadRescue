import { defineStore } from 'pinia'
import { getUserInfo } from '../api'
import { TOKEN_KEY } from '../utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({ name: '', avatar: '', buttons: [] }),
  actions: {
    async load() {
      const info = await getUserInfo()
      this.name = info?.name || ''
      this.avatar = info?.avatar || ''
      this.buttons = info?.buttons || []
    },
    logout() {
      localStorage.removeItem(TOKEN_KEY)
      this.$reset()
    }
  }
})
