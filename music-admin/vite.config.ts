import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'

  return {
    plugins: [vue()],
    server: {
      // 关键：允许局域网/FRP 访问
      host: '0.0.0.0',
      port: Number(env.VITE_DEV_PORT || 15555),
      strictPort: true,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          headers: {
            'Accept-Charset': 'UTF-8'
          },
          // 增加大文件上传支持
          timeout: 600000, // 10分钟
          
          proxyTimeout: 600000, // 10分钟
          // 关键：禁用 body 解析，支持大文件流式传输
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq, req) => {
              // 设置更长的超时时间
              req.setTimeout(600000)
              // 流式传输 - 不缓存请求体
              proxyReq.setNoDelay(true)
            })
            proxy.on('error', (err) => {
              console.error('代理错误:', err.message)
            })
          }
        },
        '/static': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    },
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    }
  }
})
