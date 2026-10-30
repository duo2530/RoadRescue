<template>
  <view class="upload" :style="{ width, height }" @click="choose">
    <image v-if="modelValue" class="photo" :src="modelValue" mode="aspectFill"></image>
    <image v-else-if="placeholder" class="photo" :src="placeholder" mode="aspectFill"></image>
    <view v-else class="blank">
      <view class="corner lt"></view>
      <view class="corner rt"></view>
      <view class="corner lb"></view>
      <view class="corner rb"></view>
      <image class="camera" src="/static/images/driver/icon-camera.png" mode="aspectFit"></image>
      <text class="hint">点击拍摄或上传</text>
      <view v-if="label" class="bar">{{ label }}</view>
    </view>
    <view v-if="modelValue" class="redo">重新上传</view>
  </view>
</template>

<script setup>
  import { pickImage } from '@/utils/image'

  /**
   * 证件/照片上传卡片。有截图素材的（头像、身份证）用 placeholder 图还原原样式，
   * 其余（驾驶证、行驶证等截图里没有的）按同样风格用 CSS 绘制。
   */
  const props = defineProps({
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    label: { type: String, default: '' },
    // 不传时由外部 class 控制尺寸（内联样式会覆盖 class）
    width: { type: String, default: '' },
    height: { type: String, default: '' }
  })
  const emit = defineEmits(['update:modelValue', 'change'])

  function choose() {
    pickImage().then(url => {
      emit('update:modelValue', url)
      emit('change', url)
    }).catch(() => {})
  }
</script>

<style lang="scss" scoped>
  .upload {
    position: relative;
    overflow: hidden;
    border-radius: r(4);
    background: #f8fbff;
  }

  .photo {
    display: block;
    width: 100%;
    height: 100%;
  }

  .blank {
    position: relative;
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: r(18);
    box-sizing: border-box;
  }

  .corner {
    position: absolute;
    width: r(12);
    height: r(12);
    border-color: #2b84ff;
    border-style: solid;
    border-width: 0;
  }

  .lt {
    left: r(6);
    top: r(6);
    border-left-width: 2px;
    border-top-width: 2px;
  }

  .rt {
    right: r(6);
    top: r(6);
    border-right-width: 2px;
    border-top-width: 2px;
  }

  .lb {
    left: r(6);
    bottom: r(6);
    border-left-width: 2px;
    border-bottom-width: 2px;
  }

  .rb {
    right: r(6);
    bottom: r(6);
    border-right-width: 2px;
    border-bottom-width: 2px;
  }

  .camera {
    width: r(34);
    height: r(34);
  }

  .hint {
    margin-top: r(6);
    font-size: r(12);
    color: #2b84ff;
  }

  .bar {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: r(24);
    line-height: r(24);
    text-align: center;
    font-size: r(12);
    color: #fff;
    background: linear-gradient(90deg, #6aa5ff, #2b84ff);
  }

  .redo {
    position: absolute;
    right: 0;
    bottom: 0;
    padding: 0 r(8);
    height: r(22);
    line-height: r(22);
    border-top-left-radius: r(8);
    font-size: r(11);
    color: #fff;
    background: rgba(0, 0, 0, 0.45);
  }
</style>
