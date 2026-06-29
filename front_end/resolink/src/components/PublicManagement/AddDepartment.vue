<template>
  <el-form :model="departmentForm">
    <el-form-item label="部门名称">
      <el-input v-model="departmentForm.departmentName" placeholder="请输入部门名称" />
    </el-form-item>
    <el-form-item label="身份标签">
      <div class="tag-container" :style="hasTags ? { gap: '8px' } : {}">
        <transition-group
          name="tag-transition"
          class="tags-wrapper"
          tag="div"
        >
          <el-tag
            v-for="tag in departmentForm.tags"
            :key="tag.id"
            closable
            :disable-transitions="true"
            effect="plain"
            @close="handleCloseTag(tag.id)"
          >
            {{ tag.name }}
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
  </el-form>
</template>

<script setup>
import { ref, nextTick, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'
import { useTokenStore } from '../../stores/token'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 生成唯一ID的函数
const generateId = () => {
  return Date.now() + Math.random().toString(36).substr(2, 9)
}

// 部门表单
const departmentForm = ref({
  departmentName: '',
  tags: []
})

// 标签输入相关
const inputVisible = ref(false)
const inputValue = ref('')
const inputRef = ref(null)

// 用于控制gap的状态变量，考虑动画延迟
const hasTags = ref(false)

// 监听标签变化，在动画结束后更新hasTags状态
watchEffect(() => {
  if (departmentForm.value.tags.length > 0) {
    hasTags.value = true
  } else {
    // 当标签数组为空时，延迟更新hasTags以等待动画结束
    setTimeout(() => {
      hasTags.value = false
    }, 300) // 延迟时间应与动画持续时间一致
  }
})

// 定义emit事件
const emit = defineEmits(['close', 'success'])

// 显示标签输入框
const showInput = () => {
  inputVisible.value = true
  nextTick(() => {
    inputRef.value?.focus()
  })
}

// 确认标签输入
const handleInputConfirm = () => {
  if (inputValue.value && !departmentForm.value.tags.some(tag => tag.name === inputValue.value)) {
    departmentForm.value.tags.push({
      id: generateId(),
      name: inputValue.value
    })
  }
  inputVisible.value = false
  inputValue.value = ''
}

// 删除标签
const handleCloseTag = (tagId) => {
  const index = departmentForm.value.tags.findIndex(tag => tag.id === tagId)
  if (index !== -1) {
    // 确保删除的是正确的标签
    console.log('删除标签:', tagId, '索引:', index)
    departmentForm.value.tags.splice(index, 1)
  }
}

// 提交表单
const submitEditForm = async () => {
  // 表单验证
  if (!departmentForm.value.departmentName.trim()) {
    ElMessage.error('部门名称不能为空')
    return
  }
  
  if (departmentForm.value.tags.length === 0) {
    ElMessage.error('请至少添加一个身份标签')
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
        dep_id: 0,
        dep_name: departmentForm.value.departmentName,
        statuses: departmentForm.value.tags.map(tag => ({
          sta_id: 0,
          sta_name: tag.name
        }))
      }]
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
      ElMessage.success('部门添加成功')
      emit('success')
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error) {
    console.error('添加部门失败:', error)
    ElMessage.error('网络请求失败，请重试')
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
</style>