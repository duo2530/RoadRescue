/**
 * 选择一张图片，返回可保存的图片地址。
 * 演示版（H5）：压缩为 dataURL 存进 mock 数据库，刷新页面后仍可显示；
 * 接真实后端时，这里改为 uni.uploadFile 上传后返回服务端 url，页面无需改动。
 */
const MAX_SIDE = 640

export function pickImage() {
  return new Promise((resolve, reject) => {
    uni.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: res => resolve(res.tempFilePaths[0]),
      fail: reject
    })
  }).then(path => {
    // #ifdef H5
    return compress(path)
    // #endif
    // #ifndef H5
    return path
    // #endif
  })
}

// #ifdef H5
function compress(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      const scale = Math.min(1, MAX_SIDE / Math.max(img.width, img.height))
      const canvas = document.createElement('canvas')
      canvas.width = Math.round(img.width * scale)
      canvas.height = Math.round(img.height * scale)
      canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height)
      resolve(canvas.toDataURL('image/jpeg', 0.7))
    }
    img.onerror = reject
    img.src = src
  })
}
// #endif
