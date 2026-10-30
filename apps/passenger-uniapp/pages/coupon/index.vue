<template>
  <view class="page">
    <zd-nav-bar title="我的优惠券"></zd-nav-bar>
    <view class="tabs" :style="{ top: navHeight + 'px' }">
      <view v-for="t in tabs" :key="t.value" class="tab" :class="{ active: t.value === status }" @click="switchTab(t.value)">
        <text>{{ t.label }}</text>
      </view>
    </view>
    <view class="tabs-placeholder"></view>

    <view class="list">
      <view v-for="c in list" :key="c.id" class="coupon" :class="{ invalid: status !== 'unused' }">
        <view class="coupon-left">
          <text class="coupon-value"><text class="coupon-yen">¥</text>{{ c.value }}</text>
          <text class="coupon-threshold">{{ c.threshold ? `满${c.threshold}可用` : '无门槛' }}</text>
        </view>
        <view class="coupon-right">
          <text class="coupon-name">{{ c.name }}</text>
          <text class="coupon-date">有效期至 {{ formatDate(c.expireTime) }}</text>
        </view>
        <text v-if="status !== 'unused'" class="coupon-tag">{{ status === 'used' ? '已使用' : '已过期' }}</text>
      </view>
    </view>

    <view class="redeem-placeholder"></view>
    <view class="redeem">
      <input class="redeem-input" v-model="code" placeholder="请输入兑换码" placeholder-class="redeem-ph" />
      <view class="redeem-btn" @click="redeem">兑换</view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { listCoupon, redeemCoupon } from '@/api/coupon'
  import { formatDate } from '@/utils/order'

  const navHeight = (uni.getSystemInfoSync().statusBarHeight || 0) + 44
  const tabs = [
    { label: '未使用', value: 'unused' },
    { label: '已使用', value: 'used' },
    { label: '已过期', value: 'expired' }
  ]
  const status = ref('unused')
  const list = ref([])
  const code = ref('')

  function load() {
    listCoupon(status.value).then(res => {
      list.value = res
    })
  }

  function switchTab(value) {
    status.value = value
    list.value = []
    load()
  }

  function redeem() {
    if (!code.value.trim()) {
      uni.showToast({ title: '请输入兑换码', icon: 'none' })
      return
    }
    redeemCoupon(code.value).then(() => {
      uni.showToast({ title: '兑换成功', icon: 'success' })
      code.value = ''
      switchTab('unused')
    }).catch(() => {})
  }

  onShow(load)
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #fafafa;
  }

  .tabs {
    position: fixed;
    left: 0;
    right: 0;
    z-index: 98;
    height: r(46);
    display: flex;
    background: #fff;
  }

  .tabs-placeholder {
    height: r(46);
  }

  .tab {
    flex: 1;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(14.5);
    color: #838383;

    &.active {
      color: #1f1f1f;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: r(6);
        transform: translateX(-50%);
        width: r(10);
        height: r(2);
        border-radius: r(1);
        background: #3361ae;
      }
    }
  }

  .list {
    padding: r(12);
  }

  .coupon {
    position: relative;
    display: flex;
    align-items: center;
    height: r(80);
    margin-bottom: r(10);
    background: #fff;
    border-radius: r(8);
    overflow: hidden;
  }

  .coupon-left {
    width: r(100);
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #3963b5, #4f8cf0);
    color: #fff;
  }

  .coupon-value {
    font-size: r(26);
    font-weight: 600;
  }

  .coupon-yen {
    font-size: r(14);
  }

  .coupon-threshold {
    margin-top: r(2);
    font-size: r(11);
  }

  .coupon-right {
    flex: 1;
    padding: 0 r(14);
    display: flex;
    flex-direction: column;
  }

  .coupon-name {
    font-size: r(14);
    font-weight: 500;
    color: #222;
  }

  .coupon-date {
    margin-top: r(8);
    font-size: r(11);
    color: #999;
  }

  .coupon-tag {
    position: absolute;
    right: r(12);
    top: r(10);
    font-size: r(11);
    color: #999;
  }

  .coupon.invalid .coupon-left {
    background: #c8c8c8;
  }

  .redeem-placeholder {
    height: calc(#{r(85)} + env(safe-area-inset-bottom));
  }

  .redeem {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    padding: r(14) r(30) calc(#{r(20)} + env(safe-area-inset-bottom));
    display: flex;
    align-items: center;
    background: #fefefe;
    border-top: 1px solid #ebebeb;
  }

  .redeem-input {
    flex: 1;
    height: r(36);
    padding: 0 r(11);
    border: 1px solid #cfcfcf;
    border-radius: r(3);
    font-size: r(12);
  }

  .redeem-btn {
    width: r(72);
    height: r(36);
    margin-left: r(55);
    border-radius: r(3);
    background: #3963b5;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(12);
    color: #fff;
  }
</style>

<style>
  .redeem-ph {
    color: #999;
  }
</style>
