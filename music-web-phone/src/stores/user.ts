import { defineStore } from 'pinia'
import { currentUserApi, loginApi, registerApi } from '@/api/auth'
import type { UserInfo } from '@/api/types'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: (JSON.parse(localStorage.getItem('userInfo') || 'null') as UserInfo | null)
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    async login(username: string, password: string) {
      const data = await loginApi({ username, password })
      this.token = data.token
      this.userInfo = data.userInfo
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    },
    async register(username: string, password: string) {
      await registerApi({ username, password })
    },
    async refreshMe() {
      const data = await currentUserApi()
      if (data?.userInfo) {
        this.userInfo = data.userInfo
        localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
      }
    },
    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
