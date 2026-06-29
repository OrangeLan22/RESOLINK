<template>
  <el-form :model="editForm">
    <el-form-item label="企业名称">
      <el-input v-model="editForm.companyName" placeholder="请输入企业名称" />
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { usePublicInfoStore } from '../../stores/publicInfo'
import { ElMessage, ElLoading } from 'element-plus'

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 编辑表单
const editForm = ref({
  companyName: ''
})

// 组件挂载时初始化表单数据
onMounted(() => {
  editForm.value.companyName = publicInfoStore.publicData?.[0]?.companyName || ''
})

// 定义emit事件
const emit = defineEmits(['close', 'success'])

// 提交编辑表单
const submitEditForm = async () => {
  // 这里可以添加表单验证逻辑
  if (!editForm.value.companyName.trim()) {
    ElMessage.error('企业名称不能为空')
    return
  }
  
  // 检查是否有token并去除引号
  let token = localStorage.getItem('token')
  // 去除可能存在的引号
  token = token ? token.replace(/^['"]|['"]$/g, '').trim() : ''
  if (!token) {
    ElMessage.error('请先登录')
    return
  }
  
  // 创建加载动画
  const loading = ElLoading.service({
    lock: true,
    text: '',
    background: 'rgba(0,0,0,0.3)'
  })

  try {
    // 调用store方法更新企业名称
    await publicInfoStore.updateCompanyName(editForm.value.companyName)
    ElMessage.success('企业信息更新成功')
    emit('success')
  } catch (error) {
    console.error('更新企业信息失败:', error)
    // 直接检查statusCode是否为403
    if (error.statusCode === 403) {
      ElMessage.error('权限错误')
    } else {
      ElMessage.error('操作失败，请重试')
    }
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

:deep(.is-focus) {
  box-shadow: 0 0 0 1px #3f51b5;
}
</style>