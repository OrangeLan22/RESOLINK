import { defineStore } from 'pinia'

export const useAccountsStore = defineStore('accounts', {
  state: () => ({
    accounts: [], // 存储账户列表数据
    total: 0, // 总数据条数
    totalPages: 0, // 总页数
    loading: false,
    error: null,
    hasPermission: true // 默认有权限访问页面
  }),
  
  actions: {
    // 获取账户列表的方法
    async fetchAccounts(page = 1, size = 15) {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号和前后空格
        let token = localStorage.getItem('token') || ''
        token = token.replace(/^['"]|['"]$/g, '').trim()
        
        // 调用API获取账户列表
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/account-mgm/get-accounts`, {
          method: 'GET',
          headers: {
            'token': token, // 将token添加到请求头中
            'size': size.toString(),
            'page': page.toString()
          }
        })
        
        // 处理403权限错误
        if (response.status === 403) {
          this.hasPermission = false
          console.error('没有权限访问账号信息')
        }
        
        if (!response.ok) {
          console.error('获取账号信息失败')
        }
        
        const data = await response.json()
        if (data.success) {
          this.accounts = data.data || []
          this.total = data.total || 0
          this.totalPages = data.totalPages || 0
          // 成功获取数据，设置为有权限
          this.hasPermission = true
        } else {
          console.error(data.message || '获取账号信息失败')
        }
        return data
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 清除账户数据的方法
    clearAccounts() {
      this.accounts = []
      this.total = 0
      this.totalPages = 0
      this.error = null
    }
  },
  
  getters: {
    // 获取账户列表的getter
    getAccounts: (state) => state.accounts,
    
    // 获取总数据条数
    getTotal: (state) => state.total,
    
    // 获取总页数
    getTotalPages: (state) => state.totalPages,
    
    // 检查是否正在加载
    isLoading: (state) => state.loading,
    
    // 获取错误信息
    getError: (state) => state.error,
    
    // 检查是否有权限
    isAuthorized: (state) => state.hasPermission
  }
})