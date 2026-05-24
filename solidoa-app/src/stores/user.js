import { defineStore } from 'pinia'
import { api } from '@/api/index'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: uni.getStorageSync('access_token') || '',
    userInfo: null
  }),

  actions: {
    async login(username, password) {
      try {
        const res = await api.login({ username, password })
        this.token = res.data.accessToken
        uni.setStorageSync('access_token', this.token)
        await this.getUserInfo()
        return res
      } catch (error) {
        throw error
      }
    },

    async getUserInfo() {
      // 简化处理
      this.userInfo = {
        id: 1,
        username: 'admin',
        realName: '系统管理员'
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      uni.removeStorageSync('access_token')
    }
  }
})