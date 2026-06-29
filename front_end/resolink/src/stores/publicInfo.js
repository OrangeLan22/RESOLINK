import { defineStore } from 'pinia'

export const usePublicInfoStore = defineStore('publicInfo', {
  state: () => ({
    // 公共信息数据结构，根据实际API返回的数据结构进行调整
    publicData: null,
    departments: [], // 存储部门和身份信息
    authorities: [], // 存储权限列表
    loading: false,
    error: null,
    hasPermission: true // 是否有权限访问页面
  }),
  persist: true,
  
  actions: {
    // 获取公共信息的方法
    async fetchPublicInfo() {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号、前后空格和反斜杠
        let token = localStorage.getItem('token') || ''
        // 去除可能存在的引号和反斜杠
        token = token.replace(/^['"]|['"]$/g, '').replace(/\\/g, '').trim()
        
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public`, {
          method: 'GET',
          headers: {
            'token': token // 将token添加到请求头中
          }
        })
        
        if (!response.ok) {
          throw new Error('Failed to fetch public information')
        }
        
        const data = await response.json()
        this.publicData = data
        return data
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 清除公共信息的方法
    clearPublicInfo() {
      this.publicData = null
      this.error = null
    },
    
    // 更新企业名称的方法
    async updateCompanyName(companyName) {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号和前后空格
        let token = localStorage.getItem('token') || ''
        // 去除可能存在的引号
        token = token.replace(/^['"]|['"]$/g, '').trim()
        
        // 调用真实API
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/company-name`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json;charset=utf-8' // 明确指定UTF-8编码
          },
          body: JSON.stringify({ 
            token, 
            "company-name": companyName 
          })
        })
        
        // 根据状态码处理不同情况
        if (response.status === 200) {
          // 更新本地数据
          if (this.publicData && this.publicData.length > 0) {
            this.publicData[0].companyName = companyName
          } else {
            this.publicData = [{ companyName }]
          }
          return true
        } else {
          // 非200状态视为失败，抛出包含状态码的错误对象
          const error = new Error(`更新失败，服务器返回${response.status}状态码`)
          error.statusCode = response.status // 添加状态码属性
          throw error
        }
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取部门和身份信息的方法
    async fetchDepartmentsWithStatuses() {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号和前后空格
        let token = localStorage.getItem('token') || ''
        token = token.replace(/^['"]|['"]$/g, '').trim()
        
        // 调用API获取部门和身份信息
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/departments-with-statuses`, {
          method: 'GET',
          headers: {
            'token': token // 将token添加到请求头中
          }
        })
        
        // 检查是否为403状态码
        if (response.status === 403) {
          this.hasPermission = false
          throw new Error('没有权限访问部门和身份信息')
        }
        
        if (!response.ok) {
          throw new Error('Failed to fetch departments and statuses')
        }
        
        const data = await response.json()
        if (data.success) {
          this.departments = data.data || []
          // 成功获取数据，设置为有权限
          this.hasPermission = true
        } else {
          throw new Error(data.message || '获取部门和身份信息失败')
        }
        return data
      } catch (error) {
        this.error = error.message
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取权限列表的方法
    async fetchAuthorities() {
      this.loading = true
      this.error = null
      
      try {
        // 获取token并去除引号和前后空格
        let token = localStorage.getItem('token') || ''
        token = token.replace(/^['"]|['"]$/g, '').trim()
        
        // 调用API获取权限列表
        const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/authorities`, {
          method: 'GET',
          headers: {
            'token': token // 将token添加到请求头中
          }
        })
        
        // 检查是否为403状态码
        if (response.status === 403) {
          this.hasPermission = false
          throw new Error('没有权限访问权限列表')
        }
        
        if (!response.ok) {
          throw new Error('Failed to fetch authorities')
        }
        
        const data = await response.json()
        if (data.success) {
          this.authorities = data.data || []
          // 成功获取数据，设置为有权限
          this.hasPermission = true
        } else {
          throw new Error(data.message || '获取权限列表失败')
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
    // 获取公共信息的getter
    getPublicInfo: (state) => state.publicData,
    
    // 检查是否正在加载
    isLoading: (state) => state.loading,
    
    // 获取错误信息
    getError: (state) => state.error
  }
})