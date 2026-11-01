<template>
  <view class="page">
    <zd-nav-bar title="订单详情"></zd-nav-bar>

    <view v-if="order" class="content">
      <!-- 状态 -->
      <view class="status-card">
        <text class="status-title">{{ statusText[order.status] }}</text>
        <text class="status-desc">{{ statusDesc }}</text>
        <view v-if="order.status !== 'cancelled'" class="steps">
          <view v-for="(s, i) in STATUS_STEPS" :key="s" class="step" :class="{ done: i <= stepIndex }">
            <view class="step-dot"></view>
            <text class="step-text">{{ statusText[s] }}</text>
            <view v-if="i < STATUS_STEPS.length - 1" class="step-line" :class="{ done: i < stepIndex }"></view>
          </view>
        </view>
      </view>

      <!-- 救援师傅 -->
      <view v-if="order.driver" class="card driver">
        <view class="driver-info">
          <text class="driver-name">{{ order.driver.name }}</text>
          <text class="driver-plate">救援车 {{ order.driver.plate }}</text>
        </view>
        <view class="driver-call" @click="call(order.driver.phone)">
          <uni-icons type="phone-filled" size="16" color="#fff"></uni-icons>
          <text>联系师傅</text>
        </view>
      </view>

      <!-- 服务信息 -->
      <view class="card">
        <view class="card-title">{{ serviceName(order.serviceType) }}服务</view>
        <view class="cell">
          <text class="cell-label">救援地点</text>
          <text class="cell-value">{{ order.startAddress.name }}</text>
        </view>
        <view v-if="order.endAddress" class="cell">
          <text class="cell-label">拖车终点</text>
          <text class="cell-value">{{ order.endAddress.name }}</text>
        </view>
        <view v-if="order.distance" class="cell">
          <text class="cell-label">拖车里程</text>
          <text class="cell-value">{{ order.distance }}公里</text>
        </view>
        <view class="cell">
          <text class="cell-label">车型</text>
          <text class="cell-value">{{ order.carType }}</text>
        </view>
        <view class="cell">
          <text class="cell-label">当前位于</text>
          <text class="cell-value">{{ order.position }}</text>
        </view>
        <view class="cell">
          <text class="cell-label">车牌号</text>
          <text class="cell-value">{{ order.plate }}</text>
        </view>
        <view class="cell">
          <text class="cell-label">联系电话</text>
          <text class="cell-value">{{ order.phone }}</text>
        </view>
      </view>

      <!-- 费用 -->
      <view class="card">
        <view class="cell">
          <text class="cell-label">服务费用</text>
          <text class="cell-value">¥{{ order.amount }}</text>
        </view>
        <view v-if="order.discount" class="cell">
          <text class="cell-label">优惠券</text>
          <text class="cell-value red">-¥{{ order.discount }}</text>
        </view>
        <view class="cell">
          <text class="cell-label">{{ order.status === 'finished' ? '实付金额' : '应付金额' }}</text>
          <text class="cell-value price">¥{{ order.payAmount }}</text>
        </view>
      </view>

      <view class="card">
        <view class="cell">
          <text class="cell-label">订单编号</text>
          <text class="cell-value">{{ order.id }}</text>
        </view>
        <view class="cell">
          <text class="cell-label">下单时间</text>
          <text class="cell-value">{{ formatTime(order.createTime) }}</text>
        </view>
      </view>

      <view class="actions">
        <view v-if="canPay" class="btn btn-primary" @click="pay">{{ paying ? '正在打开收银台…' : paymentLabel }}</view>
        <view v-if="canPay" class="btn btn-plain" @click="checkPayment(false)">我已完成支付，查询结果</view>
        <view class="btn btn-plain" @click="call(servicePhone)">联系客服</view>
        <view v-if="canCancel" class="btn btn-plain" @click="cancel">取消订单</view>
      </view>
      <view v-if="canPay" class="payment-note">{{ paymentNote }}</view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed } from 'vue'
  import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
  import config from '@/config'
  import { getOrder, cancelOrder, createPayment, queryPayment } from '@/api/order'
  import { serviceName, statusText, STATUS_STEPS, formatTime } from '@/utils/order'

  const servicePhone = config.appInfo.servicePhone
  const order = ref(null)
  const paying = ref(false)
  let checking = false
  let returningFromPayment = false
  let paymentChannel = 'WECHAT_MINI'
  let paymentLabel = '微信支付（暂未启用）'
  let paymentNote = '小程序微信支付接口已预留，暂未启用'
  // #ifdef H5
  paymentChannel = 'ALIPAY_WAP_SANDBOX'
  paymentLabel = '支付宝沙箱支付'
  paymentNote = '支付宝沙箱环境，使用沙箱买家账号支付，不扣真实资金'
  // #endif
  let id = ''
  let timer = null

  const stepIndex = computed(() => STATUS_STEPS.indexOf(order.value.status === 'settling' ? 'unpaid' : order.value.status))
  const canPay = computed(() => order.value && ['settling', 'unpaid'].includes(order.value.status))
  const canCancel = computed(() => ['waiting', 'dispatched'].includes(order.value.status))
  const statusDesc = computed(() => ({
    waiting: '平台正在为您调度附近的救援车辆，请保持电话畅通',
    dispatched: '救援师傅已出发，预计15分钟内到达',
    rescuing: '救援师傅正在为您服务',
    settling: '救援已结束，支付前将由平台核对最终账单',
    unpaid: '救援已结束，请核对账单并完成支付',
    finished: '本次救援已完成，感谢您的信任',
    cancelled: '订单已取消'
  })[order.value.status])

  function load() {
    return getOrder(id).then(res => {
      order.value = res
    })
  }

  onLoad(options => {
    id = options.id
    returningFromPayment = options.paymentReturn === '1'
  })

  onShow(() => {
    clearInterval(timer)
    load().then(() => {
      const pending = uni.getStorageSync('zd_pending_payment')
      if (canPay.value && (returningFromPayment || (pending && pending.id === id))) return checkPayment(true)
    }).catch(() => {})
    timer = setInterval(() => {
      if (order.value && !['finished', 'cancelled'].includes(order.value.status)) {
        load().then(() => {
          const pending = uni.getStorageSync('zd_pending_payment')
          if (canPay.value && pending && pending.id === id) return checkPayment(true)
        }).catch(() => {})
      }
    }, 5000)
  })

  onHide(() => clearInterval(timer))
  onUnload(() => clearInterval(timer))

  async function pay() {
    if (paying.value || !canPay.value) return
    if (paymentChannel === 'WECHAT_MINI') {
      uni.showToast({ title: '微信支付暂未启用', icon: 'none' })
      return
    }
    paying.value = true
    try {
      const data = { orderNo: order.value.orderNo, channel: paymentChannel }
      // #ifdef H5
      data.returnOrigin = window.location.origin
      // #endif
      const session = await createPayment(data)
      uni.setStorageSync('zd_pending_payment', { id, orderNo: order.value.orderNo })
      // #ifdef H5
      const url = new URL(session.redirectUrl)
      if (url.protocol !== 'https:' || !['openapi-sandbox.dl.alipaydev.com', 'openapi.alipaydev.com'].includes(url.hostname)) {
        throw new Error('收银台地址无效')
      }
      window.location.assign(url.href)
      // #endif
    } catch (error) {
      if (error instanceof Error) uni.showToast({ title: error.message, icon: 'none' })
    } finally { paying.value = false }
  }

  async function checkPayment(silent = false) {
    if (checking || !order.value || !canPay.value) return
    checking = true
    try {
      const paid = await queryPayment(order.value.orderNo)
      await load()
      if (paid) {
        uni.removeStorageSync('zd_pending_payment')
        if (!silent) uni.showToast({ title: '支付成功', icon: 'success' })
      } else if (!silent) {
        uni.showToast({ title: '尚未确认支付，请稍后查询', icon: 'none' })
      }
    } catch (error) {
      if (!silent) uni.showToast({ title: (error && error.msg) || '支付结果查询失败，请重试', icon: 'none' })
    } finally { checking = false }
  }

  function call(phoneNumber) {
    uni.makePhoneCall({ phoneNumber })
  }

  function cancel() {
    uni.showModal({
      title: '提示',
      content: '确定取消本次救援吗？',
      success: res => {
        if (!res.confirm) return
        cancelOrder(id).then(res => {
          order.value = res
          uni.showToast({ title: '已取消', icon: 'none' })
        })
      }
    })
  }
</script>

<style lang="scss" scoped>
  .payment-note { margin-top: r(12); color: #888; font-size: r(12); text-align: center; }
  .btn-primary { color: #fff; background: $zd-blue; }
  .page {
    min-height: 100vh;
    background: #f1f3f7;
  }

  .content {
    padding: r(10) r(12) r(30);
  }

  .status-card {
    padding: r(18) r(16);
    border-radius: r(8);
    background: linear-gradient(135deg, #2e7ff8, #55aefb);
    color: #fff;
  }

  .status-title {
    display: block;
    font-size: r(20);
    font-weight: 600;
  }

  .status-desc {
    display: block;
    margin-top: r(6);
    font-size: r(12);
    opacity: 0.9;
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
    width: r(10);
    height: r(10);
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.4);
  }

  .step.done .step-dot {
    background: #fff;
  }

  .step-text {
    margin-top: r(6);
    font-size: r(11);
    opacity: 0.7;
  }

  .step.done .step-text {
    opacity: 1;
  }

  .step-line {
    position: absolute;
    top: r(4);
    left: calc(50% + #{r(8)});
    right: calc(-50% + #{r(8)});
    height: r(2);
    background: rgba(255, 255, 255, 0.4);

    &.done {
      background: #fff;
    }
  }

  .card {
    margin-top: r(10);
    padding: r(4) r(14);
    background: #fff;
    border-radius: r(8);
  }

  .card-title {
    padding: r(10) 0 r(4);
    font-size: r(15);
    font-weight: 600;
    color: #000;
  }

  .driver {
    display: flex;
    align-items: center;
    padding: r(12) r(14);
  }

  .driver-info {
    flex: 1;
    display: flex;
    flex-direction: column;
  }

  .driver-name {
    font-size: r(15);
    font-weight: 600;
    color: #000;
  }

  .driver-plate {
    margin-top: r(4);
    font-size: r(12);
    color: #666;
  }

  .driver-call {
    height: r(30);
    padding: 0 r(12);
    border-radius: r(15);
    background: $zd-blue;
    display: flex;
    align-items: center;
    font-size: r(12);
    color: #fff;

    text {
      margin-left: r(4);
    }
  }

  .cell {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: r(9) 0;
  }

  .cell-label {
    font-size: r(13);
    color: #666;
    flex-shrink: 0;
  }

  .cell-value {
    margin-left: r(20);
    font-size: r(13);
    color: #000;
    text-align: right;
    word-break: break-all;

    &.red {
      color: #ff3535;
    }

    &.price {
      font-size: r(16);
      font-weight: 600;
      color: #ff3535;
    }
  }

  .actions {
    display: flex;
    flex-wrap: wrap;
    gap: r(10);
    justify-content: flex-end;
    margin-top: r(16);
  }

  .btn {
    height: r(34);
    padding: 0 r(18);
    justify-content: center;
    border-radius: r(17);
    display: flex;
    align-items: center;
    font-size: r(13);
  }

  .actions .btn-primary { flex-basis: 100%; height: r(44); }

  .btn-plain {
    border: 1px solid #ccc;
    color: #333;
    background: #fff;
  }
</style>
