import { defineConfig, loadEnv } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const gateway = env.VITE_GATEWAY || 'http://127.0.0.1:8600'
  return {
    // 让 .env 中的 TECENT_MAP_KEY 可通过 import.meta.env 读取
    envPrefix: ['VITE_', 'UNI_', 'TECENT_'],
    plugins: [uni()],
    server: {
      // apiMode=real 时 H5 请求 /customer-api/**，代理到 server-gateway
      proxy: {
        '/customer-api': { target: gateway, changeOrigin: true }
      }
    }
  }
})
