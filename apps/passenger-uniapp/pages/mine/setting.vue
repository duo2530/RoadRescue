<template>
  <view class="page">
    <zd-nav-bar title="设置"></zd-nav-bar>

    <view class="menu">
      <view class="menu-item" @click="openText('用户协议', USER_AGREEMENT_TEXT)">
        <text class="menu-text">用户协议</text>
        <uni-icons type="right" size="16" color="#999"></uni-icons>
      </view>
      <view class="menu-item" @click="openText('隐私政策', PRIVACY_TEXT)">
        <text class="menu-text">隐私政策</text>
        <uni-icons type="right" size="16" color="#999"></uni-icons>
      </view>
    </view>

    <view class="logout" @click="logout">退出登录</view>
  </view>
</template>

<script setup>
  import { useUserStore } from '@/store'
  import { USER_AGREEMENT_TEXT, PRIVACY_TEXT } from '@/utils/texts'

  function openText(title, content) {
    uni.navigateTo({ url: `/pages/common/textview?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}` })
  }

  function logout() {
    uni.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: res => {
        if (!res.confirm) return
        // 演示版：清除登录态后回到首页，首页会自动重新登录
        useUserStore().logOut().then(() => {
          uni.reLaunch({ url: '/pages/index' })
        })
      }
    })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #fff;
  }

  .menu-item {
    height: r(56);
    padding: 0 r(20) 0 r(19);
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f3f3f3;
  }

  .menu-text {
    flex: 1;
    font-size: r(13.5);
    font-weight: 600;
    color: #171925;
  }

  .logout {
    margin: r(53) r(42) 0;
    height: r(46);
    border-radius: r(23);
    background: #3e86f4;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(14.5);
    color: #fff;
  }
</style>
