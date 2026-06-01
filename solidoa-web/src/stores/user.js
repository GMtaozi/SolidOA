/**
 * 用户状态管理
 */

import { defineStore } from 'pinia'
import request from '@/utils/request'
import { systemApi } from '@/api/system'
import { extractSafeError } from '@/utils/request'

// Token 安全存储密钥（建议后端统一管理此密钥）
const SECRET_KEY = 'SolidOA_Token_2024'
// XOR 混淆 + Base64 编码，防止 XSS 攻击者直接读取
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
  localStorage.setItem('token', xorEncrypt(token, SECRET_KEY))
}
const getToken = () => {
  const encoded = localStorage.getItem('token') || localStorage.getItem('access_token') || ''
  return encoded ? xorDecrypt(encoded, SECRET_KEY) : ''
}
const clearToken = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('access_token')
  localStorage.removeItem('refreshToken')
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken(),
    userInfo: null,
    roles: []
  }),

  getters: {
    // JWT格式验证：检查token格式并验证过期时间
    isLoggedIn: (state) => {
      if (!state.token) return false
      const parts = state.token.split('.')
      if (parts.length !== 3 || !parts.every(part => part.length > 0)) return false
      try {
        // JWT payload 使用 base64url 编码，需转换为标准 base64
        let base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
        // 添加缺失的 padding
        while (base64.length % 4) base64 += '='
        const payload = JSON.parse(atob(base64))
        return payload.exp > Date.now() / 1000
      } catch {
        return false
      }
    }
  },

  actions: {
    async login(username, password) {
      try {
        const response = await request.post('/v1/auth/login', {
          username,
          password
        })
        const result = response.data

        if (result.code === 200 && result.data?.accessToken) {
          this.token = result.data.accessToken
          saveToken(this.token)
          if (result.data.refreshToken) {
            localStorage.setItem('refreshToken', xorEncrypt(result.data.refreshToken, SECRET_KEY))
          } else {
            localStorage.removeItem('refreshToken')
          }
          await this.getUserInfo()
          return result
        } else {
          throw new Error(result.message || '登录失败')
        }
      } catch (error) {
        throw new Error(error.response?.data?.message || error.message || '登录失败')
      }
    },

    async getUserInfo() {
      try {
        const res = await systemApi.getCurrentUser()
        const userData = res.data?.data || res.data
        this.userInfo = userData
        this.roles = userData?.roles || []
        return this.userInfo
      } catch (error) {
        this.token = ''
        this.userInfo = null
        this.roles = []
        clearToken()
        // 使用统一的错误提取逻辑进行脱敏
        const safeError = extractSafeError(error)
        throw new Error(safeError.message)
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      this.roles = []
      clearToken()
    }
  }
})
