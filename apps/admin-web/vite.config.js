import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  // 开发时 /mgr-api 代理到 server-gateway（默认 8600），网关再路由到 web-mgr
  const gateway = env.VITE_GATEWAY || 'http://127.0.0.1:8600'
  return {
    plugins: [vue()],
    server: {
      port: 9527,
      // 允许通过 Tailscale serve 的 HTTPS 域名访问
      allowedHosts: ['.ts.net'],
      proxy: {
        '/mgr-api': { target: gateway, changeOrigin: true }
      }
    }
  }
})
