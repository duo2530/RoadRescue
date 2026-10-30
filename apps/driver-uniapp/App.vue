<script setup>
  import { onLaunch } from '@dcloudio/uni-app'
  import { getToken } from '@/utils/auth'
  import { useUserStore } from '@/store'

  onLaunch(() => {
    // 已登录则拉取司机信息；未登录时各页面通过 ensureLogin 跳转登录页
    if (getToken()) useUserStore().fetchInfo().catch(() => {})
  })
</script>

<style lang="scss">
  @import '@/static/scss/index.scss';
</style>
