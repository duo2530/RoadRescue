<script setup>
  import { onLaunch } from '@dcloudio/uni-app'
  import { useUserStore } from '@/store'

  onLaunch(() => {
    // 演示版：没有登录页，启动时自动获取（mock 下为预置）用户信息
    useUserStore().ensureLogin()
  })
</script>

<style lang="scss">
  @import '@/static/scss/index.scss';
</style>
