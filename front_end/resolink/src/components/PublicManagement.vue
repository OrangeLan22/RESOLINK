<template>
  <div class="tab-container scrollbar">
    <!-- 无权限提示页面 -->
    <NoPermission v-if="!hasPermission" />
    
    <!-- 正常内容页面 -->
    <div v-else class="tab-main flex">
      <div class="info-card">
        <el-descriptions title="企业信息">
          <el-descriptions-item label="企业名称">{{ companyName }}</el-descriptions-item>
        </el-descriptions>
        <el-button type="primary" size="small" @click="handleEditCompany">编辑</el-button>
      </div>

      <div class="info-card">
        <el-descriptions title="部门信息" />
        <div class="departments">
          <template v-if="isLoading">
            <el-skeleton :rows="1" animated />
          </template>
          <template v-else-if="departments.length > 0">
            <button v-for="department in departments" :key="department.dep_id" class="department" @click="handleDepartmentClick(department)">
              {{ department.dep_name }}
            </button>
          </template>
          <template v-else>
            <div class="no-departments">
              暂无部门，通过"添加部门"创建部门及身份
            </div>
          </template>
        </div>
        <el-button type="primary" size="small" @click="handleAddDepartment">添加部门</el-button>
      </div>

      <div class="info-card">
        <el-descriptions title="权限列表" />
        <div class="departments">
          <template v-if="isLoading">
            <el-skeleton :rows="1" animated />
          </template>
          <template v-else>
            <button v-for="authority in authorities" :key="authority.id" class="department" @click="handlePermissionClick(authority)">
              {{ authority.tag }}
            </button>
          </template>
        </div>
        <el-button type="primary" size="small" @click="handleAddPermission">添加自定义权限</el-button>
      </div>
    </div>

    <!-- 弹窗容器 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        :width="dialogWidth"
        :show-close="true"
        :close-on-click-modal="false"
    >
      <!-- 弹窗内容区域，动态渲染不同组件 -->
      <div class="dialog-content">
        <component ref="currentComponentRef"
                   :is="currentComponent"
                   :department="currentDepartment"
                   :original-department="originalDepartment"
                   @success="handleComponentSuccess"
                   @update:departmentName="handleUpdateDepartmentName"
                   @update:statuses="handleUpdateStatuses"
        />
      </div>
      <template #footer>
      <span class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button v-if="!isSuperAdminPermission" type="primary" @click="handleSubmit">保存</el-button>
      </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, shallowRef, onMounted } from 'vue'
import { usePublicInfoStore } from '../stores/publicInfo'
import { ElMessage, ElMessageBox } from 'element-plus'
import NoPermission from './Common/NoPermission.vue'
import EditCompany from './PublicManagement/EditCompany.vue'
import AddPermission from './PublicManagement/AddPermission.vue'
import EditPermission from './PublicManagement/EditPermission.vue'
import AddDepartment from './PublicManagement/AddDepartment.vue'
import DepartmentStatuses from './PublicManagement/DepartmentStatuses.vue'

// 获取公共信息store实例
const publicInfoStore = usePublicInfoStore()

// 计算属性：从store中获取企业名称
const companyName = computed(() => {
  return publicInfoStore.publicData?.[0]?.companyName || '未获取到企业名称'
})

// 计算属性：从store中获取部门信息
const departments = computed(() => {
  return publicInfoStore.departments || []
})

// 计算属性：从store中获取权限列表
const authorities = computed(() => {
  return publicInfoStore.authorities || []
})

// 获取加载状态
const isLoading = computed(() => publicInfoStore.isLoading)

// 计算属性：是否有权限访问页面
const hasPermission = computed(() => publicInfoStore.$state.hasPermission)

// 计算属性：是否为高级管理员权限（id=99）
const isSuperAdminPermission = computed(() => {
  return currentDepartment.value?.id === 99
})

// 组件挂载时获取企业信息、部门信息和权限列表
onMounted(() => {
  publicInfoStore.fetchPublicInfo()
  publicInfoStore.fetchDepartmentsWithStatuses()
  publicInfoStore.fetchAuthorities()
})

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('')
const dialogWidth = ref('500px')
const currentComponent = shallowRef(null)
const currentComponentRef = ref(null)

// 打开弹窗方法
const openDialog = (title, component, width = '500px', department = null) => {
  dialogTitle.value = title
  dialogWidth.value = width
  currentComponent.value = component
  
  // 如果是编辑部门，保存原始数据的副本
  if (department) {
    currentDepartment.value = JSON.parse(JSON.stringify(department))
    originalDepartment.value = JSON.parse(JSON.stringify(department))
  } else {
    currentDepartment.value = department
    originalDepartment.value = null
  }
  
  dialogVisible.value = true
}

// 当前选中的部门
const currentDepartment = ref(null)

// 保存原始部门数据的副本
const originalDepartment = ref(null)

// 检查是否有未保存的修改
const hasUnsavedChanges = () => {
  if (!originalDepartment.value || !currentDepartment.value) {
    return false
  }
  
  // 深拷贝并比较原始数据和当前数据
  const original = JSON.parse(JSON.stringify(originalDepartment.value))
  const current = JSON.parse(JSON.stringify(currentDepartment.value))
  
  return JSON.stringify(original) !== JSON.stringify(current)
}

// 关闭弹窗方法
const closeDialog = async () => {
  // 检查是否有未保存的修改
  if (hasUnsavedChanges()) {
    try {
      await ElMessageBox.confirm('确定要放弃修改？', '暂未保存', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonType: 'danger',
        cancelButtonType: 'default'
      })
      
      // 用户点击确定，关闭弹窗并恢复数据
      dialogVisible.value = false
      
      // 如果有原始部门数据，恢复数据
      if (originalDepartment.value) {
        currentDepartment.value = JSON.parse(JSON.stringify(originalDepartment.value))
        originalDepartment.value = null
      }
      
      setTimeout(() => {
        currentComponent.value = null
      }, 300)
    } catch (error) {
      // 用户点击取消，不执行任何操作
      return
    }
  } else {
    // 没有未保存的修改，直接关闭弹窗
    dialogVisible.value = false
    
    // 如果有原始部门数据，恢复数据
    if (originalDepartment.value) {
      originalDepartment.value = null
    }
    
    setTimeout(() => {
      currentComponent.value = null
    }, 300)
  }
}

// 组件成功事件处理
const handleComponentSuccess = () => {
  // 保存成功，清除原始部门数据
  originalDepartment.value = null
  closeDialog()
  // 重新获取部门信息
  publicInfoStore.fetchDepartmentsWithStatuses()
}

// 处理部门名称更新
const handleUpdateDepartmentName = (newName) => {
  if (currentDepartment.value) {
    currentDepartment.value.dep_name = newName
  }
}

// 处理身份标签更新
const handleUpdateStatuses = (newStatuses) => {
  if (currentDepartment.value) {
    currentDepartment.value.statuses = newStatuses
  }
}

// 提交按钮点击事件
const handleSubmit = () => {
  if (currentComponentRef.value && typeof currentComponentRef.value.submitEditForm === 'function') {
    currentComponentRef.value.submitEditForm()
  }
}

// 编辑企业信息按钮点击事件
const handleEditCompany = () => {
  openDialog('编辑企业信息', EditCompany)
}

// 添加自定义权限按钮点击事件
const handleAddPermission = () => {
  openDialog('添加自定义权限', AddPermission)
}

// 部门按钮点击事件
const handleDepartmentClick = (department) => {
  openDialog('编辑部门与身份', DepartmentStatuses, '500px', department)
}

// 权限按钮点击事件
const handlePermissionClick = (permission) => {
  openDialog('编辑权限', EditPermission, '500px', permission)
}

// 添加部门按钮点击事件
const handleAddDepartment = () => {
  openDialog('添加部门', AddDepartment)
}
</script>

<style scoped>
* {
  user-select: none;
}
.flex {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 40px;
}

.info-card {
  min-width: 200px;
  width: fit-content;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
  transition: box-shadow 0.3s;
  align-self: flex-start; /* 让高度根据内部元素自适应 */
}
.info-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-button--small) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}
:deep(.el-button--small:active) {
  transform: scale(0.95);
}

:deep(.el-descriptions__cell) {
  display: flex;
  flex-wrap: wrap;
}

.departments {
  padding: 0 0 10px 0;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
}

.department {
  white-space: nowrap;
  cursor: pointer;
  padding: 5px 11px;
  border-radius: 4px;
  border: 0;
  background-color: transparent;
  color: #424242;
  transition: all 0.1s;
}
.department:hover {
  box-shadow: 0 1px 10px 1px #dbdbdb;
}

.no-departments {
  color: #999;
  font-size: 14px;
  margin-bottom: 10px;
}

.add-department-link {
  color: #3f51b5;
  cursor: pointer;
  text-decoration: underline;
}

.add-department-link:hover {
  color: #5c6bc0;
}

:deep(.el-dialog__footer) {
  padding-top: 0;
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

</style>