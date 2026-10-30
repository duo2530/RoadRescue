<template>
  <view class="page">
    <view class="banner">
      <image class="banner-img" src="/static/images/driver/hall-banner.jpg" mode="widthFix"></image>
      <view v-if="!authDone" class="notice" @click="goAuth">
        <image class="notice-icon" src="/static/images/driver/icon-warn.png" mode="aspectFit"></image>
        <text class="notice-strong">完善证件</text>
        <text class="notice-text">立刻开启抢单！</text>
        <view class="notice-btn">
          <image class="notice-btn-icon" src="/static/images/driver/icon-id.png" mode="aspectFit"></image>
          <text>去上传</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="tabs">
        <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: scope === t.key }" @click="switchScope(t.key)">
          <text class="tab-text">{{ t.text }}</text>
          <view class="tab-line"></view>
        </view>
      </view>

      <view v-if="scope === 'nearby'" class="filters">
        <view class="filter f-city">
          <view class="dot dot-blue"></view>
          <text>{{ location.city || '定位中' }}</text>
        </view>
        <view class="filter f-radius" @click="openPicker('radius')">
          <text>附近：{{ radius }}KM</text>
          <view class="tri"></view>
        </view>
      </view>
      <view v-else class="filters">
        <view class="filter f-start" @click="openPicker('start')">
          <view class="dot dot-blue"></view>
          <text :class="{ placeholder: !startProvince }">{{ startProvince || '出发地' }}</text>
          <view class="tri"></view>
        </view>
        <view class="filter f-end" @click="openPicker('end')">
          <view class="dot dot-orange"></view>
          <text :class="{ placeholder: !endProvince }">{{ endProvince || '目的地' }}</text>
          <view class="tri"></view>
        </view>
        <view class="filter f-time" @click="openPicker('time')">
          <text>{{ publishLabel }}</text>
          <view class="tri"></view>
        </view>
      </view>
    </view>

    <view class="list">
      <view v-for="o in list" :key="o.id" class="card" @click="goDetail(o)">
        <view class="route">
          <view class="route-row">
            <text class="route-tag tag-start">起</text>
            <text class="route-place">{{ o.start.label }}</text>
            <text class="route-dist">距您约: <text class="blue">{{ o.distance }}公里</text></text>
          </view>
          <template v-if="scope === 'nearby' && o.end">
            <view class="route-line"></view>
            <view class="route-row">
              <text class="route-tag tag-end">终</text>
              <text class="route-place">{{ o.end.label }}</text>
            </view>
          </template>
        </view>
        <view class="divider"></view>
        <view class="info">
          <text class="plate">{{ o.plateColor }}</text>
          <text class="info-text">拖车:{{ o.towType }} 服务类型:{{ serviceName(o.serviceType) }}</text>
          <text class="price">¥ {{ money(o.price) }}</text>
        </view>
        <view class="extra">
          <view v-if="o.instant" class="extra-head">
            <image class="rocket" src="/static/images/driver/icon-rocket.png" mode="aspectFit"></image>
            <text class="instant">即时订单</text>
          </view>
          <view class="grab" :class="{ disabled: o.poolStatus !== 'open' }" @click.stop="grab(o)">
            {{ o.poolStatus === 'open' ? '抢单' : '已安排' }}
          </view>
        </view>
      </view>
      <view v-if="loaded && !list.length" class="empty">
        <image class="empty-img" src="/static/images/driver/empty.png" mode="aspectFit"></image>
        <text class="empty-text">{{ scope === 'nearby' ? '附近暂无订单，试试扩大范围' : '暂无订单' }}</text>
      </view>
    </view>

    <view class="fab" :class="{ spinning }" @click="reload(true)">
      <image class="fab-icon" src="/static/images/driver/icon-refresh.png" mode="aspectFit"></image>
    </view>

    <zd-tab-bar current="hall"></zd-tab-bar>

    <!-- 底部选择弹层（H5 下 uni.showActionSheet 样式差异大，统一自定义） -->
    <view v-if="picker" class="sheet-mask" @click="picker = null">
      <view class="sheet" @click.stop>
        <view class="sheet-title">{{ picker.title }}</view>
        <scroll-view scroll-y class="sheet-body">
          <view v-for="opt in picker.options" :key="opt.label" class="sheet-item" :class="{ selected: opt.value === picker.value }" @click="pick(opt)">
            {{ opt.label }}
          </view>
        </scroll-view>
        <view class="sheet-gap"></view>
        <view class="sheet-item" @click="picker = null">取消</view>
      </view>
    </view>

    <!-- 未解锁业务提示 -->
    <view v-if="authTip" class="modal-mask" @click="authTip = ''">
      <view class="modal" @click.stop>
        <view class="modal-title">暂未解锁</view>
        <view class="modal-content">{{ authTip }}</view>
        <view class="modal-btns">
          <view class="modal-btn" @click="authTip = ''">再看看</view>
          <view class="modal-btn primary" @click="authTip = ''; goAuth()">去认证</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed } from 'vue'
  import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
  import { listHallOrders, grabOrder } from '@/api/order'
  import { useUserStore } from '@/store'
  import { getLocation } from '@/utils/location'
  import { serviceName, money } from '@/utils/format'
  import { PROVINCES } from '@/mock/regions'
  import { CODE_AUTH_REQUIRED } from '@/mock'

  const userStore = useUserStore()

  const tabs = [
    { key: 'nearby', text: '附近订单' },
    { key: 'all', text: '全国订单' }
  ]
  const RADIUS_OPTIONS = [10, 30, 50, 100]
  const TIME_OPTIONS = [
    { label: '不限', value: '' },
    { label: '1小时内', value: 1 },
    { label: '24小时内', value: 24 },
    { label: '3天内', value: 72 }
  ]

  const scope = ref('nearby')
  const radius = ref(10)
  const startProvince = ref('')
  const endProvince = ref('')
  const publishWithin = ref('')
  const location = ref({})
  const list = ref([])
  const loaded = ref(false)
  const spinning = ref(false)
  const picker = ref(null)
  const authTip = ref('')

  const authDone = computed(() => {
    const auth = (userStore.info && userStore.info.auth) || []
    return auth.length > 0 && auth.every(s => s.done)
  })
  const publishLabel = computed(() => {
    const opt = TIME_OPTIONS.find(o => o.value === publishWithin.value)
    return publishWithin.value ? opt.label : '发布时间'
  })

  onShow(() => {
    userStore.ensureLogin().then(() => {
      userStore.fetchInfo()
      return getLocation()
    }).then(loc => {
      location.value = loc
      reload()
    }).catch(() => {})
  })

  onPullDownRefresh(() => {
    reload().finally(() => uni.stopPullDownRefresh())
  })

  function reload(manual) {
    if (manual) {
      spinning.value = true
      setTimeout(() => (spinning.value = false), 600)
    }
    const params = { scope: scope.value, latitude: location.value.latitude, longitude: location.value.longitude }
    if (scope.value === 'nearby') {
      params.radius = radius.value
    } else {
      params.startProvince = startProvince.value
      params.endProvince = endProvince.value
      params.publishWithin = publishWithin.value
    }
    return listHallOrders(params).then(data => {
      list.value = data
      loaded.value = true
      if (manual) uni.showToast({ title: '已刷新', icon: 'none' })
    })
  }

  function switchScope(key) {
    if (scope.value === key) return
    scope.value = key
    list.value = []
    loaded.value = false
    reload()
  }

  function openPicker(type) {
    const provinceOptions = [{ label: '不限', value: '' }, ...PROVINCES.map(p => ({ label: p, value: p }))]
    picker.value = {
      radius: { title: '附近范围', options: RADIUS_OPTIONS.map(v => ({ label: v + 'KM', value: v })), value: radius.value },
      start: { title: '出发地', options: provinceOptions, value: startProvince.value },
      end: { title: '目的地', options: provinceOptions, value: endProvince.value },
      time: { title: '发布时间', options: TIME_OPTIONS, value: publishWithin.value }
    }[type]
    picker.value.type = type
  }

  function pick(opt) {
    const target = { radius, start: startProvince, end: endProvince, time: publishWithin }[picker.value.type]
    target.value = opt.value
    picker.value = null
    reload()
  }

  function grab(o) {
    if (o.poolStatus !== 'open') return
    uni.showLoading({ title: '抢单中' })
    grabOrder(o.id).then(() => {
      uni.hideLoading()
      uni.showToast({ title: '抢单成功', icon: 'success' })
      reload()
      setTimeout(() => uni.navigateTo({ url: '/pages/order/detail?id=' + o.id }), 800)
    }).catch(res => {
      uni.hideLoading()
      if (res && res.code === CODE_AUTH_REQUIRED) {
        authTip.value = res.msg
      } else {
        uni.showToast({ title: (res && res.msg) || '抢单失败', icon: 'none' })
        reload()
      }
    })
  }

  function goDetail(o) {
    uni.navigateTo({ url: '/pages/order/detail?id=' + o.id })
  }

  function goAuth() {
    uni.navigateTo({ url: '/pages/auth/index' })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #f5f5f5;
  }

  .banner {
    position: relative;
    height: r(240);
    overflow: hidden;
  }

  .banner-img {
    display: block;
    width: 100%;
  }

  .notice {
    position: absolute;
    left: r(10);
    right: r(10);
    top: r(185);
    height: r(51.3);
    padding: 0 r(10) 0 r(13);
    box-sizing: border-box;
    display: flex;
    align-items: center;
    background: linear-gradient(180deg, #faf4e5, #f6efe0);
    border-radius: r(10);
  }

  .notice-icon {
    width: r(16);
    height: r(16);
    margin-right: r(7);
  }

  .notice-strong {
    font-size: r(17);
    font-weight: bold;
    color: #f66a19;
  }

  .notice-text {
    margin-left: r(7);
    font-size: r(16);
    font-style: italic;
    color: #604445;
  }

  .notice-btn {
    margin-left: auto;
    width: r(80);
    height: r(29);
    border-radius: r(15);
    background: #f57c21;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(14);
    color: #fff;
  }

  .notice-btn-icon {
    width: r(15);
    height: r(12.5);
    margin-right: r(3);
  }

  .panel {
    position: relative;
    margin-top: r(6);
    padding: 0 r(33);
    background: #fff;
  }

  .tabs {
    display: flex;
    height: r(43);
    padding-top: r(8.5);
    box-sizing: border-box;
  }

  .tab {
    margin-right: r(29);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .tab-text {
    font-size: r(16);
    line-height: r(20);
    color: #adadad;
  }

  .tab-line {
    margin-top: r(11.5);
    width: r(15.3);
    height: r(3);
    border-radius: r(2);
  }

  .tab.active .tab-text {
    font-size: r(18);
    font-weight: bold;
    color: #222;
  }

  .tab.active .tab-line {
    background: #222;
  }

  /* 筛选项按截图实测的横向位置绝对定位 */
  .filters {
    position: relative;
    height: r(51);
    font-size: r(13);
    color: #333;
  }

  .filter {
    position: absolute;
    top: 0;
    bottom: 0;
    display: flex;
    align-items: center;
  }

  .f-city {
    left: r(43);
  }

  .f-radius {
    left: r(213);
  }

  .f-start {
    left: r(7);
  }

  .f-end {
    left: r(131);
  }

  .f-time {
    left: r(256);
  }

  .placeholder {
    color: #999;
  }

  .dot {
    width: r(6);
    height: r(6);
    border-radius: 50%;
    margin-right: r(4);
  }

  .dot-blue {
    background: #2b84ff;
  }

  .dot-orange {
    background: #ff8a3d;
  }

  .tri {
    margin-left: r(5);
    width: 0;
    height: 0;
    border-left: r(5) solid transparent;
    border-right: r(5) solid transparent;
    border-top: r(6.7) solid #ebebeb;
  }

  .list {
    padding: r(11) r(10) r(20);
  }

  /* 实测：卡片 351.3-533.3pt，内框 460.3-523.3pt（x 30-364） */
  .card {
    margin-bottom: r(10);
    padding: r(12) r(16) r(7.5);
    background: #fff;
    border-radius: r(10);
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

  .route-dist {
    margin-left: auto;
    font-size: r(14);
    color: #292929;
  }

  .blue {
    color: #2b84ff;
  }

  .route-line {
    margin: r(1) 0 r(1) r(10);
    width: 1px;
    height: r(5);
    background: #c8c8c8;
  }

  .divider {
    margin: r(9) r(-6) 0;
    height: 1px;
    background: linear-gradient(90deg, #fbfbfb, #f4f4f4 30%, #f4f4f4 75%, #fafafb);
  }

  .info {
    margin-top: r(15);
    display: flex;
    align-items: center;
  }

  .plate {
    padding: 0 r(4);
    height: r(14);
    line-height: r(14);
    border-radius: r(2);
    background: #006ef6;
    font-size: r(10);
    color: #fff;
  }

  .info-text {
    margin-left: r(8);
    font-size: r(13);
    color: #4c4c4c;
  }

  .price {
    margin-left: auto;
    font-size: r(18);
    font-weight: 500;
    color: #ff5d2e;
  }

  .extra {
    position: relative;
    margin: r(10) 0 0 r(4);
    height: r(63);
    padding: r(10.5) r(6.3);
    box-sizing: border-box;
    background: #fafafa;
    border-radius: r(4);
  }

  .extra-head {
    display: flex;
    align-items: center;
    height: r(14);
  }

  .rocket {
    width: r(17.3);
    height: r(17.3);
    margin: 0 r(1) 0 r(-2.5);
  }

  .instant {
    font-size: r(14);
    font-weight: 500;
    color: #fe4b0f;
  }

  .grab {
    position: absolute;
    right: r(5.3);
    bottom: r(5.3);
    width: r(83);
    height: r(30);
    line-height: r(30);
    border-radius: r(15);
    text-align: center;
    background: #2b84ff;
    font-size: r(15);
    color: #fff;
  }

  .grab.disabled {
    background: #c2c2c2;
  }

  .empty {
    padding-top: r(60);
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .empty-img {
    width: r(290);
    height: r(92.7);
  }

  .empty-text {
    margin-top: r(4);
    font-size: r(12);
    color: #b6b7b8;
  }

  .fab {
    position: fixed;
    left: r(12.5);
    bottom: calc(#{r(65.5)} + env(safe-area-inset-bottom));
    z-index: 98;
    width: r(50);
    height: r(50);
    border-radius: 50%;
    background: #fff;
    box-shadow: 0 r(2) r(8) rgba(0, 0, 0, 0.12);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .fab-icon {
    width: r(28);
    height: r(27);
  }

  .fab.spinning .fab-icon {
    animation: spin 0.6s linear;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  .sheet-mask,
  .modal-mask {
    position: fixed;
    inset: 0;
    z-index: 200;
    background: rgba(0, 0, 0, 0.45);
  }

  .sheet {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background: #fff;
    border-radius: r(12) r(12) 0 0;
    padding-bottom: env(safe-area-inset-bottom);
  }

  .sheet-title {
    height: r(48);
    line-height: r(48);
    text-align: center;
    font-size: r(14);
    color: #999;
    border-bottom: 1px solid #f2f2f2;
  }

  .sheet-body {
    max-height: r(330);
  }

  .sheet-item {
    height: r(50);
    line-height: r(50);
    text-align: center;
    font-size: r(16);
    color: #333;
    border-bottom: 1px solid #f5f5f5;
  }

  .sheet-item.selected {
    color: #2b84ff;
    font-weight: bold;
  }

  .sheet-gap {
    height: r(8);
    background: #f4f5f7;
  }

  .modal-mask {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .modal {
    width: r(290);
    background: #fff;
    border-radius: r(12);
    overflow: hidden;
  }

  .modal-title {
    padding-top: r(22);
    text-align: center;
    font-size: r(17);
    font-weight: bold;
    color: #222;
  }

  .modal-content {
    padding: r(12) r(24) r(22);
    text-align: center;
    font-size: r(14);
    line-height: r(22);
    color: #666;
  }

  .modal-btns {
    display: flex;
    border-top: 1px solid #f0f0f0;
  }

  .modal-btn {
    flex: 1;
    height: r(48);
    line-height: r(48);
    text-align: center;
    font-size: r(16);
    color: #666;
  }

  .modal-btn.primary {
    color: #2b84ff;
    font-weight: bold;
    border-left: 1px solid #f0f0f0;
  }
</style>
