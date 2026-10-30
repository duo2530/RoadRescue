<template>
  <view class="nav-bar" :style="{ paddingTop: statusBarHeight + 'px', background }">
    <view class="nav-inner">
      <view v-if="back" class="nav-back" @click="goBack()">
        <uni-icons type="left" size="20" :color="color"></uni-icons>
      </view>
      <text class="nav-title" :style="{ color }">{{ title }}</text>
    </view>
  </view>
  <view v-if="placeholder" :style="{ height: statusBarHeight + 44 + 'px' }"></view>
</template>

<script setup>
  /**
   * 自定义导航栏：截图中导航栏为深蓝底白字，部分 tab 页无返回箭头，统一自定义。
   */
  import { goBack } from '@/utils/common'
  const props = defineProps({
    title: { type: String, default: '' },
    back: { type: Boolean, default: true },
    background: { type: String, default: '#2c395f' },
    color: { type: String, default: '#ffffff' },
    // 是否占位（fixed 导航下方留出高度）
    placeholder: { type: Boolean, default: true }
  })

  // H5 没有系统状态栏，按截图（iPhone 状态栏 47pt）留出同样高度，保持页面比例
  const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 47

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
    font-size: 15px;
    font-weight: normal;
  }
</style>
