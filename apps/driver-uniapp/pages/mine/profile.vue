<template>
  <view class="page">
    <zd-nav-bar title="我的资料"></zd-nav-bar>

    <view class="group">
      <view class="row">
        <text class="label">头像</text>
        <image class="avatar" :src="userStore.avatar" mode="aspectFill"></image>
      </view>
      <view class="row">
        <text class="label">昵称</text>
        <text class="value">{{ info.nickname }}</text>
      </view>
      <view class="row">
        <text class="label">手机号</text>
        <text class="value">{{ info.phone }}</text>
      </view>
    </view>

    <view class="group">
      <view class="group-title">认证信息</view>
      <view v-for="s in auth" :key="s.key" class="row" @click="goAuth">
        <text class="label wide">{{ s.title }}</text>
        <text class="value" :class="s.done ? 'ok' : 'todo'">{{ s.done ? '已认证' : '未认证' }}</text>
        <uni-icons type="right" size="13" color="#999"></uni-icons>
      </view>
    </view>

    <view class="group">
      <view class="row" @click="reset">
        <text class="label wide">重置演示数据</text>
        <text class="value todo-gray">清空订单、认证和钱包</text>
        <uni-icons type="right" size="13" color="#999"></uni-icons>
      </view>
    </view>

    <view class="logout" @click="logout">退出登录</view>
  </view>
</template>

<script setup>
  import { computed } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { useUserStore } from '@/store'
  import { resetMock } from '@/api/user'

  const userStore = useUserStore()
  const info = computed(() => userStore.info || {})
  const auth = computed(() => info.value.auth || [])

  onShow(() => {
    userStore.ensureLogin().then(() => userStore.fetchInfo()).catch(() => {})
  })

  function goAuth() {
    uni.navigateTo({ url: '/pages/auth/index' })
  }

  function reset() {
    uni.showModal({
      title: '重置演示数据',
      content: '将恢复初始订单，清空认证资料、我的订单和钱包，并退出登录。',
      success: r => {
        if (!r.confirm) return
        resetMock().then(() => userStore.logOut()).then(() => uni.reLaunch({ url: '/pages/login' }))
      }
    })
  }

  function logout() {
    uni.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success: r => {
        if (!r.confirm) return
        userStore.logOut().then(() => uni.reLaunch({ url: '/pages/login' }))
      }
    })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
    padding-top: r(10);
  }

  .group {
    margin: 0 r(12) r(10);
    padding: 0 r(16);
    background: #fff;
    border-radius: r(10);
  }

  .group-title {
    padding-top: r(14);
    font-size: r(13);
    color: #999;
  }

  .row {
    min-height: r(52);
    display: flex;
    align-items: center;
  }

  .row + .row {
    border-top: 1px solid #f4f4f4;
  }

  .label {
    width: r(80);
    font-size: r(15);
    color: $zd-text;
  }

  .label.wide {
    width: auto;
    flex: 1;
  }

  .value {
    flex: 1;
    text-align: right;
    font-size: r(14);
    color: #666;
    margin-right: r(4);
  }

  .label.wide + .value {
    flex: none;
  }

  .value.ok {
    color: #3cb371;
  }

  .value.todo {
    color: #ff4444;
  }

  .value.todo-gray {
    color: #999;
    font-size: r(12);
  }

  .avatar {
    margin-left: auto;
    width: r(40);
    height: r(40);
    border-radius: 50%;
  }

  .logout {
    margin: r(30) r(12);
    height: r(48);
    line-height: r(48);
    border-radius: r(10);
    text-align: center;
    font-size: r(16);
    color: #ff4444;
    background: #fff;
  }
</style>
