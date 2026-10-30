<template>
  <view class="page">
    <view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="title-bar">
        <text class="title">我的订单</text>
      </view>
      <view class="tabs">
        <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: status === t.key }" @click="switchTab(t.key)">
          <text class="tab-text">{{ t.text }}</text>
          <view class="tab-line"></view>
        </view>
      </view>
    </view>
    <view :style="{ height: statusBarHeight + 'px' }"></view>
    <view class="header-placeholder"></view>

    <view class="list">
      <view v-for="o in list" :key="o.id" class="card" @click="goDetail(o)">
        <view class="card-head">
          <text class="order-no">订单号：{{ o.id }}</text>
          <text class="status" :class="'status-' + o.myStatus">{{ statusText(o) }}</text>
        </view>
        <view class="route">
          <view class="route-row">
            <text class="route-tag tag-start">起</text>
            <text class="route-place">{{ o.start.label }}</text>
          </view>
          <template v-if="o.end">
            <view class="route-line"></view>
            <view class="route-row">
              <text class="route-tag tag-end">终</text>
              <text class="route-place">{{ o.end.label }}</text>
            </view>
          </template>
        </view>
        <view class="card-foot">
          <text class="info-text">{{ serviceName(o.serviceType) }}<text v-if="o.mileage"> · 约{{ o.mileage }}公里</text> · {{ formatTime(o.grabTime) }}</text>
          <text class="price">¥ {{ money(o.price) }}</text>
        </view>
      </view>

      <view v-if="loaded && !list.length" class="empty">
        <image class="empty-img" src="/static/images/driver/empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无数据</text>
      </view>
    </view>

    <zd-tab-bar current="order"></zd-tab-bar>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
  import { listMyOrders } from '@/api/order'
  import { useUserStore } from '@/store'
  import { serviceName, money, formatTime, MY_STATUS_TEXT } from '@/utils/format'

  // H5 没有系统状态栏，按截图（iPhone 状态栏 47pt）留出同样高度，保持页面比例
  const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 47

  const tabs = [
    { key: '', text: '全部订单' },
    { key: 'waiting', text: '待发车' },
    { key: 'shipping', text: '运输中' },
    { key: 'finished', text: '已完成' }
  ]
  const status = ref('')
  const list = ref([])
  const loaded = ref(false)

  onShow(() => {
    useUserStore().ensureLogin().then(load).catch(() => {})
  })

  onPullDownRefresh(() => {
    load().finally(() => uni.stopPullDownRefresh())
  })

  function load() {
    return listMyOrders(status.value).then(data => {
      list.value = data
      loaded.value = true
    })
  }

  function switchTab(key) {
    if (status.value === key) return
    status.value = key
    loaded.value = false
    list.value = []
    load()
  }

  function statusText(o) {
    if (o.myStatus === 'finished') return o.audited ? '已完成' : '运费审核中'
    return MY_STATUS_TEXT[o.myStatus]
  }

  function goDetail(o) {
    uni.navigateTo({ url: '/pages/order/detail?id=' + o.id })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
  }

  .header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 99;
    background: $zd-navy;
  }

  .header-placeholder {
    height: calc(44px + #{r(64)});
  }

  .title-bar {
    height: 44px;
    margin-bottom: r(9);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .title {
    font-size: r(18);
    font-weight: bold;
    color: #fff;
  }

  .tabs {
    display: flex;
    height: r(55);
  }

  .tab {
    flex: 1;
    padding-top: r(13);
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .tab-text {
    font-size: r(13);
    line-height: r(13);
    color: #c0c4cf;
  }

  .tab-line {
    margin-top: r(6);
    width: r(10);
    height: r(2);
    border-radius: r(1);
  }

  .tab.active .tab-text {
    color: #fff;
  }

  .tab.active .tab-line {
    background: #fff;
  }

  .list {
    padding: r(10);
  }

  .card {
    margin-bottom: r(10);
    padding: r(14) r(16);
    background: #fff;
    border-radius: r(10);
  }

  .card-head {
    display: flex;
    align-items: center;
    padding-bottom: r(12);
    margin-bottom: r(12);
    border-bottom: 1px solid #f4f4f4;
  }

  .order-no {
    font-size: r(12);
    color: #999;
  }

  .status {
    margin-left: auto;
    font-size: r(13);
    font-weight: bold;
  }

  .status-waiting {
    color: #f57c21;
  }

  .status-shipping {
    color: #2b84ff;
  }

  .status-finished {
    color: #3cb371;
  }

  .route-row {
    display: flex;
    align-items: center;
    height: r(18);
  }

  .route-tag {
    width: r(20);
    height: r(18);
    line-height: r(18);
    border-radius: r(4);
    text-align: center;
    font-size: r(12);
    color: #fff;
  }

  .tag-start {
    background: #2b84ff;
  }

  .tag-end {
    background: #e85f48;
  }

  .route-place {
    margin-left: r(6);
    font-size: r(15);
    font-weight: bold;
    color: #262626;
  }

  .route-line {
    margin: r(1) 0 r(1) r(10);
    width: 1px;
    height: r(5);
    background: #c8c8c8;
  }

  .card-foot {
    margin-top: r(12);
    display: flex;
    align-items: center;
  }

  .info-text {
    font-size: r(12);
    color: #888;
  }

  .price {
    margin-left: auto;
    font-size: r(15);
    font-weight: bold;
    color: $zd-orange;
  }

  .empty {
    padding-top: r(97);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .empty-img {
    width: r(290);
    height: r(92.7);
  }

  .empty-text {
    margin-top: r(3);
    font-size: r(11);
    line-height: r(13);
    color: #b6b7b8;
  }
</style>
