<template>
  <el-form
    ref="physicalFormRef"
    :model="physicalForm"
    :rules="rules"
    class="physical-form"
    :hide-required-asterisk="true"
  >
    <!-- 设备名称 -->
    <div style="display: flex; gap: 8px;">
      <el-form-item label="设备名称" prop="name" style="flex: 1;">
        <el-input v-model="physicalForm.name" placeholder="输入设备名称" />
      </el-form-item>
      <el-button
          type="danger"
          @click="handleDelete"
      >
        删除资源
      </el-button>
    </div>

    <!-- 存放位置 -->
    <el-form-item label="存放位置" prop="location">
      <el-input v-model="physicalForm.location" placeholder="输入设备位置" />
    </el-form-item>

    <!-- 设备类型 -->
    <el-form-item label="设备类型" prop="type">
      <el-input v-model="physicalForm.type" placeholder="输入设备类型" />
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
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElLoading, ElMessageBox } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 定义props
const props = defineProps({
  editData: {
    type: Object,
    required: true
  }
})

// 定义emit事件
const emit = defineEmits(['success', 'cancel', 'close', 'delete-success'])

// 表单引用
const physicalFormRef = ref()

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 部门列表
const departments = computed(() => publicInfoStore.departments)

// 表单数据
const physicalForm = reactive({
  id: '',
  name: '',
  location: '',
  type: '',
  isPublic: true, // 默认开启公共资源
  departments: [], // 所属部门
  needCheck: false, // 默认关闭归还检查
  remark: ''
})

// 初始表单数据，用于比较是否有修改
const initialFormData = ref({})

// 表单是否被修改
const formModified = ref(false)

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入设备名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  location: [
    { required: true, message: '请输入设备位置', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请输入设备类型', trigger: 'change' }
  ],
  departments: [
    { 
      required: () => !physicalForm.isPublic, 
      message: '请选择所属部门', 
      trigger: 'change' 
    }
  ]
}

// 组件挂载时，将编辑数据填充到表单
onMounted(() => {
  if (props.editData) {
    physicalForm.id = props.editData.id
    physicalForm.name = props.editData.equipmentName
    physicalForm.location = props.editData.location
    physicalForm.type = props.editData.type
    physicalForm.isPublic = props.editData.publicFlag === 1
    physicalForm.needCheck = props.editData.checkFlag === 1
    physicalForm.remark = props.editData.note || ''
    // 处理所属部门
    physicalForm.departments = props.editData.depId ? props.editData.depId.split(',').map(Number) : []
    
    // 保存初始表单数据
    initialFormData.value = {
      ...physicalForm,
      departments: [...physicalForm.departments]
    }
  }
})

// 监听表单数据变化
watch(
  () => [
    physicalForm.name,
    physicalForm.location,
    physicalForm.type,
    physicalForm.isPublic,
    physicalForm.needCheck,
    physicalForm.remark,
    physicalForm.departments
  ],
  () => {
    // 比较表单数据是否有变化
    formModified.value = !(
      physicalForm.name === initialFormData.value.name &&
      physicalForm.location === initialFormData.value.location &&
      physicalForm.type === initialFormData.value.type &&
      physicalForm.isPublic === initialFormData.value.isPublic &&
      physicalForm.needCheck === initialFormData.value.needCheck &&
      physicalForm.remark === initialFormData.value.remark &&
      JSON.stringify(physicalForm.departments) === JSON.stringify(initialFormData.value.departments)
    )
  },
  { deep: true }
)

// 取消处理
const handleCancel = () => {
  // 如果表单有修改，显示二次确认弹窗
  if (formModified.value) {
    ElMessageBox.confirm('确定要放弃修改？', '暂未保存', {
      confirmButtonText: '确定',
      confirmButtonType: 'danger',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      emit('cancel')
    }).catch(() => {
      // 取消操作，不做任何处理
    })
  } else {
    // 表单没有修改，直接取消
    emit('cancel')
  }
}

// 提交表单
const submitForm = async () => {
  if (!physicalFormRef.value) return
  
  // 如果表单没有修改，直接关闭弹窗
  if (!formModified.value) {
    emit('cancel')
    return
  }
  
  await physicalFormRef.value.validate((valid) => {
    if (valid) {
      // 显示提交loading
      const loading = ElLoading.service({
        lock: true,
        text: ' ',
        background: 'rgba(0,0,0,0.3)'
      })
      
      // 获取token
      let token = localStorage.getItem('token') || ''
      token = token.replace(/^['"]|['"]$/g, '').trim()
      
      // 准备请求数据
      const requestData = {
        token: token,
        data: {
          id: physicalForm.id,
          equipment_name: physicalForm.name,
          location: physicalForm.location,
          type: physicalForm.type,
          public: physicalForm.isPublic ? 1 : 0, // 转换为0/1格式
          check: physicalForm.needCheck ? 1 : 0, // 转换为0/1格式
          note: physicalForm.remark
        }
      }
      
      // 如果不是公共资源，添加部门ID
      if (!physicalForm.isPublic && physicalForm.departments.length > 0) {
        requestData.data.dep_id = physicalForm.departments.join(',') // 使用英文逗号分隔
      }
      
      // 发送请求
      fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/update-physical`, {
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
          ElMessage.success('编辑设备成功')
          // 通知父组件更新列表
          emit('success')
        } else {
          ElMessage.error(data.message || '编辑设备失败')
        }
      })
      .catch(error => {
        console.error('编辑设备失败:', error)
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

// 删除资源
const handleDelete = async () => {
  try {
    // 显示确认对话框
    await ElMessageBox.confirm('确定要删除该资源吗？', '删除确认', {
      confirmButtonText: '确定',
      confirmButtonType: 'danger',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 显示整个页面的加载动画
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
        id: physicalForm.id
      }
    }
    
    // 发送请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/delete-physical`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8'
      },
      body: JSON.stringify(requestData)
    })
    
    if (response.status === 403) {
      ElMessage.error('权限错误')
      emit('cancel')
      return
    }
    
    const data = await response.json()
    
    if (data.success) {
      ElMessage.success('资源删除成功')
      // 通知父组件删除成功，以便更新pinia数据
      emit('delete-success', physicalForm.id)
    } else {
      ElMessage.error(data.message || '资源删除失败')
    }
    
    // 无论成功失败都关闭弹窗
    emit('cancel')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除资源失败:', error)
    }
  } finally {
    // 关闭加载动画
    ElLoading.service().close()
  }
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