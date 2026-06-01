const BASE_URL = process.env.NODE_ENV === 'development'
  ? 'http://localhost:8080'
  : 'https://api.solidoa.com'

// Token 安全存储密钥
const SECRET_KEY = 'SolidOA_Token_2024'
// XOR 解密获取 token
const xorDecrypt = (encoded) => {
  try {
    const decoded = atob(encoded)
    const result = []
    for (let i = 0; i < decoded.length; i++) {
      result.push(String.fromCharCode(decoded.charCodeAt(i) ^ SECRET_KEY.charCodeAt(i % SECRET_KEY.length)))
    }
    return result.join('')
  } catch {
    return ''
  }
}

const request = (options) => {
  return new Promise((resolve, reject) => {
    // 安全获取加密存储的 token
    const encoded = uni.getStorageSync('access_token')
    const token = encoded ? xorDecrypt(encoded) : ''
    const headers = { 'Content-Type': 'application/json' }
    if (token) headers['Authorization'] = `Bearer ${token}`

    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: headers,
      success: (res) => {
        if (res.statusCode === 200) {
          if (res.data && res.data.code === 200) {
            resolve(res.data)
          } else {
            uni.showToast({ title: res.data?.message || '请求失败', icon: 'none' })
            reject(res.data)
          }
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('access_token')
          uni.navigateTo({ url: '/pages/login/index' })
          reject({ message: '未授权，请重新登录' })
        } else {
          uni.showToast({ title: '网络错误', icon: 'none' })
          reject(res)
        }
      },
      fail: (err) => {
        uni.showToast({ title: '请求失败', icon: 'none' })
        reject(err)
      }
    })
  })
}

export default {
  get: (url, data) => request({ url, method: 'GET', data }),
  post: (url, data) => request({ url, method: 'POST', data }),
  put: (url, data) => request({ url, method: 'PUT', data }),
  delete: (url, data) => request({ url, method: 'DELETE', data })
}