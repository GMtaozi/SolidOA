import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarOpened: true,
    language: 'zh-cn'
  }),

  actions: {
    toggleSidebar() {
      this.sidebarOpened = !this.sidebarOpened
    }
  }
})