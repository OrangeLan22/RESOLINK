import { defineStore } from 'pinia'

export const useUserInfoStore = defineStore('userInfo', {
  state: () => ({
    // 用户信息数据结构，根据登录API返回的数据结构调整
    userInfo: null,
    isLoggedIn: false,
    // 飞书绑定状态
    feishuBindStatus: {
      isBound: false,
      userInfo: null
    }
  }),
  
  // 配置持久化，确保只存储必要的字段
  persist: {
    key: 'userInfo',
    storage: localStorage,
    // 只持久化明确指定的字段
    paths: ['userInfo', 'isLoggedIn', 'feishuBindStatus'],
    // 添加序列化器，确保在存储前过滤掉任何额外字段
    serializer: {
      serialize: (state) => {
        // 只序列化指定的字段
        const { userInfo, isLoggedIn, feishuBindStatus } = state
        return JSON.stringify({ userInfo, isLoggedIn, feishuBindStatus })
      },
      deserialize: (value) => {
        try {
          const parsed = JSON.parse(value)
          // 确保只返回指定的字段
          return {
            userInfo: parsed.userInfo || null,
            isLoggedIn: parsed.isLoggedIn || false,
            feishuBindStatus: parsed.feishuBindStatus || { isBound: false, userInfo: null }
          }
        } catch (e) {
          return { userInfo: null, isLoggedIn: false, feishuBindStatus: { isBound: false, userInfo: null } }
        }
      }
    }
  },
  
  actions: {
    // 初始化store，清除任何可能存在的token
    initialize() {
      // 清除localStorage中可能存在的旧数据
      const stored = localStorage.getItem('userInfo')
      if (stored) {
        try {
          const parsed = JSON.parse(stored)
          // 如果包含token字段，重新存储过滤后的数据
          if (parsed.token) {
            delete parsed.token
            localStorage.setItem('userInfo', JSON.stringify(parsed))
          }
        } catch (e) {
          // 如果解析失败，清除数据
          localStorage.removeItem('userInfo')
        }
      }
    },
    
    // 设置用户信息
    setUserInfo(userData) {
      // 确保只提取用户信息部分，排除token
      if (!userData || typeof userData !== 'object') {
        this.userInfo = null
        this.isLoggedIn = false
        return
      }
      
      // 提取纯净的用户信息
      const userInfoData = userData.userInfo ? { ...userData.userInfo } : null
      
      // 确保用户信息中不包含token
      if (userInfoData) {
        delete userInfoData.token // 删除可能存在的token字段
      }

      // 检查authid是否为99，如果是则修改部门和身份名称
      if (userInfoData.authId === 99) {
        userInfoData.depName = '管理员'
        userInfoData.staName = '高级管理员'
      }
      
      // 更新状态
      this.userInfo = userInfoData
      this.isLoggedIn = true
    },
    
    // 清除用户信息（登出）
    clearUserInfo() {
      this.userInfo = null
      this.isLoggedIn = false
      this.feishuBindStatus = { isBound: false, userInfo: null }
      // 彻底清除localStorage中的数据
      localStorage.removeItem('userInfo')
    },
    
    // 更新飞书绑定状态
    updateFeishuBindStatus(status) {
      this.feishuBindStatus = status
    },
    
    // 清除飞书绑定状态
    clearFeishuBindStatus() {
      this.feishuBindStatus = { isBound: false, userInfo: null }
    }
  },
  
  getters: {
    // 获取用户信息
    getUserInfo: (state) => state.userInfo,

    // 检查是否已登录
    getIsLoggedIn: (state) => state.isLoggedIn,
    
    // 获取飞书绑定状态
    getFeishuBindStatus: (state) => state.feishuBindStatus
  }
})