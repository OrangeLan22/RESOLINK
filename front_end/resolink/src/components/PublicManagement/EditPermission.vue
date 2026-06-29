<template>
  <div class="permission-edit">
    <el-form :model="permissionForm">
      <el-form-item label="权限名称">
        <div style="display: flex; gap: 8px; justify-content: center;">
          <el-input 
            v-model="permissionForm.permissionName" 
            placeholder="请输入自定义权限名称"
            :disabled="isSuperAdmin"
            style="flex: 1;"
          />
          <el-button 
            v-if="!isSuperAdmin"
            type="danger"
            @click="handleDeletePermission"
          >
            删除权限
          </el-button>
        </div>
        <div v-if="permissionForm.permissionName === '高级管理员' && !isSuperAdmin" class="error-message">
          禁止使用的名称
        </div>
      </el-form-item>

      <div v-if="isSuperAdmin" class="super-admin-warning">
        高级管理员权限禁止添加、修改、删除
      </div>
      
      <el-form-item label="权限选项">
        <el-checkbox-group v-model="permissionForm.permissions" :disabled="isSuperAdmin">
          <el-checkbox value="资源预约" label="资源预约" />
          <el-checkbox value="公共信息管理" label="公共信息管理" />
          <el-checkbox value="账号管理" label="账号管理" />
          <el-checkbox value="资源管理" label="资源管理" />
          <el-checkbox value="历史记录" label="历史记录" />
          <el-checkbox value="设备检查" label="设备检查" />
        </el-checkbox-group>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElLoading, ElMessageBox } from 'element-plus'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 获取权限store
const publicInfoStore = usePublicInfoStore()

// 定义props
const props = defineProps({
  department: {
    type: Object,
    default: null
  },
  originalDepartment: {
    type: Object,
    default: null
  }
})

// 权限表单
const permissionForm = ref({
  permissionName: '',
  permissions: []
})

// 错误信息
const errorMessage = ref('')

// 权限映射关系
const permissionMap = {
  '资源预约': 'appointment',
  '公共信息管理': 'public-info',
  '账号管理': 'account-mgm',
  '资源管理': 'resource-mgm',
  '历史记录': 'history',
  '设备检查': 'check'
}

// 计算属性：是否为高级管理员权限
const isSuperAdmin = computed(() => {
  return props.department?.id === 99 && props.department?.tag === '高级管理员'
})

// 监听权限数据变化，更新表单
watch(() => props.department, (newPermission) => {
  if (newPermission) {
    permissionForm.value.permissionName = newPermission.tag || ''
    
    // 转换权限格式
    const auth = newPermission.auth?.[0] || {}
    const permissions = []
    
    if (auth.appointment === 1) permissions.push('资源预约')
    if (auth['public-info'] === 1) permissions.push('公共信息管理')
    if (auth['account-mgm'] === 1) permissions.push('账号管理')
    if (auth['resource-mgm'] === 1) permissions.push('资源管理')
    if (auth.history === 1) permissions.push('历史记录')
    if (auth.check === 1) permissions.push('设备检查')
    
    permissionForm.value.permissions = permissions
  }
}, { immediate: true })

// 定义emit事件
const emit = defineEmits(['close', 'success'])

// 提交表单
const submitEditForm = async () => {
  // 表单验证
  if (!permissionForm.value.permissionName.trim()) {
    ElMessage.error('权限名称不能为空')
    return
  }
  
  if (permissionForm.value.permissionName === '高级管理员' && !isSuperAdmin.value) {
    ElMessage.error('禁止使用的名称')
    return
  }
  
  if (permissionForm.value.permissions.length === 0) {
    ElMessage.error('请至少选择一个权限选项')
    return
  }
  
  // 创建加载动画
  const loading = ElLoading.service({
    lock: true,
    text: '',
    background: 'rgba(0,0,0,0.3)'
  })

  try {
    // 转换权限格式
    const auth = {
      appointment: permissionForm.value.permissions.includes('资源预约') ? 1 : 0,
      'public-info': permissionForm.value.permissions.includes('公共信息管理') ? 1 : 0,
      'account-mgm': permissionForm.value.permissions.includes('账号管理') ? 1 : 0,
      'resource-mgm': permissionForm.value.permissions.includes('资源管理') ? 1 : 0,
      history: permissionForm.value.permissions.includes('历史记录') ? 1 : 0,
      check: permissionForm.value.permissions.includes('设备检查') ? 1 : 0
    }
    
    // 获取token
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    // 准备请求数据
    const requestData = {
      token: token, // 将token添加到请求体中
      data: [{
        id: props.department?.id , // 使用原ID
        tag: permissionForm.value.permissionName,
        auth: [auth]
      }]
    }
    
    // 发送请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/manage-authorities`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8' // 明确指定utf-8编码
      },
      body: JSON.stringify(requestData)
    })
    
    if (response.status === 403) {
      ElMessage.error('权限错误')
      return
    }

    if (response.status === 200) {
      const data = await response.json()
      if (data.success) {
        // 更新pinia中的权限列表
        publicInfoStore.authorities = data.data || []
        ElMessage.success('权限更新成功')
        emit('success')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } else {
      ElMessage.error('操作失败，请重试')
    }
  } catch (error) {
    console.error('更新权限失败:', error)
    ElMessage.error('操作失败，请重试')
  } finally {
    // 关闭加载动画
    loading.close()
  }
}

// 删除权限
const handleDeletePermission = async () => {
  let loading = null
  try {
    // 弹出确认对话框
    await ElMessageBox.confirm(
      `确定删除 <strong>${permissionForm.value.permissionName}</strong> 吗？此操作不可逆。`,
      '删除权限',
      {
        confirmButtonText: '确定删除',
        confirmButtonType: 'danger',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    )
    
    // 创建加载动画
    loading = ElLoading.service({
      lock: true,
      text: '',
      background: 'rgba(0,0,0,0.3)'
    })
    
    // 获取token
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    // 准备请求数据：删除时id设置为-1，tag不变，不需要auth字段
    const requestData = {
      token: token,
      data: [{
        id: -1, // 删除时id设置为-1
        tag: permissionForm.value.permissionName // tag保持不变
      }]
    }
    
    // 发送请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/manage-authorities`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify(requestData)
    })

    if (response.status === 403) {
      ElMessage.error('权限错误')
      return
    }

    const data = await response.json()

    if (response.status === 200) {
      if (data.success) {
        // 更新pinia中的权限列表
        publicInfoStore.authorities = data.data || []
        ElMessage.success('权限删除成功')
        emit('success')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } else {
      ElMessage.error(data.message || '操作失败，请重试')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除权限失败:', error)
      ElMessage.error('网络请求失败，请重试')
    }
  } finally {
    // 关闭加载动画
    if (loading) {
      loading.close()
    }
  }
}

// 暴露方法给父组件
defineExpose({
  submitEditForm
})
</script>

<style scoped>
.permission-edit {
  padding: 0;
}

:deep(.el-form-item__label) {
  justify-content: left !important;
}

.error-message {
  position: absolute;
  top: 25px;
  left: 5px;
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
  text-align: left;
}

.super-admin-warning {
  color: #f56c6c;
  font-size: 14px;
  margin: 18px 0;
  text-align: center;
  padding: 8px 15px;
  background-color: rgba(245, 108, 108, 0.1);
  border-radius: 4px;
  width: fit-content;
  position: relative;
  left: 50%;
  transform: translateX(-50%);
}

:deep(.el-button--small) {
  background-color: #fff !important;
  border: 1px solid #dcdfe6 !important;
}

:deep(.el-button--small:hover) {
  background-color: #f5f7fa !important;
  border-color: #3f51b5 !important;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px inset #3f51b5 !important;
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.el-checkbox__input.is-checked+.el-checkbox__label) {
  color: #3f51b5;
}

:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.el-checkbox__inner:hover) {
  border-color: #3f51b5;
}

:deep(.el-form-item__content) {
  display: inline;
}
</style>