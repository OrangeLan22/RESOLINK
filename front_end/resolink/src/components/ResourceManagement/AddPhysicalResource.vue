<template>
  <el-form
    ref="physicalFormRef"
    :model="physicalForm"
    :rules="rules"
    class="physical-form"
    :hide-required-asterisk="true"
  >
    <!-- 设备名称 -->
    <el-form-item label="设备名称" prop="name">
      <el-input v-model="physicalForm.name" placeholder="输入设备名称" />
    </el-form-item>

    <!-- 批量创建 -->
    <el-form-item label="批量添加" prop="batchCreate">
      <div class="form-item-with-tooltip">
        <el-switch v-model="physicalForm.batchCreate" />
        <el-tooltip content="添加多个同种设备" placement="right" :show-after="1000">
          <el-icon class="info-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </div>
    </el-form-item>

    <!-- 设备数量（仅在批量创建开启时显示） -->
    <el-form-item 
      v-if="physicalForm.batchCreate" 
      label="设备数量" 
      prop="quantity"
    >
      <el-input-number 
        v-model="physicalForm.quantity" 
        :min="2" 
        :max="100" 
        placeholder="2~100"
      />
    </el-form-item>

    <!-- 存放位置 -->
    <el-form-item label="存放位置" prop="location">
      <el-input v-model="physicalForm.location" placeholder="输入设备位置" />
    </el-form-item>

    <!-- 设备类型 -->
    <el-form-item label="设备类型" prop="type">
      <el-input v-model="physicalForm.type" placeholder="输入设备类型" />
    </el-form-item>

    <!-- 设备标签（必填） -->
    <el-form-item label="设备标签" prop="tag">
      <el-select v-model="physicalForm.tag" placeholder="请选择设备标签">
        <el-option label="办公设备" value="办公设备" />
        <el-option label="办公耗材" value="办公耗材" />
        <el-option label="交通出行" value="交通出行" />
      </el-select>
    </el-form-item>

    <!-- 公共资源 -->
    <el-form-item label="公共资源" prop="isPublic">
      <div class="form-item-with-tooltip">
        <el-switch v-model="physicalForm.isPublic" />
        <el-tooltip content="关闭后仅向对应部门的员工开放预约权限" placement="right" :show-after="1000">
          <el-icon class="info-icon"><QuestionFilled /></el-icon>
        </el-tooltip>
      </div>
    </el-form-item>

    <!-- 所属部门（仅在公共资源关闭时显示） -->
    <el-form-item 
      v-if="!physicalForm.isPublic" 
      label="所属部门" 
      prop="departments"
    >
      <el-select 
        v-model="physicalForm.departments" 
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
        <el-switch v-model="physicalForm.needCheck" />
        <el-tooltip
          content="员工归还管资源后将提醒设备检查员，检查完毕前设备将处于不可预约状态"
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
        v-model="physicalForm.remark" 
        type="textarea" 
        :rows="3" 
        placeholder="输入备注信息(选填)"
        resize="vertical"
      />
    </el-form-item>

    <!-- 操作按钮 -->
    <el-form-item class="form-actions" style="margin-bottom: 0">
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="handleReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElLoading } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 定义emit事件
const emit = defineEmits(['success'])

// 表单引用
const physicalFormRef = ref()

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 部门列表
const departments = computed(() => publicInfoStore.departments)

// 表单数据
const physicalForm = reactive({
  name: '',
  model: '',
  batchCreate: false, // 默认关闭批量创建
  quantity: 2, // 默认批量创建数量
  location: '',
  type: '',
  tag: '', // 设备标签
  isPublic: true, // 默认开启公共资源
  departments: [], // 所属部门
  needCheck: false, // 默认关闭归还检查
  remark: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入设备名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  batchCreate: [],
  quantity: [
    { 
      required: () => physicalForm.batchCreate, 
      message: '请输入设备数量', 
      trigger: 'change' 
    },
    { 
      type: 'number', 
      min: 2, 
      max: 100, 
      message: '设备数量必须在 2 到 100 之间', 
      trigger: 'change' 
    }
  ],
  model: [
    { required: true, message: '请输入设备型号', trigger: 'blur' },
    { min: 1, max: 50, message: '长度在 1 到 50 个字符', trigger: 'blur' }
  ],
  location: [
    { required: true, message: '请输入设备位置', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请输入设备类型', trigger: 'change' }
  ],
  tag: [
    { required: true, message: '请选择设备标签', trigger: 'change' }
  ],
  departments: [
    { 
      required: () => !physicalForm.isPublic, 
      message: '请选择所属部门', 
      trigger: 'change' 
    }
  ]
}

// 重置处理
const handleReset = () => {
  resetForm()
}

// 重置表单到初始状态
const resetForm = () => {
  if (physicalFormRef.value) {
    physicalFormRef.value.resetFields()
  }
  // 重置表单数据到默认值
  physicalForm.name = ''
  physicalForm.location = ''
  physicalForm.type = ''
  physicalForm.tag = ''
  physicalForm.isPublic = true
  physicalForm.departments = []
  physicalForm.needCheck = false
  physicalForm.remark = ''
  physicalForm.batchCreate = false
  physicalForm.quantity = 1
}

// 暴露重置方法供父组件调用
defineExpose({
  resetForm
})

// 提交表单
const submitForm = async () => {
  if (!physicalFormRef.value) return
  await physicalFormRef.value.validate((valid) => {
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
      let requestData = {
        token: token,
        data: []
      }
      
      // 检查是否是批量创建
      if (physicalForm.batchCreate) {
        // 批量创建：生成多个设备数据
        for (let i = 0; i < physicalForm.quantity; i++) {
          const deviceData = {
            equipment_name: physicalForm.name,
            location: physicalForm.location,
            type: physicalForm.type,
            tag: physicalForm.tag,
            public: physicalForm.isPublic ? 1 : 0, // 转换为0/1格式
            check: physicalForm.needCheck ? 1 : 0, // 转换为0/1格式
            note: physicalForm.remark
          }
          
          // 如果不是公共资源，添加部门ID
          if (!physicalForm.isPublic && physicalForm.departments.length > 0) {
            deviceData.dep_id = physicalForm.departments.join(',') // 使用英文逗号分隔
          }
          
          requestData.data.push(deviceData)
        }
      } else {
        // 单个创建：单个设备数据
        requestData.data = {
          equipment_name: physicalForm.name,
          location: physicalForm.location,
          type: physicalForm.type,
          tag: physicalForm.tag,
          public: physicalForm.isPublic ? 1 : 0, // 转换为0/1格式
          check: physicalForm.needCheck ? 1 : 0, // 转换为0/1格式
          note: physicalForm.remark
        }
        
        // 如果不是公共资源，添加部门ID
        if (!physicalForm.isPublic && physicalForm.departments.length > 0) {
          requestData.data.dep_id = physicalForm.departments.join(',') // 使用英文逗号分隔
        }
      }
      
      // 发送请求
      fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/create-physical`, {
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
          ElMessage.success('添加设备成功')
          // 通知父组件更新列表
          emit('success')
        } else {
          ElMessage.error(data.message || '添加设备失败')
        }
      })
      .catch(error => {
        console.error('添加设备失败:', error)
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
.physical-form {
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

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.el-input-number__decrease:hover~.el-input:not(.is-disabled) .el-input__wrapper),
:deep(.el-input-number__increase:hover~.el-input:not(.is-disabled) .el-input__wrapper) {
  box-shadow: #3f51b5 0 0 0 1px inset;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  color: #3f51b5;
}
</style>