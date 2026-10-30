<template>
  <view class="page">
    <zd-nav-bar title="订单详情"></zd-nav-bar>

    <view v-if="order" class="content">
      <view class="status-card">
        <text class="status-title">{{ statusTitle }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
        <view v-if="order.myStatus" class="steps">
          <view v-for="(s, i) in STEPS" :key="s.key" class="step" :class="{ done: stepIndex >= i }">
            <view class="step-dot"></view>
            <text class="step-text">{{ s.text }}</text>
            <view v-if="i < STEPS.length - 1" class="step-bar"></view>
          </view>
        </view>
      </view>

      <view class="card">
        <view class="route-row">
          <text class="route-tag tag-start">起</text>
          <view class="route-body">
            <text class="route-place">{{ order.start.label }}</text>
            <text class="route-addr">{{ order.start.address }}</text>
          </view>
        </view>
        <template v-if="order.end">
          <view class="route-line"></view>
          <view class="route-row">
            <text class="route-tag tag-end">终</text>
            <view class="route-body">
              <text class="route-place">{{ order.end.label }}</text>
              <text class="route-addr">{{ order.end.address }}</text>
            </view>
          </view>
        </template>
        <view class="nav-btn" @click="openMap">
          <uni-icons type="navigate" size="14" color="#2b84ff"></uni-icons>
          <text>导航到{{ order.myStatus === 'shipping' && order.end ? '终点' : '起点' }}</text>
        </view>
      </view>

      <view class="card">
        <view class="row"><text class="label">服务类型</text><text class="value">{{ serviceName(order.serviceType) }}</text></view>
        <view class="row"><text class="label">车辆</text><text class="value">{{ order.plateColor }} · {{ order.carModel }}</text></view>
        <view class="row"><text class="label">拖车方式</text><text class="value">{{ order.towType }}</text></view>
        <view v-if="order.mileage" class="row"><text class="label">预估里程</text><text class="value">约 {{ order.mileage }} 公里</text></view>
        <view v-if="order.remark" class="row"><text class="label">客户备注</text><text class="value">{{ order.remark }}</text></view>
        <view class="row"><text class="label">发布时间</text><text class="value">{{ formatTime(order.publishTime) }}</text></view>
        <view class="row"><text class="label">订单号</text><text class="value">{{ order.id }}</text></view>
        <view class="row"><text class="label">运费</text><text class="value price">¥ {{ money(order.price) }}</text></view>
      </view>

      <view v-if="order.myStatus" class="card">
        <view class="row">
          <text class="label">客户</text>
          <text class="value">{{ order.customerName }}</text>
        </view>
        <view class="row" @click="call">
          <text class="label">联系电话</text>
          <text class="value link">{{ order.customerPhone }}</text>
          <uni-icons type="phone-filled" size="16" color="#2b84ff"></uni-icons>
        </view>
        <view class="row"><text class="label">接单时间</text><text class="value">{{ formatTime(order.grabTime) }}</text></view>
        <view v-if="order.departTime" class="row"><text class="label">发车时间</text><text class="value">{{ formatTime(order.departTime) }}</text></view>
        <view v-if="order.finishTime" class="row"><text class="label">送达时间</text><text class="value">{{ formatTime(order.finishTime) }}</text></view>
      </view>
    </view>

    <view v-if="order && actions.length" class="footer">
      <view v-for="a in actions" :key="a.text" class="footer-btn" :class="a.type" @click="a.handler">{{ a.text }}</view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed } from 'vue'
  import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
  import { getOrder, grabOrder, departOrder, finishOrder, cancelOrder } from '@/api/order'
  import { serviceName, money, formatTime } from '@/utils/format'
  import { CODE_AUTH_REQUIRED } from '@/mock'

  const STEPS = [
    { key: 'waiting', text: '已接单' },
    { key: 'shipping', text: '运输中' },
    { key: 'finished', text: '已送达' },
    { key: 'audited', text: '运费入账' }
  ]

  const id = ref('')
  const order = ref(null)
  let timer = null

  onLoad(options => {
    id.value = options.id
  })
  onShow(() => {
    load()
    // 运费审核中时轮询，审核通过后自动刷新
    timer = setInterval(() => {
      if (order.value && order.value.myStatus === 'finished' && !order.value.audited) load()
    }, 5000)
  })
  onUnload(() => clearInterval(timer))

  function load() {
    return getOrder(id.value).then(data => {
      order.value = data
    })
  }

  const stepIndex = computed(() => {
    const o = order.value
    if (!o || !o.myStatus) return -1
    if (o.myStatus === 'finished') return o.audited ? 3 : 2
    return o.myStatus === 'shipping' ? 1 : 0
  })

  const statusTitle = computed(() => {
    const o = order.value
    if (!o.myStatus) return o.poolStatus === 'open' ? '待抢单' : '已安排'
    return ['待发车', '运输中', '运费审核中', '已完成'][stepIndex.value]
  })

  const statusDesc = computed(() => {
    const o = order.value
    if (!o.myStatus) return o.poolStatus === 'open' ? '抢单成功后可查看客户联系方式' : '该订单已由其他司机承接'
    return [
      '请尽快联系客户，到达起点后点击「确认发车」',
      '运输途中请注意安全，送达后点击「确认送达」',
      '平台审核运费中，审核通过后自动计入钱包',
      '运费已计入钱包余额'
    ][stepIndex.value]
  })

  const actions = computed(() => {
    const o = order.value
    if (!o.myStatus) {
      return o.poolStatus === 'open' ? [{ text: '立即抢单', type: 'primary', handler: grab }] : []
    }
    if (o.myStatus === 'waiting') {
      return [
        { text: '取消接单', type: 'plain', handler: cancel },
        { text: '确认发车', type: 'primary', handler: () => act(departOrder, '已发车') }
      ]
    }
    if (o.myStatus === 'shipping') {
      return [{ text: '确认送达', type: 'primary', handler: () => act(finishOrder, '已送达') }]
    }
    return []
  })

  function act(fn, title) {
    fn(id.value).then(data => {
      order.value = data
      uni.showToast({ title, icon: 'success' })
    })
  }

  function grab() {
    grabOrder(id.value).then(() => {
      uni.showToast({ title: '抢单成功', icon: 'success' })
      load()
    }).catch(res => {
      if (res && res.code === CODE_AUTH_REQUIRED) {
        uni.showModal({
          title: '暂未解锁',
          content: res.msg,
          confirmText: '去认证',
          success: r => r.confirm && uni.navigateTo({ url: '/pages/auth/index' })
        })
      } else {
        uni.showToast({ title: (res && res.msg) || '抢单失败', icon: 'none' })
        load()
      }
    })
  }

  function cancel() {
    uni.showModal({
      title: '取消接单',
      content: '取消后订单将退回抢单大厅，确定取消吗？',
      success: r => {
        if (!r.confirm) return
        cancelOrder(id.value).then(() => {
          uni.showToast({ title: '已取消', icon: 'none' })
          setTimeout(() => uni.navigateBack(), 600)
        })
      }
    })
  }

  function call() {
    uni.makePhoneCall({ phoneNumber: order.value.customerPhone })
  }

  function openMap() {
    const o = order.value
    const p = o.myStatus === 'shipping' && o.end ? o.end : o.start
    uni.openLocation({ latitude: p.latitude, longitude: p.longitude, name: p.label, address: p.address })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
    padding-bottom: calc(#{r(80)} + env(safe-area-inset-bottom));
  }

  .content {
    padding: r(10);
  }

  .status-card {
    padding: r(18) r(16);
    margin-bottom: r(10);
    border-radius: r(10);
    background: linear-gradient(135deg, #3b7cf6, #2c5ad8);
    color: #fff;
  }

  .status-title {
    display: block;
    font-size: r(20);
    font-weight: bold;
  }

  .status-desc {
    display: block;
    margin-top: r(6);
    font-size: r(13);
    opacity: 0.85;
  }

  .steps {
    display: flex;
    margin-top: r(18);
  }

  .step {
    position: relative;
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .step-dot {
    position: relative;
    z-index: 1;
    width: r(10);
    height: r(10);
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.4);
  }

  .step-bar {
    position: absolute;
    top: r(4);
    left: 50%;
    width: 100%;
    height: r(2);
    background: rgba(255, 255, 255, 0.3);
  }

  .step.done .step-dot {
    background: #fff;
  }

  .step-text {
    margin-top: r(6);
    font-size: r(12);
    opacity: 0.6;
  }

  .step.done .step-text {
    opacity: 1;
  }

  .card {
    margin-bottom: r(10);
    padding: r(14) r(16);
    background: #fff;
    border-radius: r(10);
  }

  .route-row {
    display: flex;
  }

  .route-tag {
    flex-shrink: 0;
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

  .route-body {
    margin-left: r(8);
    display: flex;
    flex-direction: column;
  }

  .route-place {
    font-size: r(15);
    font-weight: bold;
    line-height: r(18);
    color: #262626;
  }

  .route-addr {
    margin-top: r(4);
    font-size: r(12);
    color: #999;
  }

  .route-line {
    margin: r(4) 0 r(4) r(10);
    width: 1px;
    height: r(12);
    background: #c8c8c8;
  }

  .nav-btn {
    margin-top: r(14);
    height: r(34);
    border-radius: r(17);
    background: #eef5ff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(13);
    color: #2b84ff;
  }

  .nav-btn text {
    margin-left: r(4);
  }

  .row {
    display: flex;
    align-items: center;
    min-height: r(34);
    font-size: r(14);
  }

  .label {
    width: r(80);
    color: #999;
  }

  .value {
    flex: 1;
    color: $zd-text;
  }

  .value.price {
    font-weight: bold;
    color: $zd-orange;
  }

  .value.link {
    color: #2b84ff;
  }

  .footer {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 99;
    display: flex;
    padding: r(12) r(16) calc(#{r(12)} + env(safe-area-inset-bottom));
    background: #fff;
    box-shadow: 0 r(-2) r(8) rgba(0, 0, 0, 0.05);
  }

  .footer-btn {
    flex: 1;
    height: r(46);
    line-height: r(46);
    border-radius: r(23);
    text-align: center;
    font-size: r(16);
  }

  .footer-btn + .footer-btn {
    margin-left: r(12);
  }

  .footer-btn.primary {
    background: $zd-btn;
    color: #fff;
  }

  .footer-btn.plain {
    border: 1px solid #d0d4de;
    color: #666;
  }
</style>
