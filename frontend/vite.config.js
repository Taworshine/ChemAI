import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // G6 v4 的 ESM 入口存在循环依赖，Vite 预打包会报
      // "Class extends value [object Object] is not a constructor"
      // 改用自包含的 UMD 构建绕过该问题。
      '@antv/g6': '@antv/g6/dist/g6.min.js'
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
