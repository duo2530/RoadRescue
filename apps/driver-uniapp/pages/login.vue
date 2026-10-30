<template>
  <view class="page">
    <view class="hero" :style="{ paddingTop: statusBarHeight + 'px' }">
      <text class="hero-title">中道救援司机端</text>
      <text class="hero-sub">订单又多又好，接单就上中道</text>
    </view>

    <view class="card">
      <view class="field">
        <uni-icons type="phone" size="18" color="#999"></uni-icons>
        <input v-model="phone" class="input" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
      </view>
      <view class="field">
        <uni-icons type="locked" size="18" color="#999"></uni-icons>
        <input v-model="code" class="input" type="number" maxlength="6" placeholder="请输入验证码" placeholder-class="ph" />
        <text class="code-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">{{ countdown > 0 ? countdown + 's 后重发' : '获取验证码' }}</text>
      </view>

      <view class="agree" @click="agreed = !agreed">
        <view class="check" :class="{ on: agreed }">
          <uni-icons v-if="agreed" type="checkmarkempty" size="11" color="#fff"></uni-icons>
        </view>
        <text>我已阅读并同意</text>
        <text class="link" @click.stop="openText('服务协议', SERVICE_AGREEMENT_TEXT)">《服务协议》</text>
        <text>和</text>
        <text class="link" @click.stop="openText('隐私协议', PRIVACY_TEXT)">《隐私协议》</text>
      </view>

      <view class="btn" @click="submit">登录</view>
      <view class="demo" @click="fillDemo">演示账号：{{ DEMO_PHONE }}，验证码 {{ DEMO_SMS_CODE }}（点击填入）</view>
    </view>
  </view>
</template>

<script setup>
  import { ref, onUnmounted } from 'vue'
  import { sendSmsCode } from '@/api/user'
  import { useUserStore } from '@/store'
  import { DEMO_PHONE } from '@/mock/db'
  import { DEMO_SMS_CODE } from '@/mock/rules'
  import { SERVICE_AGREEMENT_TEXT, PRIVACY_TEXT } from '@/utils/texts'

  // H5 没有系统状态栏，按截图（iPhone 状态栏 47pt）留出同样高度，保持页面比例
  const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 47
  const userStore = useUserStore()

  const phone = ref('')
  const code = ref('')
  const agreed = ref(false)
  const countdown = ref(0)
  let timer = null
  onUnmounted(() => clearInterval(timer))

  function sendCode() {
    if (countdown.value > 0) return
    sendSmsCode(phone.value).then(res => {
      uni.showToast({ title: res.hint, icon: 'none', duration: 2500 })
      countdown.value = 60
      timer = setInterval(() => {
        if (--countdown.value <= 0) clearInterval(timer)
      }, 1000)
    })
  }

  function fillDemo() {
    phone.value = DEMO_PHONE
    code.value = DEMO_SMS_CODE
    agreed.value = true
  }

  function submit() {
    if (!agreed.value) {
      uni.showToast({ title: '请先阅读并同意服务协议和隐私协议', icon: 'none' })
      return
    }
    uni.showLoading({ title: '登录中' })
    userStore.loginBy({ phone: phone.value, code: code.value }).then(() => {
      uni.hideLoading()
      uni.reLaunch({ url: '/pages/hall/index' })
    }).catch(() => uni.hideLoading())
  }

  function openText(title, content) {
    uni.navigateTo({ url: '/pages/common/textview?title=' + encodeURIComponent(title) + '&content=' + encodeURIComponent(content) })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
  }

  .hero {
    height: r(240);
    padding-left: r(30);
    display: flex;
    flex-direction: column;
    justify-content: center;
    background: linear-gradient(180deg, #cce9fe 0%, #d5e9fd 50%, #eaf3fe 100%);
  }

  .hero-title {
    margin-top: r(20);
    font-size: r(26);
    font-weight: bold;
    color: #0074eb;
  }

  .hero-sub {
    margin-top: r(8);
    font-size: r(14);
    color: #2a5d9f;
  }

  .card {
    position: relative;
    margin: r(-20) r(16) 0;
    padding: r(24) r(20) r(20);
    background: #fff;
    border-radius: r(12);
    box-shadow: 0 r(4) r(16) rgba(0, 0, 0, 0.05);
  }

  .field {
    height: r(50);
    display: flex;
    align-items: center;
    border-bottom: 1px solid #f0f0f0;
  }

  .input {
    flex: 1;
    margin-left: r(10);
    font-size: r(15);
  }

  .code-btn {
    font-size: r(14);
    color: #2b84ff;
  }

  .code-btn.disabled {
    color: #bbb;
  }

  .agree {
    margin-top: r(16);
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    font-size: r(12);
    color: #999;
  }

  .check {
    width: r(14);
    height: r(14);
    margin-right: r(6);
    border-radius: 50%;
    border: 1px solid #ccc;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .check.on {
    border-color: #2b84ff;
    background: #2b84ff;
  }

  .link {
    color: #2b84ff;
  }

  .btn {
    margin-top: r(24);
    height: r(46);
    line-height: r(46);
    border-radius: r(23);
    text-align: center;
    font-size: r(16);
    color: #fff;
    background: $zd-btn;
  }

  .demo {
    margin-top: r(16);
    text-align: center;
    font-size: r(12);
    color: #2b84ff;
  }
</style>

<style lang="scss">
  .ph {
    color: #bbb;
    font-size: 14px;
  }
</style>
