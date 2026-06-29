<template>
  <div class="department-statuses">
    <!-- 部门名称编辑框 -->
    <div class="department-name-edit">
      <el-form-item label="部门名称">
        <div style="display: flex; gap: 8px; justify-content: center;">
          <el-input 
            v-model="departmentName" 
            placeholder="请输入部门名称"
            @change="handleNameChange"
            style="flex: 1;"
          />
          <el-button 
            type="danger"
            @click="handleDeleteDepartment"
            :disabled="!props.department?.dep_id"
          >
            删除部门
          </el-button>
        </div>
      </el-form-item>
    </div>

    <el-form-item label="身份标签">
      <div class="tag-container" :style="hasTags ? { gap: '8px' } : {}">
        <transition-group
          name="tag-transition"
          class="tags-wrapper"
          tag="div"
        >
          <el-tag
            v-for="status in department.statuses"
            :key="status.sta_id"
            closable
            :disable-transitions="true"
            effect="plain"
            @close="handleCloseTag(status.sta_id)"
          >
            {{ status.sta_name }}
          </el-tag>
        </transition-group>
        <el-input
          v-if="inputVisible"
          ref="inputRef"
          v-model="inputValue"
          class="tag-input"
          size="small"
          @keyup.enter="handleInputConfirm"
          @blur="handleInputConfirm"
        />
        <el-button v-else class="tag-add-btn" size="small" @click="showInput">
          + 新标签
        </el-button>
      </div>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, watch, watchEffect, nextTick, defineProps, defineEmits } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useTokenStore } from '../../stores/token'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 接收父组件传递的部门参数
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

// 定义事件
const emit = defineEmits(['update:departmentName', 'update:statuses'])

// 部门名称
const departmentName = ref('')

// 监听部门数据变化，更新部门名称
watch(() => props.department, (newDepartment) => {
  if (newDepartment) {
    departmentName.value = newDepartment.dep_name || ''
  }
}, { immediate: true })

// 处理部门名称变化
const handleNameChange = () => {
  emit('update:departmentName', departmentName.value)
}

// 标签输入相关
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref(null)

// 用于控制gap的状态变量，考虑动画延迟
const hasTags = ref(false)

// 监听标签变化，在动画结束后更新hasTags状态
watchEffect(() => {
  if (props.department?.statuses?.length > 0) {
    hasTags.value = true
  } else {
    // 当标签数组为空时，延迟更新hasTags以等待动画结束
    setTimeout(() => {
      hasTags.value = false
    }, 300) // 延迟时间应与动画持续时间一致
  }
})

// 显示标签输入框
const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// 确认标签输入
const handleInputConfirm = () => {
  if (inputValue.value && !props.department?.statuses?.some(status => status.sta_name === inputValue.value)) {
    // 为新添加的标签生成唯一的临时ID（使用负数）
    // 这样可以确保与服务器返回的正ID不冲突，也避免了重复
    const newStatus = {
      sta_id: -Date.now() - Math.random().toString(36).substr(2, 9),
      sta_name: inputValue.value
    }
    
    // 更新状态数组
    const updatedStatuses = [...(props.department.statuses || []), newStatus]
    emit('update:statuses', updatedStatuses)
  }
  inputVisible.value = false
  inputValue.value = ''
}

// 删除标签
const handleCloseTag = (statusId) => {
  if (props.department?.statuses) {
    const updatedStatuses = props.department.statuses.filter(status => status.sta_id !== statusId)
    emit('update:statuses', updatedStatuses)
  }
}

// 提交表单
const submitEditForm = async () => {
  // 表单验证
  if (!departmentName.value.trim()) {
    ElMessage.error('部门名称不能为空')
    return
  }
  
  try {
    // 获取token
    const tokenStore = useTokenStore()
    const token = tokenStore.getToken
    
    if (!token) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }
    
    // 构建请求体数据
    const requestData = {
      token,
      data: [{
        dep_id: props.department?.dep_id || 0,
        dep_name: departmentName.value,
        statuses: []
      }]
    }
    
    // 获取原始状态和当前状态的ID集合
    const originalStatusIds = new Set(props.originalDepartment?.statuses?.map(s => s.sta_id) || [])
    const currentStatusIds = new Set(props.department?.statuses?.map(s => s.sta_id) || [])
    
    // 处理身份标签：添加、修改、删除
    // 1. 处理当前状态中的标签
    for (const status of props.department?.statuses || []) {
      if (status.sta_id > 0) {
        // 原有标签，保留sta_id
        requestData.data[0].statuses.push({
          sta_id: status.sta_id,
          sta_name: status.sta_name
        })
      } else {
        // 新添加的标签（临时负数ID），sta_id设为0
        requestData.data[0].statuses.push({
          sta_id: 0,
          sta_name: status.sta_name
        })
      }
    }
    
    // 2. 处理删除的标签（只处理原始状态中的正ID）
    for (const originalId of originalStatusIds) {
      if (originalId > 0 && !currentStatusIds.has(originalId)) {
        // 找到被删除的标签，sta_id设为-1
        const deletedStatus = props.originalDepartment?.statuses?.find(s => s.sta_id === originalId)
        if (deletedStatus) {
          requestData.data[0].statuses.push({
            sta_id: -1,
            sta_name: deletedStatus.sta_name
          })
        }
      }
    }
    
    // 发送请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/manage-departments-statuses`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify(requestData)
    })
    
    const result = await response.json()
    
    if (response.status === 403) {
      ElMessage.error('权限错误')
      return
    }
    
    if (result.success) {
      ElMessage.success('部门信息保存成功')
      emit('success')
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    console.error('保存部门信息失败:', error)
    ElMessage.error('网络请求失败，请重试')
  }
}

// 删除部门
const handleDeleteDepartment = async () => {
  if (!props.department?.dep_id || !departmentName.value) {
    return
  }

  try {
    // 弹出确认对话框
    await ElMessageBox.confirm(
      `确定删除 <strong>${departmentName.value}</strong> 吗？这会同步删除此部门下的所有身份，此操作不可逆。`,
      '删除部门',
      {
        confirmButtonText: '确定删除',
        confirmButtonType: 'danger',
        cancelButtonText: '取消',
        type: 'warning',
        dangerouslyUseHTMLString: true
      }
    )

    // 获取token
    const tokenStore = useTokenStore()
    const token = tokenStore.getToken
    
    if (!token) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }

    // 构建删除请求体数据
    const requestData = {
      token,
      data: [{
        dep_id: -1,
        dep_name: departmentName.value
      }]
    }

    // 发送删除请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/public-info/manage-departments-statuses`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=utf-8'
      },
      body: JSON.stringify(requestData)
    })
    
    const result = await response.json()
    
    if (response.status === 403) {
      ElMessage.error('权限错误')
      return
    }
    
    if (result.success) {
      ElMessage.success('部门删除成功')
      emit('success')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除部门失败:', error)
      ElMessage.error('网络请求失败，请重试')
    }
  }
}

// 暴露方法给父组件
defineExpose({
  submitEditForm
})
</script>

<style scoped>
.department-statuses {
  padding: 0;
}

.department-name-edit {
  margin-bottom: 0;
}

:deep(.el-form-item__label) {
  justify-content: left !important;
}

.tag-container {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding: 5px 0;
}

.tag-input {
  width: 120px;
}

.tag-add-btn {
  margin-left: 0;
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

:deep(.el-tag) {
  background-color: rgba(63, 81, 181, 0.01) !important;
  color: #3f51b5 !important;
  border-color: #3f51b5;
}

.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

/* 自定义标签过渡动画 */
.tag-transition-enter-active,
.tag-transition-leave-active {
  transition: all 0.3s ease;
}

.tag-transition-enter-from,
.tag-transition-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.tag-transition-move {
  transition: transform 0.3s ease;
}

:deep(.el-tag .el-icon) {
  color: #717171 !important;
}
:deep(.el-tag .el-icon:hover) {
  color: #ff0000 !important;
}
:deep(.el-tag__close:hover) {
  background-color: transparent;
}

.no-statuses {
  color: #999;
  text-align: center;
  padding-bottom: 20px;
}

:deep(.el-form-item__content) {
  display: inline;
}
</style>