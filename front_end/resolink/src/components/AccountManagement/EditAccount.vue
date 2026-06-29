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
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElLoading, ElMessageBox } from 'element-plus'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 定义props
const props = defineProps({
  editData: {
    type: Object,
    required: true
  }
})

// 定义emit事件
const emit = defineEmits(['success', 'cancel'])

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 表单引用
const accountFormRef = ref()

// 表单数据
const accountForm = reactive({
  id: 0,
  name: '',
  employeeId: '',
  department: '',
  identity: '',
  username: '',
  permissions: ''
})

// 标志：是否正在初始化表单
const isInitializing = ref(true)

// 组件挂载时初始化表单数据
onMounted(() => {
  if (props.editData) {
    // 设置初始化标志
    isInitializing.value = true
    
    accountForm.id = props.editData.id
    accountForm.name = props.editData.name
    accountForm.employeeId = props.editData.emp_id
    accountForm.username = props.editData.useracc
    accountForm.permissions = props.editData.auth_id
    
    // 先设置部门，然后设置身份
    accountForm.department = props.editData.dep_id
    accountForm.identity = props.editData.sta_id
    
    // 初始化完成
    nextTick(() => {
      isInitializing.value = false
    })
  }
})

// 监听部门变化，更新可用的身份列表
watch(() => accountForm.department, (newDepartment) => {
  // 只有当不是初始化时（即用户手动改变部门）才重置身份
  if (!isInitializing.value) {
    accountForm.identity = ''
  }
})

// 计算属性：根据当前选择的部门过滤可用的身份
const availableIdentities = computed(() => {
  if (!accountForm.department) return []
  
  const selectedDepartment = publicInfoStore.departments.find(dep => {
    // 确保类型匹配进行比较
    return String(dep.dep_id) === String(accountForm.department)
  })
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

// 取消处理函数
const handleCancel = async () => {
  // 检查是否有修改
  const hasChanges = checkFormChanges()
  
  if (hasChanges) {
    // 显示二次确认对话框
    try {
      await ElMessageBox.confirm(
        '确定要放弃修改？',
        '暂未保存',
        {
          confirmButtonText: '确定',
          confirmButtonType: 'danger',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      // 用户确认取消，通知父组件
      emit('cancel')
    } catch (error) {
      // 用户取消操作，不做任何处理
    }
  } else {
    // 没有修改，直接通知父组件
    emit('cancel')
  }
}

// 检查表单是否有修改
const checkFormChanges = () => {
  if (!props.editData) return false
  
  return (
    accountForm.name !== props.editData.name ||
    accountForm.employeeId !== props.editData.emp_id ||
    accountForm.username !== props.editData.useracc ||
    accountForm.department !== props.editData.dep_id ||
    accountForm.identity !== props.editData.sta_id ||
    accountForm.permissions !== props.editData.auth_id
  )
}

// 提交表单
const submitForm = async () => {
  if (!accountFormRef.value) return
  await accountFormRef.value.validate((valid) => {
    if (valid) {
      // 检查是否有实际修改
      if (!checkFormChanges()) {
        // 没有修改，直接返回成功
        emit('success', { noChanges: true })
        return false
      }
      
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
        data: [{
          id: accountForm.id,
          emp_id: accountForm.employeeId,
          name: accountForm.name,
          useracc: accountForm.username,
          dep_id: Number(accountForm.department),
          sta_id: Number(accountForm.identity),
          auth_id: Number(accountForm.permissions)
        }]
      }
      
      // 发送请求
      fetch(`${import.meta.env.VITE_API_BASE_URL}/api/account-mgm/update-account`, {
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
          // 编辑成功
          ElMessage.success('账号编辑成功')
          // 传递更新后的数据给父组件，以便更新表格
          emit('success', data.data)
        } else {
          ElMessage.error(data.message || '编辑账号失败')
        }
      })
      .catch(error => {
        console.error('编辑账号失败:', error)
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
</style>