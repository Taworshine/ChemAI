import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from './router'

// 独立后台的 token 存储键，与教师端/学生端/移动端互不串用
const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('admin_token')
  if (token) cfg.headers['satoken'] = token
  return cfg
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body
      ElMessage.error(body.msg || '请求失败')
      if (body.code === 401) {
        localStorage.removeItem('admin_token')
        router.push('/login')
      }
      return Promise.reject(new Error(body.msg))
    }
    return body
  },
  err => {
    ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
