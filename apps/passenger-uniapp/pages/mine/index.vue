<template>
  <view class="page">
    <zd-nav-bar title="我的"></zd-nav-bar>

    <view class="header">
      <image class="header-bg" src="/static/images/rescue/mine-header.jpg" mode="aspectFill"></image>
      <image class="avatar" :src="userStore.avatar" mode="aspectFill"></image>
      <text class="account">账号：{{ userStore.phone || '--' }}</text>
    </view>

    <view class="menu">
      <view v-for="(m, i) in menus" :key="m.text" class="menu-item" @click="m.action">
        <image class="menu-icon" :src="'/static/images/rescue/menu-' + i + '.png'" mode="aspectFit"></image>
        <text class="menu-text">{{ m.text }}</text>
        <uni-icons type="right" size="14" color="#555"></uni-icons>
      </view>
    </view>

    <zd-tab-bar current="mine"></zd-tab-bar>

    <!-- 客服电话弹层（还原截图样式，uni.showActionSheet 在 H5 下样式差异较大） -->
    <view v-if="sheetVisible" class="sheet-mask" @click="sheetVisible = false">
      <view class="sheet" @click.stop>
        <view class="sheet-title">{{ servicePhone }}</view>
        <view class="sheet-item" @click="call">呼叫</view>
        <view class="sheet-gap"></view>
        <view class="sheet-item" @click="sheetVisible = false">取消</view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import config from '@/config'
  import { useUserStore } from '@/store'

  const userStore = useUserStore()
  userStore.ensureLogin()

  const servicePhone = config.appInfo.servicePhone

  const menus = [
    { text: '客服', action: callService },
    { text: '设置', action: () => uni.navigateTo({ url: '/pages/mine/setting' }) },
    { text: '优惠券', action: () => uni.navigateTo({ url: '/pages/coupon/index' }) },
    { text: '开票历史', action: () => uni.showToast({ title: '暂无开票记录', icon: 'none' }) }
  ]

  const sheetVisible = ref(false)

  function callService() {
    sheetVisible.value = true
  }

  function call() {
    sheetVisible.value = false
    uni.makePhoneCall({ phoneNumber: servicePhone })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #fff;
  }

  .header {
    position: relative;
    height: r(133);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .header-bg {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
  }

  .avatar {
    position: relative;
    margin-top: r(26);
    width: r(57);
    height: r(57);
    border-radius: 50%;
    border: r(2) solid rgba(255, 255, 255, 0.7);
    box-sizing: border-box;
  }

  .account {
    position: relative;
    margin-top: r(20);
    font-size: r(13);
    font-weight: 400;
    color: #fff;
  }

  .menu-item {
    height: r(56);
    margin-left: r(18);
    padding-right: r(22);
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f2f2f3;
  }

  .menu-icon {
    width: r(15);
    height: r(15);
  }

  .menu-text {
    flex: 1;
    margin-left: r(9);
    font-size: r(14.5);
    font-weight: 600;
    color: #323643;
  }

  .sheet-mask {
    position: fixed;
    left: 0;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 999;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: flex-end;
  }

  .sheet {
    width: 100%;
    padding-bottom: env(safe-area-inset-bottom);
    background: #fff;
    border-radius: r(12) r(12) 0 0;
    overflow: hidden;
  }

  .sheet-title {
    height: r(64);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(12.5);
    color: #888;
    border-bottom: 1px solid #f0f0f0;
  }

  .sheet-item {
    height: r(56);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(15.5);
    color: #000;
  }

  .sheet-gap {
    height: r(8);
    background: #f7f7f7;
  }
</style>
