// 服务器配置：APP 首次启动可改，H5 调试走 vite 代理
const DEFAULT_BASE = 'http://localhost:8080'

export function getBaseUrl() {
  // #ifdef H5
  return '' // H5 用相对路径，由 vite 代理转发到 8080
  // #endif
  return uni.getStorageSync('serverBase') || DEFAULT_BASE
}

export function setBaseUrl(url) {
  uni.setStorageSync('serverBase', url)
}

/** 统一请求封装：自动带 satoken，统一解 Result */
export function request(options) {
  const token = uni.getStorageSync('satoken')
  return new Promise((resolve, reject) => {
    uni.request({
      url: getBaseUrl() + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { satoken: token } : {}),
        ...(options.header || {})
      },
      success: (res) => {
        const body = res.data
        if (body && body.code === 200) {
          resolve(body.data)
        } else if (body && body.code === 401) {
          uni.removeStorageSync('satoken')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(body)
        } else {
          uni.showToast({ title: (body && body.msg) || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常，请检查服务器地址', icon: 'none' })
        reject(err)
      }
    })
  })
}
