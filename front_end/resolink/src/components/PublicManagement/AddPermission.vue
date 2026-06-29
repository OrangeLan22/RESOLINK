<template>
  <el-form :model="permissionForm">
    <el-form-item label="权限名称">
      <el-input v-model="permissionForm.permissionName" placeholder="请输入自定义权限名称" />
      <div v-if="permissionForm.permissionName === '高级管理员'" class="error-message">
        禁止使用的名称
      </div>
    </el-form-item>
    <el-form-item label="权限选项">
      <el-checkbox-group v-model="permissionForm.permissions">
          <el-checkbox value="资源预约" label="资源预约" />
          <el-checkbox value="公共信息管理" label="公共信息管理" />
          <el-checkbox value="账号管理" label="账号管理" />
          <el-checkbox value="资源管理" label="资源管理" />
          <el-checkbox value="历史记录" label="历史记录" />
          <el-checkbox value="设备检查" label="设备检查" />
        </el-checkbox-group>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 获取权限store
const publicInfoStore = usePublicInfoStore()

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

// 计算属性：是否禁用提交按钮
const isSubmitDisabled = computed(() => {
  return !permissionForm.value.permissionName.trim() || 
         permissionForm.value.permissionName === '高级管理员' ||
         permissionForm.value.permissions.length === 0
})

// 定义emit事件
const emit = defineEmits(['close', 'success'])

// 提交表单
const submitEditForm = async () => {
  // 表单验证
  if (!permissionForm.value.permissionName.trim()) {
    ElMessage.error('权限名称不能为空')
    return
  }
  
  if (permissionForm.value.permissionName === '高级管理员') {
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
        id: 0, // id固定为0
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
        ElMessage.success('自定义权限添加成功')
        emit('success')
      } else {
        ElMessage.error(data.message || '操作失败')
      }
    } else {
      ElMessage.error('操作失败，请重试')
    }
  } catch (error) {
    console.error('添加自定义权限失败:', error)
    ElMessage.error('操作失败，请重试')
  } finally {
    // 关闭加载动画
    loading.close()
  }
}

// 暴露方法给父组件
defineExpose({
  submitEditForm
})
</script>

<style scoped>
:deep(.el-form-item__label) {
  justify-content: left !important;
}

:deep(.el-button--primary) {
  margin-left: 0 !important;
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3f51b5 inset;
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

.error-message {
  position: absolute;
  top: 25px;
  left: 5px;
  color: #f56c6c;
  font-size: 12px;
  margin-top: 4px;
  text-align: left;
}

</style>