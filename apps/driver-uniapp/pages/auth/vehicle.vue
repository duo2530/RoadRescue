<template>
  <view class="page">
    <zd-nav-bar title="车辆信息"></zd-nav-bar>

    <view class="policy">
      <image class="policy-icon" src="/static/images/driver/icon-warn-red.png" mode="aspectFit"></image>
      <text class="policy-text">完成驾驶证、行驶证、人车合影认证后，可承接 100 公里以内的短途拖车业务。</text>
    </view>

    <view class="section">
      <view class="label"><text class="star">*</text>驾驶证 / 行驶证</view>
      <view class="pair">
        <zd-upload v-model="form.license" class="half" label="驾驶证主页"></zd-upload>
        <zd-upload v-model="form.vehicleLicense" class="half" label="行驶证主页"></zd-upload>
      </view>
    </view>

    <view class="section">
      <view class="label"><text class="star">*</text>人车合影</view>
      <view class="sub">请站在车头一侧，车牌号需清晰可见</view>
      <zd-upload v-model="form.vehiclePhoto" class="half single" label="人车合影"></zd-upload>
    </view>

    <view class="form">
      <view class="row">
        <text class="row-label"><text class="star">*</text>车牌号</text>
        <input v-model="form.plate" class="row-input" maxlength="8" placeholder="请输入车牌号，如沪D12345" placeholder-class="ph" />
      </view>
      <view class="row" @click="pickTow">
        <text class="row-label"><text class="star">*</text>拖车类型</text>
        <text class="row-value">{{ form.towType }}</text>
        <uni-icons type="right" size="13" color="#999"></uni-icons>
      </view>
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

  const TOW_TYPES = ['斜板', '平板', '拖吊一体', '轻型清障车']
  const userStore = useUserStore()
  const form = reactive({ license: '', vehicleLicense: '', vehiclePhoto: '', plate: '', towType: '斜板' })

  onLoad(() => {
    userStore.ensureLogin().then(getAuthStatus).then(res => {
      if (res.data.vehicle) Object.assign(form, res.data.vehicle)
    }).catch(() => {})
  })

  function pickTow() {
    uni.showActionSheet({
      itemList: TOW_TYPES,
      success: r => (form.towType = TOW_TYPES[r.tapIndex])
    })
  }

  function submit() {
    submitAuth('vehicle', { ...form, plate: form.plate.trim().toUpperCase() }).then(() => {
      userStore.fetchInfo()
      uni.showToast({ title: '提交成功，已解锁短途拖车业务', icon: 'none' })
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
