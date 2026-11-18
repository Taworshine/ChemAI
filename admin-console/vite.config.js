import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 独立后台管理系统：端口 7200，与教师/学生端前端（5173）、移动端（7100）互不重叠
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 7200,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  }
})
