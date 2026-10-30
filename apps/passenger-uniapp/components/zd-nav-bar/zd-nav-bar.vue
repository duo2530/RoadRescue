<template>
  <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px', background }">
    <view class="nav-inner">
      <view v-if="back" class="nav-back" @click="goBack">
        <uni-icons type="left" size="22" color="#000"></uni-icons>
      </view>
      <text class="nav-title">{{ title }}</text>
    </view>
  </view>
  <view v-if="placeholder" :style="{ height: statusBarHeight + 44 + 'px' }"></view>
</template>

<script setup>
  /**
   * 自定义导航栏：截图中的 tab 页（订单/我的）也带返回箭头，原生 tabBar 页做不到，故全部自定义。
   */
  const props = defineProps({
    title: { type: String, default: '' },
    back: { type: Boolean, default: true },
    background: { type: String, default: '#ffffff' },
    // 是否占位（fixed 导航下方留出高度）
    placeholder: { type: Boolean, default: true }
  })

  const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

  function goBack() {
    if (getCurrentPages().length > 1) {
      uni.navigateBack()
    } else {
      uni.reLaunch({ url: '/pages/index' })
    }
  }
</script>

<style lang="scss" scoped>
  .nav-bar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 99;
  }

  .nav-inner {
    position: relative;
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .nav-back {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .nav-title {
    font-size: 17px;
    font-weight: 500;
    color: #000;
  }
</style>
