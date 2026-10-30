<template>
  <view class="page">
    <view class="header" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="title-bar">
        <text class="title">个人中心</text>
      </view>
      <view class="profile" @click="goProfile">
        <image class="avatar" :src="userStore.avatar" mode="aspectFill"></image>
        <text class="nickname">{{ info.nickname || '--' }}</text>
      </view>
    </view>

    <view class="stat-card">
      <view class="stat">
        <text class="stat-num">{{ stats.orderCount }}</text>
        <text class="stat-label">总计订单</text>
      </view>
      <view class="stat">
        <text class="stat-num">{{ money(stats.totalAmount) }}</text>
        <text class="stat-label">总计金额</text>
      </view>
      <view class="stat">
        <text class="stat-num">{{ money(stats.unauditedAmount) }}</text>
        <text class="stat-label">未审核金额</text>
      </view>
    </view>

    <view class="wallet-card">
      <view class="wallet-head">
        <image class="wallet-icon" src="/static/images/driver/icon-wallet.png" mode="aspectFit"></image>
        <text class="wallet-title">钱包余额（元）</text>
        <view class="flow-link" @click="goFlows">
          <text>查看流水</text>
          <uni-icons type="right" size="12" color="#343434"></uni-icons>
        </view>
      </view>
      <view class="wallet-body">
        <view class="wallet-left">
          <text class="yen">¥</text>
          <text class="balance">{{ money(wallet.balance) }}</text>
        </view>
        <view class="wallet-divider"></view>
        <view class="wallet-right">
          <text class="wallet-line">含运费金额：¥ {{ money(wallet.freight) }}</text>
          <text class="wallet-line">含保证金额：¥ {{ money(wallet.deposit) }}</text>
        </view>
      </view>
      <view class="wallet-actions">
        <view class="wallet-action" @click="openAmount('withdraw')">
          <text>提现</text>
          <view class="tri-right"></view>
        </view>
        <view class="wallet-action" @click="openAmount('recharge')">
          <text>充值</text>
          <view class="tri-right"></view>
        </view>
      </view>
    </view>

    <view class="menu">
      <view v-for="(m, i) in menus" :key="m.text" class="menu-item" @click="m.action">
        <image v-if="i < 4" class="menu-icon" :src="'/static/images/driver/menu-' + i + '.png'" mode="aspectFit"></image>
        <text class="menu-text" :class="{ 'no-icon': i >= 4 }">{{ m.text }}</text>
        <uni-icons type="right" size="17" color="#333"></uni-icons>
      </view>
    </view>

    <zd-tab-bar current="mine"></zd-tab-bar>

    <!-- 客服电话弹层 -->
    <view v-if="sheetVisible" class="sheet-mask" @click="sheetVisible = false">
      <view class="sheet" @click.stop>
        <view class="sheet-title">{{ servicePhone }}</view>
        <view class="sheet-item" @click="call">呼叫</view>
        <view class="sheet-gap"></view>
        <view class="sheet-item" @click="sheetVisible = false">取消</view>
      </view>
    </view>

    <!-- 提现 / 充值金额弹窗 -->
    <view v-if="amountType" class="modal-mask" @click="amountType = ''">
      <view class="modal" @click.stop>
        <view class="modal-title">{{ amountType === 'withdraw' ? '提现' : '充值保证金' }}</view>
        <view class="modal-tip">
          {{ amountType === 'withdraw' ? '可提现运费 ¥' + money(wallet.freight) + '，保证金不可提现' : '演示环境，充值不会产生真实扣款' }}
        </view>
        <view class="amount-input">
          <text class="amount-yen">¥</text>
          <input v-model="amount" type="digit" placeholder="请输入金额" placeholder-class="amount-ph" :focus="true" />
        </view>
        <view class="modal-btns">
          <view class="modal-btn" @click="amountType = ''">取消</view>
          <view class="modal-btn primary" @click="submitAmount">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import config from '@/config'
  import { useUserStore } from '@/store'
  import { recharge, withdraw } from '@/api/wallet'
  import { money } from '@/utils/format'
  import { PRIVACY_TEXT, SERVICE_AGREEMENT_TEXT, COLLECTED_INFO_TEXT, THIRD_PARTY_TEXT } from '@/utils/texts'

  // H5 没有系统状态栏，按截图（iPhone 状态栏 47pt）留出同样高度，保持页面比例
  const statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 47
  const userStore = useUserStore()
  const servicePhone = config.appInfo.servicePhone

  const info = computed(() => userStore.info || {})
  const stats = computed(() => info.value.stats || { orderCount: 0, totalAmount: 0, unauditedAmount: 0 })
  const wallet = computed(() => info.value.wallet || { balance: 0, freight: 0, deposit: 0 })

  onShow(() => {
    userStore.ensureLogin().then(() => userStore.fetchInfo()).catch(() => {})
  })

  const openText = (title, content) => {
    uni.navigateTo({ url: '/pages/common/textview?title=' + encodeURIComponent(title) + '&content=' + encodeURIComponent(content) })
  }

  const menus = [
    { text: '我的资料', action: goProfile },
    { text: '联系客服', action: () => (sheetVisible.value = true) },
    { text: '服务协议', action: () => openText('服务协议', SERVICE_AGREEMENT_TEXT) },
    { text: '隐私协议', action: () => openText('隐私协议', PRIVACY_TEXT) },
    { text: '已收集个人信息清单', action: () => openText('已收集个人信息清单', COLLECTED_INFO_TEXT) },
    { text: '第三方共享信息清单', action: () => openText('第三方共享信息清单', THIRD_PARTY_TEXT) }
  ]

  const sheetVisible = ref(false)
  function call() {
    sheetVisible.value = false
    uni.makePhoneCall({ phoneNumber: servicePhone })
  }

  function goProfile() {
    uni.navigateTo({ url: '/pages/mine/profile' })
  }

  function goFlows() {
    uni.navigateTo({ url: '/pages/mine/flows' })
  }

  const amountType = ref('')
  const amount = ref('')
  function openAmount(type) {
    amountType.value = type
    amount.value = ''
  }
  function submitAmount() {
    const fn = amountType.value === 'withdraw' ? withdraw : recharge
    const title = amountType.value === 'withdraw' ? '提现申请已提交' : '充值成功'
    fn(amount.value).then(() => {
      amountType.value = ''
      uni.showToast({ title, icon: 'none' })
      userStore.fetchInfo()
    })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
  }

  .header {
    height: r(174);
    box-sizing: border-box;
    background:
      radial-gradient(ellipse 70% 60% at 45% 0%, rgba(86, 190, 255, 0.85), rgba(86, 190, 255, 0) 100%),
      linear-gradient(180deg, #4389fd 0%, #3f84fc 55%, #2a80f7 100%);
  }

  .title-bar {
    height: 44px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .title {
    font-size: r(15);
    color: #fff;
  }

  .profile {
    display: flex;
    align-items: center;
    padding-left: r(33);
    margin-top: r(5);
  }

  .avatar {
    width: r(54);
    height: r(54);
    border-radius: 50%;
  }

  .nickname {
    margin-left: r(16);
    font-size: r(16);
    font-weight: bold;
    color: #d9e8fe;
  }

  .stat-card,
  .wallet-card,
  .menu {
    margin-left: r(11);
    margin-right: r(13);
    border-radius: r(10);
    box-shadow: 0 0 r(4) rgba(0, 0, 0, 0.04);
  }

  .stat-card {
    position: relative;
    margin-top: r(-11);
    height: r(84);
    display: flex;
    background: linear-gradient(180deg, #f5faff, #fdfeff);
  }

  .stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: r(19);
    box-sizing: border-box;
  }

  .stat-num {
    font-size: r(28);
    line-height: r(28);
    font-weight: bold;
    color: #191919;
  }

  .stat-label {
    margin-top: r(9);
    font-size: r(13);
    line-height: r(15);
    font-weight: bold;
    color: #000;
  }

  .wallet-card {
    margin-top: r(11);
    background: #fff;
    overflow: hidden;
  }

  .wallet-head {
    height: r(46);
    display: flex;
    align-items: center;
    padding: 0 r(25) 0 r(26);
    background: linear-gradient(90deg, #eff5ff, #feffff);
  }

  .wallet-icon {
    width: r(24);
    height: r(24);
  }

  .wallet-title {
    margin-left: r(7);
    font-size: r(15);
    font-weight: bold;
    color: #203152;
  }

  .flow-link {
    margin-left: auto;
    display: flex;
    align-items: center;
    font-size: r(12);
    color: #666;
  }

  .wallet-body {
    position: relative;
    height: r(73);
    display: flex;
  }

  .wallet-left {
    width: r(184);
    display: flex;
    align-items: baseline;
    justify-content: center;
    padding-top: r(25);
    box-sizing: border-box;
  }

  .yen {
    font-size: r(14);
    font-weight: bold;
    color: #2d2c2c;
  }

  .balance {
    margin-left: r(1);
    font-size: r(28);
    line-height: r(28);
    font-weight: bold;
    color: #2d2c2c;
  }

  .wallet-divider {
    position: absolute;
    left: r(184);
    top: 0;
    width: 1px;
    height: r(36);
    background: #f2f3f2;
  }

  .wallet-right {
    padding: r(24) 0 0 r(46);
    display: flex;
    flex-direction: column;
  }

  .wallet-line {
    font-size: r(13);
    line-height: r(24);
    color: #2f2d2d;
  }

  .wallet-actions {
    height: r(66);
    padding-top: r(4);
    box-sizing: border-box;
    display: flex;
  }

  .wallet-action {
    width: r(184);
    display: flex;
    align-items: center;
    justify-content: center;
    padding-bottom: r(18);
    box-sizing: border-box;
    font-size: r(15);
    font-weight: bold;
    color: #1f4d95;
  }

  .wallet-action:first-child {
    padding-right: r(12);
  }

  .tri-right {
    margin-left: r(6);
    width: 0;
    height: 0;
    border-top: r(4) solid transparent;
    border-bottom: r(4) solid transparent;
    border-left: r(5) solid #1f4d95;
  }

  .menu {
    margin-top: r(10);
    margin-bottom: r(12);
    background: #fff;
    overflow: hidden;
  }

  .menu-item {
    position: relative;
    height: r(53.3);
    display: flex;
    align-items: center;
    padding: 0 r(28) 0 r(25);
  }

  .menu-item + .menu-item::before {
    content: '';
    position: absolute;
    top: 0;
    left: r(31);
    right: r(31);
    height: 1px;
    background: #f2f2f3;
  }

  .menu-icon {
    width: r(18);
    height: r(18);
    margin-right: r(8);
  }

  .menu-text {
    flex: 1;
    font-size: r(14);
    color: #000;
  }

  .menu-text.no-icon {
    padding-left: 0;
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
    height: r(52);
    line-height: r(52);
    text-align: center;
    font-size: r(14);
    color: #999;
    border-bottom: 1px solid #f2f2f2;
  }

  .sheet-item {
    height: r(54);
    line-height: r(54);
    text-align: center;
    font-size: r(17);
    color: #333;
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
    width: r(300);
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

  .modal-tip {
    padding: r(8) r(24) 0;
    text-align: center;
    font-size: r(12);
    color: #999;
  }

  .amount-input {
    margin: r(16) r(24) r(22);
    height: r(46);
    padding: 0 r(12);
    display: flex;
    align-items: center;
    border-radius: r(8);
    background: #f4f5f7;
    font-size: r(18);
  }

  .amount-yen {
    margin-right: r(6);
    font-weight: bold;
    color: #333;
  }

  .amount-input input {
    flex: 1;
    font-size: r(18);
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

<style lang="scss">
  .amount-ph {
    color: #bbb;
    font-size: 15px;
  }
</style>
