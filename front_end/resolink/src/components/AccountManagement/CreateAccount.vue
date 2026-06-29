<template>
  <el-form
    ref="accountFormRef"
    :model="accountForm"
    :rules="rules"
    class="account-form"
    :hide-required-asterisk="true"
  >
    <!-- 姓名 -->
    <el-form-item label="姓名" prop="name">
      <el-input v-model="accountForm.name" placeholder="员工姓名" />
    </el-form-item>

    <!-- 工号 -->
    <el-form-item label="工号" prop="employeeId">
      <el-input v-model="accountForm.employeeId" placeholder="员工工号" />
    </el-form-item>

    <!-- 账号 -->
    <el-form-item label="账号" prop="username">
      <el-input v-model="accountForm.username" placeholder="登录账号" />
    </el-form-item>

    <!-- 部门 -->
    <el-form-item label="部门" prop="department">
      <el-select v-model="accountForm.department" placeholder="所在部门" :loading="isLoading">
        <el-option 
          v-for="dept in publicInfoStore.departments" 
          :key="dept.dep_id" 
          :label="dept.dep_name" 
          :value="dept.dep_id" 
        />
      </el-select>
    </el-form-item>

    <!-- 身份 -->
    <el-form-item label="身份" prop="identity">
      <el-select 
        v-model="accountForm.identity" 
        :placeholder="accountForm.department ? '选择员工身份' : '请先选择部门'"
        :disabled="!accountForm.department"
      >
        <el-option 
          v-for="status in availableIdentities" 
          :key="status.sta_id" 
          :label="status.sta_name" 
          :value="status.sta_id" 
        />
      </el-select>
    </el-form-item>

    <!-- 权限 -->
    <el-form-item label="权限" prop="permissions">
      <el-select 
        v-model="accountForm.permissions" 
        placeholder="选择权限"
        :loading="isLoading"
      >
        <el-option 
          v-for="auth in filteredAuthorities" 
          :key="auth.id" 
          :label="auth.tag" 
          :value="auth.id" 
        />
      </el-select>
    </el-form-item>

    <!-- 操作按钮 -->
    <el-form-item class="form-actions" style="margin-bottom: 0">
      <el-button type="primary" @click="submitForm">提交</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-form-item>
  </el-form>

  <!-- 使用成功弹窗组件 -->
  <SuccessDialog 
    :visible="successDialogVisible"
    :useracc="successData.useracc"
    :password="successData.password"
    :message="`用户 <strong>${successData.useracc}</strong> 的初始密码为`"
    @close="closeSuccessDialog"
  />
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { usePublicInfoStore } from '../../stores/publicInfo'
import SuccessDialog from '../Common/SuccessDialog.vue'

// 定义emit事件
const emit = defineEmits(['success', 'cancel'])

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 表单引用
const accountFormRef = ref()

// 成功弹窗相关
const successDialogVisible = ref(false)
const successData = ref({ useracc: '', password: '' })

// 表单数据
const accountForm = reactive({
  name: '',
  employeeId: '',
  department: '',
  identity: '',
  username: '',
  permissions: ''
})

// 重置表单
const resetForm = () => {
  if (accountFormRef.value) {
    accountFormRef.value.resetFields()
    // 手动重置部门和身份的关系
    accountForm.identity = ''
  }
}

// 监听部门变化，更新可用的身份列表
watch(() => accountForm.department, (newDepartment) => {
  // 只要部门变化就重置身份
  accountForm.identity = ''
})

// 计算属性：根据当前选择的部门过滤可用的身份
const availableIdentities = computed(() => {
  if (!accountForm.department) return []
  
  const selectedDepartment = publicInfoStore.departments.find(dep => dep.dep_id === accountForm.department)
  // 确保statuses是数组格式
  return selectedDepartment?.statuses || []
})

// 计算属性：过滤后的权限列表（排除高级管理员权限）
const filteredAuthorities = computed(() => {
  return publicInfoStore.authorities.filter(auth => auth.id !== 99)
})

// 加载状态
const isLoading = computed(() => publicInfoStore.loading)

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  employeeId: [
    { required: true, message: '请输入工号', trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ],
  identity: [
    { required: true, message: '请选择身份', trigger: 'change' }
  ],
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, max: 16, message: '长度在 4 到 16 个字符', trigger: 'blur' }
  ],
  permissions: [
    { required: true, message: '请选择一个权限', trigger: 'change' }
  ]
}

// 关闭成功弹窗
const closeSuccessDialog = () => {
  successDialogVisible.value = false
  // 重置表单
  resetForm()
  // 触发成功事件
  emit('success')
}

// 提交表单
const submitForm = async () => {
  if (!accountFormRef.value) return
  await accountFormRef.value.validate((valid) => {
    if (valid) {
      // 显示提交loading
      const loading = ElLoading.service({
        lock: true,
        text: '',
        background: 'rgba(0,0,0,0.3)'
      })
      
      // 获取token
      let token = localStorage.getItem('token') || ''
      token = token.replace(/^['"]|['"]$/g, '').trim()
      
      // 准备请求数据
      const requestData = {
        token: token,
        data: {
          emp_id: accountForm.employeeId,
          name: accountForm.name,
          useracc: accountForm.username,
          dep_id: Number(accountForm.department),
          sta_id: Number(accountForm.identity),
          auth_id: Number(accountForm.permissions)
        }
      }
      
      // 发送请求
      fetch(`${import.meta.env.VITE_API_BASE_URL}/api/account-mgm/create-account`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8' // 确保使用正确的字符集
        },
        body: JSON.stringify(requestData)
      })
      .then(response => {
        if (response.status === 403) {
          ElMessage.error('权限错误')
          return Promise.reject('权限错误')
        }
        return response.json()
      })
      .then(data => {
        if (data.success) {
          // 创建成功，显示成功弹窗
          successData.value = data.data
          successDialogVisible.value = true
        } else {
          ElMessage.error(data.message || '创建账号失败')
        }
      })
      .catch(error => {
        console.error('创建账号失败:', error)
      })
      .finally(() => {
        // 关闭loading
        loading.close()
      })
    } else {
      ElMessage.error('表单验证失败，请检查输入')
      return false
    }
  })
}
</script>

<style scoped>
.account-form {
  padding: 0 20px;
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

:deep(.el-form-item__label) {
  justify-content: left !important;
}

:deep(.el-button--primary) {
  margin-left: 0 !important;
  background-color: #3f51b5;
  border-color: #3f51b5;
}

/* 自定义成功弹窗样式 */
.success-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(0px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  flex-direction: column;
  animation: dialog-overlay-fade-in 0.3s ease forwards;
}

.success-dialog-overlay.fade-out {
  animation: dialog-overlay-fade-out 0.3s ease forwards;
}

.success-dialog {
  background-color: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-width: 400px;
  width: 100%;
  transform: scale(0.8);
  opacity: 0;
  animation: dialog-fade-in 0.3s ease forwards;
}

.success-dialog-overlay.fade-out .success-dialog {
  animation: dialog-fade-out 0.3s ease forwards;
}

/* 渐入动画 */
@keyframes dialog-overlay-fade-in {
  to {
    backdrop-filter: blur(10px);
  }
}

@keyframes dialog-fade-in {
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 渐出动画 */
@keyframes dialog-overlay-fade-out {
  from {
    backdrop-filter: blur(10px);
  }
  to {
    backdrop-filter: blur(0px);
    opacity: 0;
  }
}

@keyframes dialog-fade-out {
  from {
    transform: scale(1);
    opacity: 1;
  }
  to {
    transform: scale(0.8);
    opacity: 0;
  }
}

.success-dialog-content {
  margin-bottom: 10px;
}

.success-dialog-content p {
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
}

.success-dialog-content strong {
  color: #333;
}

.success-dialog-footer {
  display: flex;
  justify-content: center;
}

.success-dialog-footer .el-button {
  min-width: 100px;
}

.password {
  margin: 30px 0 !important;
  letter-spacing: 5px;
  text-align: center;
  font-size: 40px !important;
  font-weight: bold;
  color: #000000;
}

.warn {
  display: flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 5px;
  background-color: #eeeeee;
  width: fit-content;
  margin-top: 20px;
}

.warn-icon {
  margin: 0 3px;
  font-size: 13px;
  color: #ff8b07;
}

.warn-text {
  position: relative;
  top: -1px;
  font-size: 13px;
  font-weight: normal;
  color: #ff8b07;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>