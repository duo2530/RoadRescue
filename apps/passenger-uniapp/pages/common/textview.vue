<template>
  <view class="page">
    <zd-nav-bar :title="title"></zd-nav-bar>
    <view class="content">
      <text>{{ content }}</text>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'

  const title = ref('')
  const content = ref('')

  onLoad(options => {
    title.value = decodeURIComponent(options.title || '')
    content.value = decodeURIComponent(options.content || '')
  })
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #fff;
  }

  .content {
    padding: r(16) r(18) r(30);
    font-size: r(14);
    line-height: r(24);
    color: #333;
    white-space: pre-wrap;
  }
</style>
