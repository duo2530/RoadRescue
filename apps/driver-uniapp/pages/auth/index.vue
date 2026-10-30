<template>
  <view class="page">
    <zd-nav-bar title="小板运输司机认证"></zd-nav-bar>

    <image class="banner" src="/static/images/driver/auth-banner.jpg" mode="widthFix"></image>

    <view class="body">
      <view class="policy">
        <image class="policy-icon" src="/static/images/driver/icon-warn-red.png" mode="aspectFit"></image>
        <text class="policy-text">{{ POLICY_TEXT }}</text>
      </view>

      <view class="steps">
        <view v-for="(s, i) in steps" :key="s.key" class="step">
          <view class="step-side">
            <image class="step-icon" :src="'/static/images/driver/step-' + (i + 1) + '.png'" mode="aspectFit"></image>
            <view v-if="i < steps.length - 1" class="step-line"></view>
          </view>
          <view class="step-main">
            <view class="step-head" @click="goStep(s, i)">
              <text class="step-title">{{ s.title }}</text>
              <text v-if="s.done" class="step-done">已提交</text>
              <uni-icons type="right" size="17" color="#343434"></uni-icons>
            </view>
            <view class="step-biz">
              <image class="lock" src="/static/images/driver/icon-lock.png" mode="aspectFit"></image>
              <text class="biz-name">{{ s.business }}</text>
              <text class="biz-status" :class="{ unlocked: s.done }">{{ s.done ? '已解锁' : '待解锁' }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="footer">
      <view class="btn" @click="next">{{ allDone ? '认证已完成' : '继续补充信息' }}</view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { getAuthStatus } from '@/api/auth'
  import { useUserStore } from '@/store'
  import { POLICY_TEXT } from '@/utils/texts'

  // 每个步骤对应的填写页
  const STEP_PAGES = {
    personal: '/pages/auth/personal',
    vehicle: '/pages/auth/vehicle',
    insurance: '/pages/auth/insurance'
  }

  const steps = ref([
    { key: 'personal', title: '个人信息', business: '小修业务', done: false },
    { key: 'vehicle', title: '驾驶证、行驶证、车辆照片', business: '短途拖车业务', done: false },
    { key: 'insurance', title: '责任险保单', business: '长途拖车业务、物流业务', done: false }
  ])
  const allDone = computed(() => steps.value.every(s => s.done))

  onShow(() => {
    useUserStore().ensureLogin().then(getAuthStatus).then(res => {
      steps.value = res.steps
    }).catch(() => {})
  })

  function goStep(s, i) {
    const prev = steps.value.slice(0, i).find(x => !x.done)
    if (prev) {
      uni.showToast({ title: '请先完成「' + prev.title + '」', icon: 'none' })
      return
    }
    uni.navigateTo({ url: STEP_PAGES[s.key] })
  }

  function next() {
    const i = steps.value.findIndex(s => !s.done)
    if (i < 0) {
      uni.showToast({ title: '全部业务已解锁，快去抢单吧', icon: 'none' })
      return
    }
    goStep(steps.value[i], i)
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
    padding-bottom: calc(#{r(86)} + env(safe-area-inset-bottom));
  }

  .banner {
    display: block;
    width: 100%;
  }

  .body {
    position: relative;
    background: #fff;
    min-height: r(520);
  }

  /* 实测：粉底 238.3-326.3pt，文字 x=40、三行，图标 x=16 垂直居中 */
  .policy {
    display: flex;
    align-items: center;
    padding: r(14.5) r(19) r(14.5) r(16);
    background: #fff0ee;
  }

  .policy-icon {
    flex-shrink: 0;
    width: r(13);
    height: r(13);
    margin-right: r(9);
  }

  .policy-text {
    font-size: r(13.6);
    line-height: r(20);
    font-weight: bold;
    color: #ff6337;
  }

  .steps {
    padding: r(37) r(30) 0;
    background: linear-gradient(180deg, #fffbf3 0, #fff r(11));
  }

  .step {
    display: flex;
  }

  .step-side {
    width: r(36);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .step-icon {
    width: r(36);
    height: r(36);
  }

  .step-line {
    flex: 1;
    width: 1px;
    margin: r(1) 0;
    background: #b2c2e4;
  }

  .step-main {
    flex: 1;
    margin-left: r(15);
    padding-bottom: r(40);
  }

  .step-head {
    height: r(26);
    margin-top: r(1);
    display: flex;
    align-items: center;
  }

  .step-title {
    flex: 1;
    font-size: r(18);
    font-weight: bold;
    color: #33495e;
  }

  .step-done {
    margin-right: r(6);
    font-size: r(12);
    color: #3cb371;
  }

  .step-biz {
    min-height: r(47.5);
    margin-top: r(13);
    padding: 0 0 0 r(2.3);
    display: flex;
    align-items: center;
    border-radius: r(4);
    background: #fafbfc;
  }

  .lock {
    width: r(21);
    height: r(24);
  }

  .biz-name {
    flex: 1;
    margin-left: r(12.3);
    font-size: r(15);
    color: #71808f;
  }

  .biz-status {
    font-size: r(14);
    color: #ff4444;
  }

  .biz-status.unlocked {
    color: #3cb371;
  }

  .footer {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    padding: r(20) r(40) calc(#{r(20)} + env(safe-area-inset-bottom));
    background: $zd-bg;
  }

  .btn {
    height: r(46);
    line-height: r(46);
    border-radius: r(23);
    text-align: center;
    font-size: r(15);
    font-weight: normal;
    color: #fff;
    background: $zd-btn;
  }
</style>
