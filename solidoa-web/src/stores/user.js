import { defineStore } from 'pinia'
import { systemApi } from '@/api/system'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('access_token') || '',
    userInfo: null
  }),

  getters: {
    isLoggedIn: (state) => !!state.token
  },

  actions: {
    async login(username, password) {
      const res = await systemApi.login({ username, password })
      this.token = res.data.accessToken
      localStorage.setItem('access_token', this.token)
      await this.getUserInfo()
      return res
    },

    async getUserInfo() {
      // 从 token 解码获取用户信息，简化处理
      this.userInfo = {
        id: 1,
        username: 'admin',
        realName: '系统管理员'
      }
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('access_token')
    }
  }
})