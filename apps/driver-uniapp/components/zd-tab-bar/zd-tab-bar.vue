<template>
  <view class="tab-bar-placeholder"></view>
  <view class="tab-bar">
    <view v-for="item in items" :key="item.key" class="tab-item" @click="switchTo(item)">
      <image class="tab-icon" :class="'tab-icon-' + item.key" :src="current === item.key ? item.activeIcon : item.icon" mode="aspectFit"></image>
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
  import hallIcon from '@/static/images/driver/tab-hall.png'
  import hallActiveIcon from '@/static/images/driver/tab-hall-active.png'
  import orderIcon from '@/static/images/driver/tab-order.png'
  import orderActiveIcon from '@/static/images/driver/tab-order-active.png'
  import mineIcon from '@/static/images/driver/tab-mine.png'
  import mineActiveIcon from '@/static/images/driver/tab-mine-active.png'

  /** 自定义 tabBar：截图中各 tab 图标宽度不同，且只有三个主页面带 tab，故不用原生 tabBar */
  const props = defineProps({
    current: { type: String, default: 'hall' }
  })

  const items = [
    { key: 'hall', text: '抢单大厅', url: '/pages/hall/index', icon: hallIcon, activeIcon: hallActiveIcon },
    { key: 'order', text: '我的订单', url: '/pages/order/index', icon: orderIcon, activeIcon: orderActiveIcon },
    { key: 'mine', text: '我的', url: '/pages/mine/index', icon: mineIcon, activeIcon: mineActiveIcon }
  ]

  function switchTo(item) {
    if (item.key === props.current) return
    uni.redirectTo({ url: item.url })
  }
</script>

<style lang="scss" scoped>
  .tab-bar-placeholder {
    height: calc(#{r(48)} + env(safe-area-inset-bottom));
  }

  .tab-bar {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 99;
    display: flex;
    background: #fff;
    padding-bottom: env(safe-area-inset-bottom);
  }

  .tab-item {
    flex: 1;
    height: r(48);
    padding-top: r(5.3);
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .tab-icon {
    height: r(22);
    width: r(21);
  }

  .tab-icon-hall {
    width: r(30);
  }

  .tab-icon-order {
    width: r(20);
  }

  .tab-text {
    margin-top: r(4.5);
    font-size: r(10);
    line-height: r(12);
    color: #707483;
  }
</style>
