import { defineStore } from 'pinia'
import type { UserVO } from '@/api/types'
import { login as loginApi } from '@/api/user'

interface UserState {
  userInfo: UserVO | null
  token: string | null
  loading: boolean
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    userInfo: null,
    token: localStorage.getItem('token'),
    loading: false
  }),
  
  getters: {
    isLoggedIn: (state) => !!state.token
  },
  
  actions: {
    async login(username: string, password: string) {
      this.loading = true
      try {
        const response = await loginApi({ username, password })
        this.token = response.token
        this.userInfo = response.userInfo
        localStorage.setItem('token', response.token)
        return response
      } finally {
        this.loading = false
      }
    },
    
    logout() {
      this.token = null
      this.userInfo = null
      localStorage.removeItem('token')
    },
    
    setUserInfo(userInfo: UserVO) {
      this.userInfo = userInfo
    }
  }
})
