<template>
  <view class="page">
    <zd-nav-bar title="道路救援" :back="false"></zd-nav-bar>

    <view class="logo-wrap">
      <image class="logo" src="/static/images/rescue/login-logo.png" mode="aspectFit"></image>
    </view>

    <view class="form">
      <view v-if="!expanded" class="btn plain" @click="expanded = true">登录/注册</view>

      <template v-else>
        <view class="field">
          <input v-model="phone" class="input" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
        </view>
        <view class="field">
          <input v-model="code" class="input" type="number" maxlength="6" placeholder="请输入验证码" placeholder-class="ph" />
          <text class="code-btn" :class="{ disabled: countdown > 0 }" @click="sendCode">{{ countdown > 0 ? countdown + 's 后重发' : '获取验证码' }}</text>
        </view>
      </template>

      <view class="btn primary" :class="{ disabled: submitting }" @click="onPrimary">手机号快捷登录</view>
      <view v-if="expanded" class="demo" @click="fillDemo">演示账号：{{ DEMO_PHONE }}，验证码 {{ DEMO_SMS_CODE }}（点击填入）</view>
    </view>

    <view class="agree" @click="agreed = !agreed">
      <view class="check" :class="{ on: agreed }">
        <uni-icons v-if="agreed" type="checkmarkempty" size="11" color="#fff"></uni-icons>
      </view>
      <view class="agree-text">
        <text>已阅读并同意</text>
        <text class="link" @click.stop="openText('用户协议', USER_AGREEMENT_TEXT)">用户协议</text>
        <text>、</text>
        <text class="link" @click.stop="openText('隐私政策', PRIVACY_TEXT)">隐私政策</text>
        <text>。若您的手机号未注册，将为您直接注册账号</text>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, onUnmounted } from 'vue'
  import { sendSmsCode } from '@/api/user'
  import { useUserStore } from '@/store'
  import { DEMO_PHONE, DEMO_SMS_CODE } from '@/mock/db'
  import { USER_AGREEMENT_TEXT, PRIVACY_TEXT } from '@/utils/texts'

  const userStore = useUserStore()

  const expanded = ref(false)
  const phone = ref('')
  const code = ref('')
  const agreed = ref(false)
  const countdown = ref(0)
  const submitting = ref(false)
  let timer = null
  onUnmounted(() => clearInterval(timer))

  function tip(title) {
    uni.showToast({ title, icon: 'none', duration: 2500 })
  }

  function sendCode() {
    if (countdown.value > 0) return
    if (!/^1\d{10}$/.test(phone.value)) return tip('请输入正确的手机号')
    sendSmsCode(phone.value).then(res => {
      tip(res.hint)
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

  // 第一次点击只展开手机号和验证码输入，第二次起才提交，和截图里“登录/注册”与“手机号快捷登录”两个按钮的关系保持一致
  function onPrimary() {
    if (!expanded.value) {
      expanded.value = true
      return
    }
    if (!agreed.value) return tip('请先阅读并同意用户协议和隐私政策')
    if (!/^1\d{10}$/.test(phone.value)) return tip('请输入正确的手机号')
    if (!/^\d{6}$/.test(code.value)) return tip('请输入6位验证码')
    if (submitting.value) return
    submitting.value = true
    uni.showLoading({ title: '登录中' })
    userStore.loginBy({ phone: phone.value, code: code.value }).then(() => {
      uni.hideLoading()
      uni.reLaunch({ url: '/pages/index' })
    }).catch(() => uni.hideLoading()).finally(() => { submitting.value = false })
  }

  function openText(title, content) {
    uni.navigateTo({ url: `/pages/common/textview?title=${encodeURIComponent(title)}&content=${encodeURIComponent(content)}` })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: #fff;
    position: relative;
  }

  .logo-wrap {
    display: flex;
    justify-content: center;
    padding-top: r(112);
  }

  .logo {
    width: r(130);
    height: r(130);
  }

  .form {
    padding: r(150) r(24) 0;
  }

  .btn {
    height: r(52);
    margin-bottom: r(22);
    border-radius: r(26);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(18);
  }

  .plain {
    background: #f2f3f5;
    color: #111;
  }

  .primary {
    background: #1aad19;
    color: #fff;
  }

  .primary.disabled {
    opacity: 0.6;
  }

  .field {
    height: r(52);
    margin-bottom: r(22);
    padding: 0 r(22);
    border-radius: r(26);
    background: #f2f3f5;
    display: flex;
    align-items: center;
  }

  .input {
    flex: 1;
    font-size: r(16);
  }

  .code-btn {
    font-size: r(14);
    color: $zd-blue;
  }

  .code-btn.disabled {
    color: #aaa;
  }

  .demo {
    margin-top: r(-4);
    text-align: center;
    font-size: r(12);
    color: #999;
  }

  .agree {
    position: absolute;
    left: 0;
    right: 0;
    bottom: calc(#{r(62)} + env(safe-area-inset-bottom));
    padding: 0 r(40);
    display: flex;
    align-items: flex-start;
    justify-content: center;
  }

  .check {
    flex-shrink: 0;
    width: r(16);
    height: r(16);
    margin: r(3) r(7) 0 0;
    border: 1px solid #b8b8b8;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
  }

  .check.on {
    background: $zd-blue;
    border-color: $zd-blue;
  }

  .agree-text {
    font-size: r(14);
    line-height: r(21);
    color: #111;
    text-align: center;
  }

  .link {
    color: $zd-blue;
  }
</style>
