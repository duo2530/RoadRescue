<template>
  <view class="page">
    <zd-nav-bar title="选择车牌号"></zd-nav-bar>

    <view class="list">
      <view v-for="v in list" :key="v.id" class="item" @click="select(v)">
        <text class="plate">{{ v.plate }}</text>
        <view class="item-right">
          <text class="del" @click.stop="remove(v)">删除</text>
          <uni-icons v-if="v.plate === selected" type="checkmarkempty" size="18" color="#2e7ff8"></uni-icons>
        </view>
      </view>
      <view v-if="!list.length" class="empty">暂无车辆，请添加</view>
    </view>

    <view class="add">
      <input class="add-input" v-model="plate" maxlength="9" placeholder="请输入车牌号，如 沪A12345" placeholder-class="ph" />
      <view class="add-btn" @click="add">添加</view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onLoad } from '@dcloudio/uni-app'
  import { listVehicle, addVehicle, delVehicle } from '@/api/vehicle'

  const list = ref([])
  const selected = ref('')
  const plate = ref('')

  function load() {
    listVehicle().then(res => {
      list.value = res
    })
  }

  onLoad(options => {
    selected.value = decodeURIComponent(options.selected || '')
    load()
  })

  function select(v) {
    uni.$emit('vehicle:selected', v.plate)
    uni.navigateBack()
  }

  function add() {
    addVehicle(plate.value).then(v => {
      plate.value = ''
      select(v)
    }).catch(() => {})
  }

  function remove(v) {
    uni.showModal({
      title: '提示',
      content: `确定删除 ${v.plate} 吗？`,
      success: res => {
        if (res.confirm) delVehicle(v.id).then(load)
      }
    })
  }
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    background: $zd-bg;
  }

  .list {
    margin-top: r(10);
    background: #fff;
  }

  .item {
    height: r(52);
    padding: 0 r(18);
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #f1f2f5;
  }

  .plate {
    font-size: r(15);
    color: #000;
    letter-spacing: r(1);
  }

  .item-right {
    display: flex;
    align-items: center;
  }

  .del {
    margin-right: r(14);
    font-size: r(12);
    color: #999;
  }

  .empty {
    padding: r(30) 0;
    text-align: center;
    font-size: r(13);
    color: #999;
  }

  .add {
    margin: r(16) r(18);
    display: flex;
    align-items: center;
  }

  .add-input {
    flex: 1;
    height: r(40);
    padding: 0 r(12);
    background: #fff;
    border-radius: r(6);
    font-size: r(14);
  }

  .add-btn {
    width: r(72);
    height: r(40);
    margin-left: r(10);
    border-radius: r(6);
    background: $zd-blue;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: r(14);
    color: #fff;
  }
</style>

<style>
  .ph {
    color: #a6a6a6;
  }
</style>
