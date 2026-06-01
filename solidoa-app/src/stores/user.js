import { defineStore } from 'pinia'
import { api } from '@/api/index'

// Token 安全存储密钥（建议后端统一管理此密钥）
const SECRET_KEY = 'SolidOA_Token_2024'
// 简单的 XOR 混淆 + Base64 编码，防止 XSS 攻击者直接读取
const xorEncrypt = (str, key) => {
  const result = []
  for (let i = 0; i < str.length; i++) {
    result.push(String.fromCharCode(str.charCodeAt(i) ^ key.charCodeAt(i % key.length)))
  }
  return btoa(result.join(''))
}
const xorDecrypt = (encoded, key) => {
  try {
    const decoded = atob(encoded)
    const result = []
    for (let i = 0; i < decoded.length; i++) {
      result.push(String.fromCharCode(decoded.charCodeAt(i) ^ key.charCodeAt(i % key.length)))
    }
    return result.join('')
  } catch {
    return ''
  }
}

const saveToken = (token) => {
  uni.setStorageSync('access_token', xorEncrypt(token, SECRET_KEY))
}
const getToken = () => {
  const encoded = uni.getStorageSync('access_token')
  return encoded ? xorDecrypt(encoded, SECRET_KEY) : ''
}
const clearToken = () => {
  uni.removeStorageSync('access_token')
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: null
  }),

  actions: {
    async login(username, password) {
      try {
        const res = await api.login({ username, password })
        // 安全提取 accessToken，处理各种响应格式
        const token = res.data?.accessToken || res.data?.token || res.accessToken || res.token
        if (!token) {
          throw new Error('登录响应缺少accessToken')
        }
        this.token = token
        saveToken(this.token)
        await this.getUserInfo()
        return res
      } catch (error) {
        throw error
      }
    },

    async getUserInfo() {
      try {
        const res = await api.getUserInfo()
        // 安全提取用户数据，明确处理各种响应格式
        const userData = res.data?.userInfo || res.data || res.userInfo || res
        if (userData && typeof userData === 'object' && userData.username) {
          this.userInfo = userData
        } else if (userData && typeof userData === 'object') {
          // 如果res.data存在但没有username，使用unknown
          this.userInfo = { username: 'unknown', ...userData }
        } else {
          this.userInfo = { username: 'unknown' }
        }
      } catch (e) {
        this.userInfo = { username: 'unknown' }
      }
    },

    async logout() {
      // 调用后端logout接口使token失效
      try {
        await api.logout()
      } catch (e) {
        console.warn('后端logout请求失败', e)
      }
      // 清除本地状态
      this.token = ''
      this.userInfo = null
      clearToken()
    }
  }
})