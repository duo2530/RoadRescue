<template>
  <view class="page">
    <zd-nav-bar title="服务查询"></zd-nav-bar>
    <view class="tabs" :style="{ top: navHeight + 'px' }">
      <view v-for="t in tabs" :key="t.value" class="tab" :class="{ active: t.value === type }" @click="switchTab(t.value)">
        <text class="tab-text">{{ t.label }}</text>
      </view>
    </view>
    <view class="tabs-placeholder"></view>

    <view class="list">
      <view v-for="o in list" :key="o.id" class="order" @click="goDetail(o)">
        <view class="order-head">
          <text class="order-service">{{ serviceName(o.serviceType) }}</text>
          <text class="order-status" :class="'status-' + o.status">{{ statusText[o.status] }}</text>
        </view>
        <view class="order-line">
          <view class="dot dot-start"></view>
          <text class="order-addr">{{ o.startAddress.name }}</text>
        </view>
        <view v-if="o.endAddress" class="order-line">
          <view class="dot dot-end"></view>
          <text class="order-addr">{{ o.endAddress.name }}</text>
        </view>
        <view class="order-foot">
          <text class="order-time">{{ formatTime(o.createTime) }}</text>
          <text class="order-price">¥{{ o.payAmount }}</text>
        </view>
      </view>
    </view>

    <zd-tab-bar current="order"></zd-tab-bar>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onShow, onHide, onPullDownRefresh } from '@dcloudio/uni-app'
  import { listOrder } from '@/api/order'
  import { serviceName, statusText, formatTime } from '@/utils/order'

  const navHeight = (uni.getSystemInfoSync().statusBarHeight || 0) + 44
  const tabs = [
    { label: '进行中服务', value: 'ongoing' },
    { label: '历史服务', value: 'history' }
  ]
  const type = ref('ongoing')
  const list = ref([])
  let timer = null

  function load() {
    return listOrder(type.value).then(res => {
      list.value = res
    })
  }

  function switchTab(value) {
    if (value === type.value) return
    type.value = value
    list.value = []
    load()
  }

  function goDetail(o) {
    uni.navigateTo({ url: '/pages/order/detail?id=' + o.id })
  }

  onShow(() => {
    load()
    // 进行中的订单状态会随时间推进，定时刷新
    timer = setInterval(() => {
      if (type.value === 'ongoing') load()
    }, 5000)
  })

  onHide(() => clearInterval(timer))
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #f1f3f7;
  }

  .tabs {
    position: fixed;
    left: 0;
    right: 0;
    z-index: 98;
    height: r(53);
    display: flex;
    background: #fff;
  }

  .tabs-placeholder {
    height: r(53);
  }

  .tab {
    flex: 1;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .tab-text {
    font-size: r(15.5);
    color: #282a30;
  }

  .tab.active {
    .tab-text {
      color: #0a59a3;
      font-weight: 600;
    }

    &::after {
      content: '';
      position: absolute;
      left: 50%;
      bottom: r(4);
      transform: translateX(-50%);
      width: r(71);
      height: r(4);
      border-radius: r(2);
      background: #0a59a3;
    }
  }

  .list {
    padding: r(10) r(12);
  }

  .order {
    margin-bottom: r(10);
    padding: r(12) r(14);
    background: #fff;
    border-radius: r(8);
  }

  .order-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: r(8);
  }

  .order-service {
    font-size: r(15);
    font-weight: 600;
    color: #000;
  }

  .order-status {
    font-size: r(12);
    color: #999;

    &.status-waiting,
    &.status-dispatched,
    &.status-rescuing {
      color: $zd-blue;
    }
  }

  .order-line {
    display: flex;
    align-items: center;
    margin-top: r(5);
  }

  .dot {
    width: r(6);
    height: r(6);
    border-radius: 50%;
    margin-right: r(8);
    flex-shrink: 0;
  }

  .dot-start {
    background: #3dae5b;
  }

  .dot-end {
    background: #f6594a;
  }

  .order-addr {
    font-size: r(13);
    color: #333;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .order-foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: r(10);
    padding-top: r(10);
    border-top: 1px solid #f1f2f5;
  }

  .order-time {
    font-size: r(12);
    color: #999;
  }

  .order-price {
    font-size: r(15);
    font-weight: 600;
    color: #ff3535;
  }
</style>
