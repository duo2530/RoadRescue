<template>
  <view class="page">
    <!-- 地图：占满面板以上的剩余高度 -->
    <view class="map-wrap">
      <map
        id="homeMap"
        class="map"
        :latitude="center.latitude"
        :longitude="center.longitude"
        :scale="scale"
        :markers="markers"
        :polyline="polyline"
        :include-points="includePoints"
        :show-location="false"
        @regionchange="onRegionChange"
      ></map>

      <!-- 非拖车 / 拖车未选终点：中心定位针 + 气泡 -->
      <view v-if="!routeMode" class="center-overlay">
        <view class="bubble" @click="chooseStart">
          <view class="bubble-head">
            <image class="bubble-signal" src="/static/images/rescue/signal.png" mode="aspectFit"></image>
            <text>附近有10辆车，最快15分钟内到达</text>
          </view>
          <view class="bubble-body">
            <view class="bubble-text">
              <text class="bubble-title">{{ startAddress ? startAddress.name : '正在获取位置…' }}</text>
              <text class="bubble-sub">{{ startAddress ? startAddress.name : '' }}</text>
            </view>
            <view class="bubble-arrow">
              <uni-icons type="right" size="12" color="#3885f8"></uni-icons>
            </view>
          </view>
        </view>
        <view class="pin-line"></view>
        <view class="pin-dot"></view>
      </view>

      <!-- 拖车已选终点：终点卡片（左上）+ 起点卡片 -->
      <template v-if="routeMode">
        <view class="addr-card end-card" @click="chooseEnd">
          <view class="addr-card-main">
            <view class="addr-card-dist">
              <text class="dist-num">{{ distance === null ? '--' : distance }}公里</text>
              <text class="dist-label">距离</text>
            </view>
            <text class="addr-card-name">{{ endAddress.address || endAddress.name }}</text>
            <text class="addr-card-edit">点击修改</text>
          </view>
          <view class="addr-card-arrow">
            <uni-icons type="right" size="10" color="#fff"></uni-icons>
          </view>
        </view>
        <view class="addr-card start-card" @click="chooseStart">
          <view class="addr-card-main">
            <text class="addr-card-name">{{ startAddress.address || startAddress.name }}</text>
            <text class="addr-card-edit">点击修改</text>
          </view>
          <view class="addr-card-arrow">
            <uni-icons type="right" size="10" color="#fff"></uni-icons>
          </view>
        </view>
      </template>

      <!-- 右上「我的」 -->
      <view class="mine-entry" :style="{ top: Math.max(statusBarHeight + 53, 100) + 'px' }" @click="goMine">
        <image class="mine-entry-icon" src="/static/images/rescue/home-mine.png" mode="aspectFit"></image>
        <text class="mine-entry-text">我的</text>
      </view>

      <!-- 左下电话救援 -->
      <view class="phone-btn" @click="callService">
        <image class="phone-24h" src="/static/images/rescue/icon-24h.png" mode="aspectFit"></image>
        <text class="phone-text">电话救援</text>
        <view class="phone-arrow">
          <uni-icons type="right" size="9" color="#fff"></uni-icons>
        </view>
      </view>

      <!-- 右下定位 -->
      <view class="locate-btn" @click="relocate">
        <image class="locate-icon" src="/static/images/rescue/locate.png" mode="aspectFit"></image>
      </view>
    </view>

    <!-- 下单面板 -->
    <view class="panel">
      <view class="panel-head">
        <image class="logo" src="/static/images/rescue/logo.png" mode="aspectFit"></image>
        <view v-if="current.type === 'tow'" class="guarantee" @click="openText('平台保障', guaranteeText)">
          <text>平台保障明码标价，5大权益保障服务</text>
          <uni-icons type="right" size="11" color="#133f79"></uni-icons>
        </view>
      </view>

      <view class="card">
        <view class="tabs">
          <view
            v-for="s in services"
            :key="s.type"
            class="tab"
            :class="['tab-' + s.type, { active: s.type === current.type }]"
            @click="switchService(s)"
          >
            <text>{{ s.name }}</text>
          </view>
        </view>

        <view class="tip">
          <text>{{ current.tip }}</text>
        </view>
      </view>

      <view class="form">
        <view class="row" @click="chooseStart">
          <text class="label">救援地点<text class="star">*</text></text>
          <text class="value" :class="{ placeholder: !startAddress }">{{ startAddress ? startAddress.name : '请选择救援地点' }}</text>
          <uni-icons class="row-arrow" type="right" size="15" color="#252237"></uni-icons>
        </view>
        <view v-if="current.needDestination" class="row" @click="chooseEnd">
          <text class="label">拖车终点<text class="star">*</text></text>
          <text class="value" :class="{ placeholder: !endAddress }">{{ endAddress ? endAddress.name : '您的拖车目的地' }}</text>
          <uni-icons class="row-arrow" type="right" size="15" color="#252237"></uni-icons>
        </view>
        <picker :range="carTypes" :value="carTypes.indexOf(form.carType)" @change="e => form.carType = carTypes[e.detail.value]">
          <view class="row no-arrow">
            <text class="label">车型<text class="star">*</text></text>
            <text class="value strong">{{ form.carType }}</text>
          </view>
        </picker>
        <picker :range="positions" :value="positions.indexOf(form.position)" @change="e => form.position = positions[e.detail.value]">
          <view class="row no-arrow">
            <text class="label">当前位于<text class="star">*</text></text>
            <text class="value strong">{{ form.position }}</text>
          </view>
        </picker>
        <view class="row" @click="choosePlate">
          <text class="label">车牌号<text class="star">*</text></text>
          <text class="value" :class="{ placeholder: !form.plate }">{{ form.plate || '请选择车牌号' }}</text>
          <uni-icons class="row-arrow" type="right" size="15" color="#252237"></uni-icons>
        </view>
        <view class="row no-arrow no-border">
          <text class="label">联系电话<text class="star">*</text></text>
          <input class="value phone-input" type="number" maxlength="11" v-model="form.phone" placeholder="请输入联系电话" placeholder-class="placeholder" />
        </view>
      </view>
    </view>

    <!-- 底部下单栏 -->
    <view class="bar-placeholder"></view>
    <view class="bottom">
      <view class="bar">
        <view class="bar-left">
          <view class="fee">
            <text class="fee-label">费用预估</text>
            <text v-if="price.payAmount !== null" class="fee-price"><text class="fee-yen">¥</text>{{ price.payAmount }}</text>
          </view>
          <view class="fee-sub">
            <view class="fee-explain" @click="showFeeExplain">
              <uni-icons type="info" size="11" color="#686868"></uni-icons>
              <text>查看费用说明</text>
            </view>
            <text v-if="price.discount" class="fee-discount">券已减¥{{ price.discount }}</text>
          </view>
        </view>
        <view class="bar-right">
          <view class="submit" :class="{ disabled: submitting }" @click="submit">
            <text>同意协议并救援</text>
          </view>
          <view class="users">
            <text class="fire">🔥</text>
            <text>已有12+万人使用过</text>
          </view>
        </view>
      </view>
      <view class="agreement">
        <text>请您阅读并同意</text>
        <text class="link" @click="openText('中道救援服务协议', agreementText)">中道救援服务协议</text>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, reactive, computed, watch } from 'vue'
  import { onLoad, onShow, onUnload } from '@dcloudio/uni-app'
  import config from '@/config'
  import { useUserStore } from '@/store'
  import { listService } from '@/api/service'
  import { estimate, createOrder } from '@/api/order'
  import { reverseGeocode, drivingRoute, straightDistance } from '@/utils/qqmap'
  import { AGREEMENT_TEXT, GUARANTEE_TEXT } from '@/utils/texts'

  // 默认定位：芈展服务园（与截图一致），浏览器定位成功后替换
  const DEFAULT_LOCATION = { latitude: 31.238096, longitude: 121.653299 }

  const agreementText = AGREEMENT_TEXT
  const guaranteeText = GUARANTEE_TEXT
  const carTypes = ['9座以下（含9座）客车', '10-19座客车', '2吨以下货车', '2-5吨货车', '新能源车']
  const positions = ['地面', '地库']

  const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0

  const services = ref([])
  const current = ref({ type: 'tow', name: '拖车', tip: '', needDestination: true })
  const center = reactive({ ...DEFAULT_LOCATION })
  const scale = ref(15)
  const startAddress = ref(null)
  const endAddress = ref(null)
  const distance = ref(null)
  const routePoints = ref([])
  const submitting = ref(false)
  const price = reactive({ amount: null, discount: 0, payAmount: null })
  const form = reactive({ carType: carTypes[0], position: positions[0], plate: '', phone: '' })

  const userStore = useUserStore()

  const routeMode = computed(() => !!(current.value.needDestination && endAddress.value && startAddress.value))

  const markers = computed(() => {
    if (!routeMode.value) return []
    const s = startAddress.value
    const e = endAddress.value
    // 起点附近散布几辆拖车，演示「附近车辆」；偏移量随起终点距离缩放，避免遮住起点
    const span = Math.max(Math.abs(e.latitude - s.latitude), Math.abs(e.longitude - s.longitude), 0.01)
    const list = [[0.12, -0.18], [-0.15, 0.1], [0.2, 0.16]].map(([dLat, dLng], i) => ({
      id: 10 + i,
      latitude: s.latitude + dLat * span,
      longitude: s.longitude + dLng * span,
      iconPath: '/static/images/rescue/truck.png',
      width: 34,
      height: 20
    }))
    // 起终点放在最后，层级在拖车之上
    list.push(
      { id: 1, latitude: s.latitude, longitude: s.longitude, iconPath: '/static/images/rescue/marker-start.png', width: 26, height: 32, anchor: { x: 0.5, y: 1 } },
      { id: 2, latitude: e.latitude, longitude: e.longitude, iconPath: '/static/images/rescue/marker-end.png', width: 26, height: 32, anchor: { x: 0.5, y: 1 } }
    )
    return list
  })

  const polyline = computed(() => routeMode.value && routePoints.value.length
    ? [{ points: routePoints.value, color: '#07bc19', width: 6, arrowLine: true }]
    : [])

  const includePoints = computed(() => routeMode.value
    ? [startAddress.value, endAddress.value].map(p => ({ latitude: p.latitude, longitude: p.longitude }))
    : [])

  function onVehicleSelected(plate) {
    form.plate = plate
  }

  onLoad(() => {
    uni.$on('vehicle:selected', onVehicleSelected)
    listService().then(list => {
      services.value = list
      current.value = list[0]
    })
    userStore.ensureLogin().then(user => {
      if (!form.phone) form.phone = user.phone
    })
    locate()
  })

  onUnload(() => {
    uni.$off('vehicle:selected', onVehicleSelected)
  })

  // 兑换优惠券后回到首页时刷新费用
  onShow(refreshPrice)

  watch([() => current.value.type, distance], refreshPrice)

  function refreshPrice() {
    estimate({ serviceType: current.value.type, distance: distance.value }).then(res => {
      Object.assign(price, res)
    })
  }

  function switchService(s) {
    current.value = s
  }

  // ---------- 定位与选点 ----------
  function locate() {
    uni.getLocation({
      type: 'gcj02',
      success: res => setStart(res.latitude, res.longitude, true),
      // 浏览器拒绝定位或非 https 时使用默认位置
      fail: () => setStart(DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude, true)
    })
  }

  function relocate() {
    endAddress.value = null
    distance.value = null
    routePoints.value = []
    locate()
  }

  function setStart(latitude, longitude, moveMap) {
    if (moveMap) {
      center.latitude = latitude
      center.longitude = longitude
    }
    return reverseGeocode(latitude, longitude).then(addr => {
      startAddress.value = addr
    }).catch(() => {
      startAddress.value = { name: '当前位置', address: '', latitude, longitude }
    })
  }

  // 拖动地图结束后，以地图中心为救援地点
  let regionTimer = null
  function onRegionChange(e) {
    if (routeMode.value) return
    const detail = e.detail || {}
    const type = e.type === 'end' || detail.type === 'end'
    const causedBy = detail.causedBy || e.causedBy
    const loc = detail.centerLocation || e.centerLocation
    if (!type || causedBy === 'scale' || !loc) return
    clearTimeout(regionTimer)
    regionTimer = setTimeout(() => setStart(loc.latitude, loc.longitude, false), 300)
  }

  function pickLocation(init) {
    return new Promise((resolve, reject) => {
      uni.chooseLocation({
        latitude: init && init.latitude,
        longitude: init && init.longitude,
        success: res => {
          if (!res.latitude) return reject()
          resolve({ name: res.name || res.address, address: res.address || res.name, latitude: res.latitude, longitude: res.longitude })
        },
        fail: reject
      })
    })
  }

  function chooseStart() {
    pickLocation(startAddress.value).then(addr => {
      startAddress.value = addr
      center.latitude = addr.latitude
      center.longitude = addr.longitude
      if (endAddress.value) planRoute()
    }).catch(() => {})
  }

  function chooseEnd() {
    pickLocation(endAddress.value || startAddress.value).then(addr => {
      endAddress.value = addr
      planRoute()
    }).catch(() => {})
  }

  function planRoute() {
    if (!startAddress.value || !endAddress.value) return
    drivingRoute(startAddress.value, endAddress.value).then(res => {
      distance.value = res.distance
      routePoints.value = res.points
    }).catch(() => {
      // 路线规划失败时按直线距离 × 1.3 估算，并画直线
      distance.value = Math.round(straightDistance(startAddress.value, endAddress.value) * 130) / 100
      routePoints.value = [startAddress.value, endAddress.value].map(p => ({ latitude: p.latitude, longitude: p.longitude }))
    })
  }

  watch(() => current.value.needDestination, need => {
    if (!need) {
      distance.value = null
    } else if (endAddress.value) {
      planRoute()
    }
  })

  // ---------- 其它入口 ----------
  function choosePlate() {
    uni.navigateTo({ url: '/pages/vehicle/index?selected=' + encodeURIComponent(form.plate) })
  }

  function goMine() {
    uni.navigateTo({ url: '/pages/order/index' })
  }

  function callService() {
    uni.makePhoneCall({ phoneNumber: config.appInfo.servicePhone })
  }

  function openText(title, content) {
    uni.navigateTo({ url: `/pages/common/textview?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}` })
  }

  function showFeeExplain() {
    const s = current.value
    let content
    if (s.rule) {
      const { basePrice, baseKm, perKm } = s.rule
      content = `起步价¥${basePrice}（含${baseKm}公里），超出部分¥${perKm}/公里，按导航里程计算。高速费、路桥费等第三方费用另计。`
    } else {
      content = `${s.name}服务一口价¥${s.price}，含上门服务费，配件及材料费用另计。`
    }
    uni.showModal({ title: '费用说明', content, showCancel: false, confirmText: '我知道了' })
  }

  // ---------- 下单 ----------
  function submit() {
    if (submitting.value) return
    if (current.value.needDestination && !endAddress.value) {
      uni.showToast({ title: '请选择拖车终点', icon: 'none' })
      return
    }
    submitting.value = true
    createOrder({
      serviceType: current.value.type,
      startAddress: startAddress.value,
      endAddress: endAddress.value,
      distance: distance.value,
      carType: form.carType,
      position: form.position,
      plate: form.plate,
      phone: form.phone
    }).then(order => {
      uni.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => {
        uni.navigateTo({ url: '/pages/order/detail?id=' + order.id })
      }, 600)
    }).catch(() => {}).finally(() => {
      submitting.value = false
    })
  }
</script>

<style lang="scss" scoped>
  /* 尺寸按 390pt 宽截图测量，r() 换算为 rpx */
  .page {
    height: 100vh;
    display: flex;
    flex-direction: column;
    background: $zd-bg;
  }

  /* ---------- 地图 ---------- */
  .map-wrap {
    position: relative;
    flex: 1;
    min-height: 220px;
  }

  .map {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
  }

  .center-overlay {
    position: absolute;
    left: 50%;
    top: calc(50% + #{r(30)});
    transform: translate(-50%, calc(-100% + #{r(4)}));
    display: flex;
    flex-direction: column;
    align-items: center;
    z-index: 2;
  }

  .bubble {
    width: r(177);
    border-radius: r(10);
    overflow: hidden;
    background: #fff;
    box-shadow: 0 r(2) r(8) rgba(0, 0, 0, 0.15);
  }

  .bubble-head {
    display: flex;
    align-items: center;
    height: r(25);
    padding: 0 r(6);
    background: linear-gradient(90deg, #3384f8, #55aefb);
    color: #fff;
    font-size: r(9.6);
    white-space: nowrap;
  }

  .bubble-signal {
    width: r(10);
    height: r(8);
    margin-right: r(4);
    flex-shrink: 0;
  }

  .bubble-body {
    display: flex;
    align-items: center;
    padding: r(9) r(7) r(9);
  }

  .bubble-text {
    display: flex;
    flex-direction: column;
    min-width: 0;
    max-width: r(140);
  }

  .bubble-title {
    font-size: r(13.5);
    line-height: r(17);
    font-weight: 600;
    color: #000;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .bubble-sub {
    margin-top: r(2);
    font-size: r(9.5);
    line-height: r(12);
    color: #005dc7;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .bubble-arrow {
    width: r(16);
    height: r(16);
    margin-left: r(6);
    border-radius: 50%;
    background: #e3eefe;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .pin-line {
    width: r(2);
    height: r(10);
    background: #2e7ff8;
  }

  .pin-dot {
    width: r(8);
    height: r(8);
    margin-top: r(-1);
    border-radius: 50%;
    background: #1a7cf5;
    box-shadow: 0 0 0 r(3) rgba(46, 127, 248, 0.2);
  }

  .addr-card {
    position: absolute;
    z-index: 2;
    display: flex;
    align-items: center;
    padding: r(6) r(8) r(6) r(10);
    box-sizing: border-box;
    background: #fff;
    border-radius: r(8);
    box-shadow: 0 r(2) r(8) rgba(0, 0, 0, 0.12);
  }

  .end-card {
    left: r(32);
    top: r(28);
    width: r(222);
  }

  .start-card {
    left: r(135);
    bottom: r(70);
    width: r(195);
  }

  .addr-card-main {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
  }

  .addr-card-dist {
    display: flex;
    align-items: baseline;
    margin-bottom: r(2);
  }

  .dist-num {
    font-size: r(12);
    font-weight: 600;
    color: #07bc19;
  }

  .dist-label {
    margin-left: r(4);
    font-size: r(10);
    color: #000;
  }

  .addr-card-name {
    font-size: r(12);
    line-height: r(16);
    color: #000;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .addr-card-edit {
    margin-top: r(1);
    font-size: r(10);
    color: #07bc19;
  }

  .addr-card-arrow {
    width: r(16);
    height: r(16);
    margin-left: r(6);
    border-radius: 50%;
    background: #07bc19;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .mine-entry {
    position: absolute;
    right: r(7);
    z-index: 2;
    width: r(39);
    height: r(39);
    background: #fff;
    border-radius: r(6);
    box-shadow: 0 r(1) r(4) rgba(0, 0, 0, 0.12);
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  .mine-entry-icon {
    width: r(17);
    height: r(17);
  }

  .mine-entry-text {
    margin-top: r(2);
    font-size: r(8);
    line-height: 1;
    color: #000;
  }

  .phone-btn {
    position: absolute;
    left: r(7);
    bottom: r(10);
    z-index: 2;
    width: r(142);
    height: r(44);
    padding: 0 r(4);
    box-sizing: border-box;
    border-radius: r(7);
    background: #1c65ff;
    display: flex;
    align-items: center;
  }

  .phone-24h {
    width: r(36);
    height: r(36);
    border-radius: r(8);
    flex-shrink: 0;
  }

  .phone-text {
    margin-left: r(20);
    font-size: r(12);
    color: #fff;
  }

  .phone-arrow {
    width: r(12);
    height: r(12);
    margin-left: auto;
    margin-right: r(6);
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.35);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .locate-btn {
    position: absolute;
    right: 0;
    bottom: r(10);
    z-index: 2;
    width: r(43);
    height: r(36);
    background: #fafafa;
    border-radius: r(8) 0 0 r(8);
    box-shadow: 0 r(1) r(4) rgba(0, 0, 0, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .locate-icon {
    width: r(34);
    height: r(23);
  }

  /* ---------- 面板 ---------- */
  .panel {
    position: relative;
    z-index: 3;
    margin-top: r(-6);
    padding-bottom: r(11);
    border-radius: r(10) r(10) 0 0;
    background:
      linear-gradient(90deg, rgba(228, 245, 255, 0.9), rgba(212, 235, 255, 0) 50%, rgba(189, 217, 255, 0.6)),
      linear-gradient(180deg, #b5d2ff 0, #d4ebff r(40), #ebf9ff r(70), $zd-bg r(100));
  }

  .panel-head {
    height: r(39);
    padding: 0 r(11) 0 r(17);
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .logo {
    width: r(88);
    height: r(22);
  }

  .guarantee {
    display: flex;
    align-items: center;
    font-size: r(10.7);
    color: #133f79;
  }

  .card {
    margin: 0 r(6);
    background: #fff;
    border-radius: r(8) r(8) 0 0;
  }

  .tabs {
    display: flex;
    padding: r(10) r(10) r(10) r(10);
  }

  .tab {
    position: relative;
    height: r(32);
    margin-right: r(10);
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f8f8f8;
    border-radius: r(6);
    font-size: r(13.5);
    color: #000;
    flex-shrink: 0;

    &.tab-tow {
      width: r(58);
    }

    &.tab-battery,
    &.tab-tire {
      width: r(60);
    }

    &:last-child {
      flex: 1;
      margin-right: 0;
    }

    &.active {
      color: #fff;
      background: $zd-blue;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        top: 100%;
        transform: translateX(-50%);
        border-left: r(5) solid transparent;
        border-right: r(5) solid transparent;
        border-top: r(5) solid $zd-blue;
      }
    }
  }

  .tip {
    padding: r(6) r(14) r(11) r(18);
    background: #fff4f4;
    font-size: r(11.3);
    line-height: r(16);
    color: #ff9876;
  }

  .form {
    margin: 0 r(6) 0 r(8);
    background: #fff;
  }

  .row {
    height: r(43);
    padding: 0 r(13) 0 r(10);
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f1f2f5;

    &.no-arrow {
      padding-right: r(11);
    }

    &.no-border {
      border-bottom: none;
    }
  }

  .label {
    width: r(80);
    font-size: r(12.5);
    font-weight: 600;
    color: #000;
    flex-shrink: 0;
  }

  .star {
    color: #fe0f12;
  }

  .value {
    flex: 1;
    min-width: 0;
    text-align: right;
    font-size: r(15);
    color: #363636;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  .strong {
    color: #000;
  }

  .placeholder {
    color: #a6a6a6;
  }

  .row-arrow {
    margin-left: r(5);
  }

  .phone-input {
    height: r(43);
    color: #186ff2;
  }

  /* ---------- 底部栏 ---------- */
  .bar-placeholder {
    flex-shrink: 0;
    height: calc(#{r(126)} + env(safe-area-inset-bottom));
  }

  .bottom {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 10;
    background: #fff;
  }

  .bar {
    height: r(78);
    padding: 0 r(20) 0 r(20);
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .bar-left {
    display: flex;
    flex-direction: column;
  }

  .fee {
    display: flex;
    align-items: baseline;
    height: r(26);
  }

  .fee-label {
    font-size: r(10.5);
    color: #000;
  }

  .fee-price {
    margin-left: r(4);
    font-size: r(26);
    line-height: 1;
    font-weight: 500;
    color: #ff3535;
  }

  .fee-yen {
    margin-right: r(3);
    font-size: r(14);
  }

  .fee-sub {
    margin-top: r(8);
    display: flex;
    align-items: center;
  }

  .fee-explain {
    display: flex;
    align-items: center;
    font-size: r(9.5);
    color: #686868;

    text {
      margin-left: r(2);
    }
  }

  .fee-discount {
    margin-left: r(8);
    font-size: r(9.5);
    color: #ff3535;
  }

  .bar-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }

  .submit {
    width: r(140);
    height: r(40);
    border-radius: r(20);
    background: linear-gradient(90deg, #296bdb, #4fa1ee);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(15);
    color: #fff;

    &.disabled {
      opacity: 0.6;
    }
  }

  .users {
    margin-top: r(3);
    display: flex;
    align-items: center;
    font-size: r(9.5);
    color: #686868;

    .fire {
      margin-right: r(1);
      font-size: r(9);
    }
  }

  .agreement {
    height: r(48);
    padding: 0 r(18);
    padding-bottom: env(safe-area-inset-bottom);
    display: flex;
    align-items: center;
    background: $zd-bg;
    font-size: r(12);
    color: #000;

    .link {
      margin-left: r(4);
      color: #1a92ff;
    }
  }
</style>
