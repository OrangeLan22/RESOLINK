import { defineStore } from 'pinia'

export const useTokenStore = defineStore('token', {
  state: () => ({
    // 只存储token
    token: null
  }),
  persist: {
    key: 'token',
    storage: localStorage,
    serializer: {
      serialize: (state) => state.token,
      deserialize: (value) => ({ token: value })
    }
  },
  
  actions: {
    // 设置token
    setToken(tokenValue) {
      this.token = tokenValue
    },
    
    // 清除token（登出）
    clearToken() {
      this.token = null
    }
  },
  
  getters: {
    // 获取token
    getToken: (state) => state.token
  }
})