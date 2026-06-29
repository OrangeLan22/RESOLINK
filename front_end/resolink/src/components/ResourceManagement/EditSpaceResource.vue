<template>
  <el-form
    ref="spaceFormRef"
    :model="spaceForm"
    :rules="rules"
    class="space-form"
    :hide-required-asterisk="true"
  >
    <!-- 空间名称 -->
    <div style="display: flex; gap: 8px;">
      <el-form-item label="空间名称" prop="name" style="flex: 1;">
        <el-input v-model="spaceForm.name" placeholder="输入空间名称" />
      </el-form-item>
      <el-button
          type="danger"
          @click="handleDelete"
      >
        删除资源
      </el-button>
    </div>

    <!-- 空间位置 -->
    <el-form-item label="空间位置" prop="location">
      <el-input v-model="spaceForm.location" placeholder="输入空间位置" />
    </el-form-item>

    <!-- 空间类型 -->
    <el-form-item label="空间类型" prop="type">
      <el-input v-model="spaceForm.type" placeholder="输入空间类型" />
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
      <el-button type="primary" @click="submitForm">确定</el-button>
      <el-button @click="handleCancel">取消</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElLoading, ElMessageBox } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { usePublicInfoStore } from '../../stores/publicInfo'

// 定义props
const props = defineProps({
  editData: {
    type: Object,
    default: null
  }
})

// 定义emit事件
const emit = defineEmits(['success', 'cancel', 'delete-success'])

// 表单引用
const spaceFormRef = ref()

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 部门列表
const departments = computed(() => publicInfoStore.departments)

// 表单数据
const spaceForm = reactive({
  id: 0,
  name: '',
  location: '',
  type: '',
  capacity: null,
  isPublic: true, // 默认开启公共资源
  departments: [], // 所属部门
  needCheck: false, // 默认关闭归还检查
  remark: ''
})

// 组件挂载时初始化表单数据
onMounted(() => {
  if (props.editData) {
    spaceForm.id = props.editData.id
    spaceForm.name = props.editData.spaceName
    spaceForm.location = props.editData.location
    spaceForm.type = props.editData.type
    spaceForm.capacity = props.editData.capacity
    spaceForm.isPublic = props.editData.publicFlag === 1
    spaceForm.departments = props.editData.depId ? props.editData.depId.split(',').map(Number) : []
    spaceForm.needCheck = props.editData.checkFlag === 1
    spaceForm.remark = props.editData.note || ''
  }
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

// 取消处理
const handleCancel = async () => {
  const hasChanges = checkFormChanges();
  if (hasChanges) {
    try {
      await ElMessageBox.confirm('确定要放弃修改？', '暂未保存', {
        confirmButtonText: '确定',
        confirmButtonType: 'danger',
        cancelButtonText: '取消',
        type: 'warning'
      });
      emit('cancel');
    } catch (error) {
      // 用户取消操作，不做任何处理
    }
  } else {
    emit('cancel');
  }
}

// 检查表单是否有修改
const checkFormChanges = () => {
  if (!props.editData) return false
  
  return (
    spaceForm.name !== props.editData.spaceName ||
    spaceForm.location !== props.editData.location ||
    spaceForm.type !== props.editData.type ||
    spaceForm.capacity !== props.editData.capacity ||
    (spaceForm.isPublic ? 1 : 0) !== props.editData.publicFlag ||
    (spaceForm.needCheck ? 1 : 0) !== props.editData.checkFlag ||
    spaceForm.remark !== (props.editData.note || '') ||
    JSON.stringify(spaceForm.departments.sort()) !== JSON.stringify((props.editData.depId ? props.editData.depId.split(',').map(Number) : []).sort())
  )
}

// 提交表单
const submitForm = async () => {
  if (!spaceFormRef.value) return
  await spaceFormRef.value.validate((valid) => {
    if (valid) {
      // 检查是否有实际修改
      if (!checkFormChanges()) {
        // 没有修改，直接关闭弹窗
        emit('cancel')
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
      const data = {
        space_name: spaceForm.name,
        location: spaceForm.location,
        type: spaceForm.type,
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
      
      // 添加ID
      data.id = spaceForm.id
      
      // 确保不包含stage字段
      delete data.stage
      
      const requestData = {
        token: token,
        data: data
      }
      
      // 发送请求
      fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/update-resource`, {
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
          ElMessage.success('编辑空间成功')
          // 通知父组件
          emit('success')
        } else {
          ElMessage.error(data.message || '编辑空间失败')
        }
      })
      .catch(error => {
        console.error('编辑空间失败:', error)
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
        id: spaceForm.id
      }
    }
    
    // 发送请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/delete-resource`, {
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
      emit('delete-success', spaceForm.id)
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