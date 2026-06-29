<template>
  <el-form
    ref="spaceFormRef"
    :model="spaceForm"
    :rules="rules"
    class="space-form"
    :hide-required-asterisk="true"
  >
    <!-- 空间名称 -->
    <el-form-item label="空间名称" prop="name">
      <el-input v-model="spaceForm.name" placeholder="输入空间名称" />
    </el-form-item>

    <!-- 空间位置 -->
    <el-form-item label="空间位置" prop="location">
      <el-input v-model="spaceForm.location" placeholder="输入空间位置" />
    </el-form-item>

    <!-- 空间类型 -->
    <el-form-item label="空间类型" prop="type">
      <el-input v-model="spaceForm.type" placeholder="输入空间类型" />
    </el-form-item>

    <!-- 空间标签（必填） -->
    <el-form-item label="空间标签" prop="tag">
      <el-select v-model="spaceForm.tag" placeholder="请选择空间标签">
        <el-option label="核心业务" value="核心业务" />
        <el-option label="公共辅助" value="公共辅助" />
        <el-option label="特殊功能" value="特殊功能" />
      </el-select>
    </el-form-item>

    <!-- 容纳人数 -->
    <el-form-item label="容纳人数" prop="capacity">
      <el-input-number 
        v-model="spaceForm.capacity" 
        :min="1" 
        :max="1000" 
        placeholder="1~1000"
      />
    </el-form-item>

    <!-- 公共资源 -->
    <el-form-item label="公共资源" prop="isPublic">
      <div class="form-item-with-tooltip">
        <el-switch v-model="spaceForm.isPublic" />
        <el-tooltip content="关闭后仅向对应部门的员工开放预约权限" placement="right" :show-after="1000">
          <el-icon class="info-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </div>
    </el-form-item>

    <!-- 所属部门（仅在公共资源关闭时显示） -->
    <el-form-item 
      v-if="!spaceForm.isPublic" 
      label="所属部门" 
      prop="departments"
    >
      <el-select 
        v-model="spaceForm.departments" 
        placeholder="选择所属部门"
        multiple
        collapse-tags
        filterable
      >
        <el-option 
          v-for="dept in departments" 
          :key="dept.dep_id" 
          :label="dept.dep_name" 
          :value="dept.dep_id"
        />
      </el-select>
    </el-form-item>

    <!-- 归还检查 -->
    <el-form-item label="归还检查" prop="needCheck">
      <div class="form-item-with-tooltip">
        <el-switch v-model="spaceForm.needCheck" />
        <el-tooltip
          content="员工归还管资源后将提醒设备检查员，检查完毕前空间将处于不可预约状态"
          placement="right" 
          :show-after="1000" 
          :dangerouslyUseHTMLString="true"
        >
          <el-icon class="info-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </div>
    </el-form-item>

    <!-- 备注 -->
    <el-form-item label="备注" prop="remark">
      <el-input 
        v-model="spaceForm.remark" 
        type="textarea" 
        :rows="3" 
        placeholder="输入备注信息(选填)"
        resize="vertical"
      />
    </el-form-item>

    <!-- 操作按钮 -->
    <el-form-item class="form-actions" style="margin-bottom: 0">
      <el-button type="primary" @click="submitForm">提交</el-button>
      <el-button @click="resetForm">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 定义emit事件
const emit = defineEmits(['success', 'cancel'])

// 表单引用
const spaceFormRef = ref()

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 部门列表
const departments = computed(() => publicInfoStore.departments)

// 表单数据
const spaceForm = reactive({
  name: '',
  location: '',
  type: '',
  tag: '', // 空间标签
  capacity: null,
  isPublic: true, // 默认开启公共资源
  departments: [], // 所属部门
  needCheck: false, // 默认关闭归还检查
  remark: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入空间名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  location: [
    { required: true, message: '请输入空间位置', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请输入空间类型', trigger: 'change' }
  ],
  tag: [
    { required: true, message: '请选择空间标签', trigger: 'change' }
  ],
  capacity: [
    { required: true, message: '请输入容纳人数', trigger: 'change' },
    { type: 'number', min: 1, message: '容纳人数必须大于0', trigger: 'change' }
  ],
  departments: [
    { 
      required: () => !spaceForm.isPublic, 
      message: '请选择所属部门', 
      trigger: 'change' 
    }
  ]
}

// 重置表单
const resetForm = () => {
  // 重置所有字段
  spaceForm.name = ''
  spaceForm.location = ''
  spaceForm.type = ''
  spaceForm.tag = ''
  spaceForm.capacity = null
  spaceForm.isPublic = true
  spaceForm.departments = []
  spaceForm.needCheck = false
  spaceForm.remark = ''
  
  // 如果表单引用存在且有resetFields方法，调用它来重置验证状态
  if (spaceFormRef.value && typeof spaceFormRef.value.resetFields === 'function') {
    spaceFormRef.value.resetFields()
  }
}

// 组件挂载时重置表单
onMounted(() => {
  resetForm()
})

// 提交表单
const submitForm = async () => {
  if (!spaceFormRef.value) return
  await spaceFormRef.value.validate((valid) => {
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
      const data = {
        space_name: spaceForm.name,
        location: spaceForm.location,
        type: spaceForm.type,
        tag: spaceForm.tag,
        capacity: spaceForm.capacity,
        public: spaceForm.isPublic ? 1 : 0, // 转换为0/1格式
        check: spaceForm.needCheck ? 1 : 0, // 转换为0/1格式
        note: spaceForm.remark
      }
      
      // 如果不是公共资源，添加部门ID
      if (!spaceForm.isPublic && spaceForm.departments.length > 0) {
        // 将部门ID数组转换为逗号分隔的字符串
        data.dep_id = spaceForm.departments.join(',')
      }
      
      const requestData = {
        token: token,
        data: data
      }
      
      // 发送请求
      fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/create-resource`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json;charset=UTF-8'
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
          ElMessage.success('添加空间成功')
          // 通知父组件
          emit('success')
        } else {
          ElMessage.error(data.message || '添加空间失败')
        }
      })
      .catch(error => {
        console.error('添加空间失败:', error)
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
.space-form {
  padding: 0 20px;
  width: calc(100% - 40px);
}

.form-actions {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.form-item-with-tooltip {
  display: flex;
  align-items: center;
}

.info-icon {
  margin-left: 10px;
  color: rgba(63, 81, 181, 0.3);
  font-size: 16px;
  cursor: help;
}

.info-icon:hover {
  color: rgba(63, 81, 181, 0.7);
}

:deep(.el-form-item__label) {
  justify-content: left !important;
}

:deep(.el-button--primary) {
  margin-left: 0 !important;
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.is-focus) {
  box-shadow: #3f51b5 0 0 0 1px inset;
}

:deep(.el-select__wrapper.is-focused) {
  box-shadow: #3f51b5 0 0 0 1px inset;
}

:deep(.el-select-dropdown__item.is-selected:after) {
  background-color: #3f51b5 !important;
}

:deep(.el-input__wrapper.is-focused) {
  box-shadow: #3f51b5 0 0 0 1px inset;
}

:deep(.el-input-number__decrease:hover~.el-input:not(.is-disabled) .el-input__wrapper),
:deep(.el-input-number__increase:hover~.el-input:not(.is-disabled) .el-input__wrapper) {
  box-shadow: #3f51b5 0 0 0 1px inset;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  color: #3f51b5;
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}
</style>