<template>
  <view class="page">
    <zd-nav-bar title="钱包流水"></zd-nav-bar>

    <view class="list">
      <view v-for="f in flows" :key="f.id" class="item">
        <view class="item-main">
          <text class="item-title">{{ f.title }}</text>
          <text class="item-remark">{{ f.remark }} · {{ formatTime(f.time) }}</text>
        </view>
        <text class="item-amount" :class="{ minus: f.amount < 0 }">{{ f.amount > 0 ? '+' : '' }}{{ money(f.amount) }}</text>
      </view>

      <view v-if="loaded && !flows.length" class="empty">
        <image class="empty-img" src="/static/images/driver/empty.png" mode="aspectFit"></image>
        <text class="empty-text">暂无流水</text>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { listFlows } from '@/api/wallet'
  import { money, formatTime } from '@/utils/format'

  const flows = ref([])
  const loaded = ref(false)

  onShow(() => {
    listFlows().then(data => {
      flows.value = data
      loaded.value = true
    })
  })
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
  }

  .list {
    padding: r(10) r(12);
  }

  .item {
    display: flex;
    align-items: center;
    padding: r(14) r(16);
    margin-bottom: r(8);
    background: #fff;
    border-radius: r(10);
  }

  .item-main {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .item-title {
    font-size: r(15);
    color: $zd-text;
  }

  .item-remark {
    margin-top: r(4);
    font-size: r(12);
    color: #999;
  }

  .item-amount {
    font-size: r(17);
    font-weight: bold;
    color: $zd-orange;
  }

  .item-amount.minus {
    color: #333;
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
    color: #b6b7b8;
  }
</style>
