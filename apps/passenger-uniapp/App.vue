<script setup>
  import { onLaunch } from '@dcloudio/uni-app'
  import { useUserStore } from '@/store'

  onLaunch(() => {
    // 没有登录态或登录已过期会跳转登录页
    useUserStore().ensureLogin().catch(() => {})
  })
</script>

<style lang="scss">
  @import '@/static/scss/index.scss';
</style>
