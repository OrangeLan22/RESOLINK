import { defineStore } from 'pinia'

export const useResourcesStore = defineStore('resources', {
  state: () => ({
    resources: [], // 存储资源列表数据
    physicalResources: [], // 存储物理资源列表数据
    loading: false,
    error: null,
    hasPermission: true // 是否有权限访问页面
  }),
  
  actions: {
    // 获取资源列表的方法
    async fetchResources() {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号和前后空格
        let token = localStorage.getItem('token') || ''
        token = token.replace(/^['"]|['"]$/g, '').trim()
        
        // 调用API获取资源列表
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/get-resources`, {
          method: 'GET',
          headers: {
            'token': token // 将token添加到请求头中
          }
        })
        
        // 处理403权限错误
        if (response.status === 403) {
          this.hasPermission = false
          console.error('没有权限访问资源信息')
        }
        
        if (!response.ok) {
          console.error('获取资源列表失败')
        }
        
        const data = await response.json()
        if (data.success) {
          this.resources = data.data || []
          // 成功获取数据，设置为有权限
          this.hasPermission = true
        } else {
          console.error(data.message || '获取资源列表失败')
        }
        return data
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取物理资源列表的方法
    async fetchPhysicalResources() {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号和前后空格
        let token = localStorage.getItem('token') || ''
        token = token.replace(/^['"]|['"]$/g, '').trim()
        
        // 调用API获取物理资源列表
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/get-physicals`, {
          method: 'GET',
          headers: {
            'token': token // 将token添加到请求头中
          }
        })
        
        // 处理403权限错误
        if (response.status === 403) {
          this.hasPermission = false
          throw new Error('没有权限访问物理资源信息')
        }
        
        if (!response.ok) {
          throw new Error('获取物理资源列表失败')
        }
        
        const data = await response.json()
        if (data.success) {
          this.physicalResources = data.data || []
          // 成功获取数据，设置为有权限
          this.hasPermission = true
        } else {
          throw new Error(data.message || '获取物理资源列表失败')
        }
        return data
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    }
  },
  
  getters: {
    // 获取资源列表的getter
    getResources: (state) => state.resources,
    
    // 获取物理资源列表的getter
    getPhysicalResources: (state) => state.physicalResources,
    
    // 检查是否正在加载
    isLoading: (state) => state.loading,
    
    // 获取错误信息
    getError: (state) => state.error,
    
    // 检查是否有权限
    isAuthorized: (state) => state.hasPermission
  }
})