<template>
  <view class="tab-bar-placeholder"></view>
  <view class="tab-bar">
    <view v-for="item in items" :key="item.key" class="tab-item" @click="switchTo(item)">
      <image class="tab-icon" :src="current === item.key ? item.activeIcon : item.icon" mode="aspectFit"></image>
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
  import orderIcon from '@/static/images/rescue/tab-order.png'
  import orderActiveIcon from '@/static/images/rescue/tab-order-active.png'
  import mineIcon from '@/static/images/rescue/tab-mine.png'
  import mineActiveIcon from '@/static/images/rescue/tab-mine-active.png'

  const props = defineProps({
    current: { type: String, default: 'order' }
  })

  const items = [
    { key: 'order', text: '订单', url: '/pages/order/index', icon: orderIcon, activeIcon: orderActiveIcon },
    { key: 'mine', text: '我的', url: '/pages/mine/index', icon: mineIcon, activeIcon: mineActiveIcon }
  ]

  function switchTo(item) {
    if (item.key === props.current) return
    uni.redirectTo({ url: item.url })
  }
</script>

<style lang="scss" scoped>
  .tab-bar-placeholder {
    height: calc(62px + env(safe-area-inset-bottom));
  }

  .tab-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 99;
    display: flex;
    background: #fff;
    border-top: 1px solid #efeeee;
    padding-bottom: env(safe-area-inset-bottom);
  }

  .tab-item {
    flex: 1;
    height: 62px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  .tab-icon {
    width: 22px;
    height: 22px;
  }

  .tab-text {
    margin-top: 3px;
    font-size: 16px;
    color: #000;
  }
</style>
