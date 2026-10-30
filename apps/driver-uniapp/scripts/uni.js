// 源码位于项目根目录（HBuilderX 目录结构），uni CLI 默认读取 ./src，这里指定为根目录后再启动 CLI
const path = require('path')
process.env.UNI_INPUT_DIR = path.resolve(__dirname, '..')
require('@dcloudio/vite-plugin-uni/bin/uni.js')
