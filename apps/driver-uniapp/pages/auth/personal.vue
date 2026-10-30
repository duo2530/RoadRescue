<template>
  <view class="page">
    <zd-nav-bar title="信息注册"></zd-nav-bar>

    <view class="policy">
      <image class="policy-icon" src="/static/images/driver/icon-warn-red.png" mode="aspectFit"></image>
      <text class="policy-text">{{ POLICY_TEXT }}</text>
    </view>

    <view class="section">
      <view class="label"><text class="star">*</text>个人头像</view>
      <zd-upload v-model="form.avatar" class="avatar-upload" placeholder="/static/images/driver/upload-avatar.jpg"></zd-upload>
    </view>

    <view class="section">
      <view class="label bold"><text class="star">*</text>身份证正反面</view>
      <view class="id-row">
        <zd-upload v-model="form.idFront" class="id-upload" placeholder="/static/images/driver/upload-id-front.jpg" @change="ocr"></zd-upload>
        <zd-upload v-model="form.idBack" class="id-upload" placeholder="/static/images/driver/upload-id-back.jpg"></zd-upload>
      </view>
    </view>

    <view class="form">
      <view class="row">
        <text class="row-label"><text class="star">*</text>手机号</text>
        <input v-model="form.phone" class="row-input" type="number" maxlength="11" placeholder="请输入手机号" placeholder-class="ph" />
      </view>
      <view class="row">
        <text class="row-label"><text class="star">*</text>姓名</text>
        <input v-model="form.name" class="row-input" placeholder="上传身份证可自动代入" placeholder-class="ph" />
      </view>
      <view class="row">
        <text class="row-label"><text class="star">*</text>身份证</text>
        <input v-model="form.idCard" class="row-input" maxlength="18" placeholder="上传身份证可自动代入" placeholder-class="ph" />
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
  import { getAuthStatus, submitAuth, ocrIdCard } from '@/api/auth'
  import { useUserStore } from '@/store'
  import { POLICY_TEXT } from '@/utils/texts'

  const userStore = useUserStore()
  const form = reactive({ avatar: '', idFront: '', idBack: '', phone: '', name: '', idCard: '' })

  onLoad(() => {
    userStore.ensureLogin().then(info => {
      form.phone = info.phone
      return getAuthStatus()
    }).then(res => {
      // 已提交过则回显
      if (res.data.personal) Object.assign(form, res.data.personal)
    }).catch(() => {})
  })

  function ocr(image) {
    uni.showLoading({ title: '识别中' })
    ocrIdCard(image).then(res => {
      uni.hideLoading()
      if (!form.name) form.name = res.name
      if (!form.idCard) form.idCard = res.idCard
      uni.showToast({ title: '已自动代入姓名和身份证号', icon: 'none' })
    }).catch(() => uni.hideLoading())
  }

  function submit() {
    submitAuth('personal', { ...form }).then(() => {
      userStore.fetchInfo()
      uni.showToast({ title: '提交成功，已解锁小修业务', icon: 'none' })
      setTimeout(() => goBack('/pages/auth/index'), 1200)
    })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
    padding-bottom: calc(#{r(86)} + env(safe-area-inset-bottom));
  }

  /* 实测：粉底 91-179pt */
  .policy {
    display: flex;
    align-items: center;
    padding: r(14.5) r(14) r(14.5) r(16);
    background: #fff0ee;
  }

  .policy-icon {
    flex-shrink: 0;
    width: r(13);
    height: r(13);
    margin-right: r(10);
  }

  .policy-text {
    font-size: r(13.6);
    line-height: r(20);
    font-weight: bold;
    color: #ff6337;
  }

  /* 实测：白块 179-350 / 360-531pt，标签 top+22，上传卡 98 高，底部留 28 */
  .section {
    padding: r(22) r(17) r(28);
    background: #fff;
  }

  .section + .section {
    margin-top: r(10);
  }

  .label {
    font-size: r(14);
    line-height: r(15);
    color: $zd-text;
  }

  /* 原图「身份证正反面」加粗、「个人头像」不加粗，照原样保留 */
  .label.bold {
    font-size: r(15);
    font-weight: bold;
  }

  .star {
    margin-right: r(4);
    color: #ff0707;
  }

  .avatar-upload {
    display: block;
    margin-top: r(7);
    width: r(178);
    height: r(98);
  }

  .id-row {
    margin-top: r(7);
    display: flex;
    justify-content: space-between;
  }

  .id-upload {
    width: r(176);
    height: r(98);
  }

  /* 实测：541-656pt，三行间距 30 */
  .form {
    margin-top: r(10);
    padding: r(8.5) r(24) r(16.5) r(31);
    background: #fff;
  }

  .row {
    height: r(30);
    display: flex;
    align-items: center;
  }

  .row-label {
    width: r(100);
    font-size: r(15);
    color: $zd-text;
  }

  .row-label .star {
    margin-right: r(2);
  }

  .row-input {
    flex: 1;
    text-align: right;
    font-size: r(13);
    color: $zd-text;
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
    color: #fff;
    background: $zd-btn;
  }
</style>

<style lang="scss">
  .ph {
    color: #808080;
    font-size: 12px;
  }
</style>
