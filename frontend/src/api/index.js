import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({ baseURL: '/api', timeout: 70000 })

api.interceptors.request.use(config => {
  const token = localStorage.getItem('satoken')
  if (token) config.headers.satoken = token
  return config
})

api.interceptors.response.use(
  resp => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) return body
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body
  },
  err => {
    if (err.response && err.response.status === 401) {
      localStorage.clear()
      router.push('/login')
    }
    ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default api
