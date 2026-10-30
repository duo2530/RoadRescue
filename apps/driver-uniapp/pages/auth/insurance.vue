<template>
  <view class="page">
    <zd-nav-bar title="责任险保单"></zd-nav-bar>

    <view class="policy">
      <image class="policy-icon" src="/static/images/driver/icon-warn-red.png" mode="aspectFit"></image>
      <text class="policy-text">完成承运人责任险认证后，可承接 100 公里以上的长途拖车业务和物流业务。保单需在有效期内。</text>
    </view>

    <view class="section">
      <view class="label"><text class="star">*</text>保单照片</view>
      <view class="sub">请上传保单首页，被保险人、保单号、保险期间需清晰可见</view>
      <zd-upload v-model="form.policyPhoto" class="half single" label="责任险保单"></zd-upload>
    </view>

    <view class="form">
      <view class="row">
        <text class="row-label"><text class="star">*</text>保单号</text>
        <input v-model="form.policyNo" class="row-input" maxlength="30" placeholder="请输入保单号" placeholder-class="ph" />
      </view>
      <picker mode="date" :value="form.expireDate" :start="today" @change="e => (form.expireDate = e.detail.value)">
        <view class="row">
          <text class="row-label"><text class="star">*</text>到期日</text>
          <text class="row-value" :class="{ empty: !form.expireDate }">{{ form.expireDate || '请选择保单到期日' }}</text>
          <uni-icons type="right" size="13" color="#999"></uni-icons>
        </view>
      </picker>
    </view>

    <view class="footer">
      <view class="btn" @click="submit">提交</view>
    </view>
  </view>
</template>

<script setup>
  import { goBack } from '@/utils/common'
  import { reactive } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { getAuthStatus, submitAuth } from '@/api/auth'
  import { useUserStore } from '@/store'

  const userStore = useUserStore()
  const today = new Date().toISOString().slice(0, 10)
  const form = reactive({ policyPhoto: '', policyNo: '', expireDate: '' })

  onLoad(() => {
    userStore.ensureLogin().then(getAuthStatus).then(res => {
      if (res.data.insurance) Object.assign(form, res.data.insurance)
    }).catch(() => {})
  })

  function submit() {
    submitAuth('insurance', { ...form, policyNo: form.policyNo.trim() }).then(() => {
      userStore.fetchInfo()
      uni.showToast({ title: '提交成功，已解锁全部业务', icon: 'none' })
      setTimeout(() => goBack('/pages/auth/index'), 1200)
    })
  }
</script>

<style lang="scss" scoped>
  @import './form.scss';
</style>

<style lang="scss">
  .ph {
    color: #808080;
    font-size: 12px;
  }
</style>
