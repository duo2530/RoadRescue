import { createSSRApp } from 'vue'
import App from './App'
import store from './store' // store
import { install } from './plugins' // plugins
import config from './config'

// #ifdef H5
// 地图 key 不写进 manifest.json（会被提交），构建时从 .env 注入后在此写入 uni 运行时配置
if (config.qqMapKey) {
  window.__uniConfig.qqMapKey = config.qqMapKey
}
// #endif

export function createApp() {
  const app = createSSRApp(App)
  app.use(store)
  install(app)
  return {
    app
  }
}
