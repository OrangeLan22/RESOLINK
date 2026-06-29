<template>
  <div class="top">
    <div class="search-and-filter">
      <div class="search">
        <div class="search-container">
          <input type="text" v-model="searchKeyword" placeholder="查找设备">
          <button @click="handleSearch">搜索</button>
        </div>
      </div>
      
      <!-- 标签过滤器 -->
      <div class="filter-tags">
        <span class="filter-label">类型：</span>
        <el-tag
          v-for="tag in filterTags"
          :key="tag.value"
          :class="{ 'active': selectedTag === tag.value }"
          @click="selectTag(tag.value)"
        >
          {{ tag.label }}
        </el-tag>
      </div>
    </div>
    <button class="add" @click="handleAddPhysical">
      添加设备
    </button>
  </div>

  <div class="resources-list">
    <el-empty v-if="error === '权限错误'" description="无权限访问"></el-empty>
    <el-empty v-else-if="filteredResources.length === 0 && !loading" description="暂无实物资源"></el-empty>

    <div class="resources-cards" v-else>
      <div class="resource-card" v-if="loading" style="width: fit-content">
        <el-skeleton animated :rows="3">
          <el-skeleton-item variant="text" style="width: 100px" />
        </el-skeleton>
      </div>

      <div class="resource-card" v-for="resource in filteredResources" :key="resource.id" v-else>
        <div class="title">
          <p>
            {{ resource.equipmentName }}
            <span class="id-tag">#{{ resource.id }}</span>
          </p>
          <span :class="['type', resource.stage === 0 ? 'idle' : 'in-use']">
            {{ resource.stage === 0 ? '可用' : '不可用' }}
          </span>
        </div>

        <div class="info">
          <span>{{ resource.type }}</span>
          <el-divider direction="vertical"></el-divider>
          <span>{{ resource.location }}</span>
        </div>

        <div class="details">
          {{ resource.note || '无备注' }}
        </div>

        <div class="tags">
          <el-tag v-if="resource.publicFlag === 1" size="small" type="success">公共资源</el-tag>
          <el-tag v-else size="small">部门专属</el-tag>
          <el-tag v-if="resource.checkFlag === 1" size="small" type="warning">归还检查</el-tag>
        </div>

        <el-button type="primary" size="small" @click="handleEditPhysical(resource)">
          编辑
        </el-button>
      </div>
    </div>
  </div>

  <!-- 添加/编辑设备弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="60%"
    :show-close="true"
    :close-on-click-modal="false"
    style="max-width: 600px;"
    @close="handleDialogClose"
  >
    <!-- 根据模式显示不同的组件 -->
    <AddPhysicalResource 
      ref="addPhysicalRef"
      v-if="dialogTitle === '添加设备'"
      @success="handleDialogSuccess" 
    />
    <EditPhysicalResource 
      ref="editPhysicalRef"
      v-else-if="editData"
      :edit-data="editData"
      @success="handleDialogSuccess" 
      @cancel="closeDialog" 
      @close="handleEditClose"
      @delete-success="handleDeleteSuccess"
    />
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElDialog, ElMessage, ElSkeleton, ElDivider, ElTag, ElSkeletonItem } from 'element-plus'
import AddPhysicalResource from './AddPhysicalResource.vue'
import EditPhysicalResource from './EditPhysicalResource.vue'
import { useResourcesStore } from '../../stores/resources'

// 状态管理
const resourcesStore = useResourcesStore()
const physicalResources = computed(() => resourcesStore.getPhysicalResources)
const loading = computed(() => resourcesStore.isLoading)
const error = computed(() => resourcesStore.error)

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加设备')
const editData = ref(null)

// 组件引用
const addPhysicalRef = ref(null)
const editPhysicalRef = ref(null)

// 搜索相关
const searchKeyword = ref('')

// 标签过滤器相关
const filterTags = [
  { label: '全部', value: '' },
  { label: '办公设备', value: '办公设备' },
  { label: '办公耗材', value: '办公耗材' },
  { label: '交通出行', value: '交通出行' }
]

const selectedTag = ref('')

// 过滤后的资源列表
const filteredResources = computed(() => {
  let result = physicalResources.value
  
  // 按标签过滤（匹配tag字段）
  if (selectedTag.value) {
    result = result.filter(resource => resource.tag === selectedTag.value)
  }
  
  // 按搜索关键词过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(resource => 
      resource.equipmentName.toLowerCase().includes(keyword) ||
      resource.location.toLowerCase().includes(keyword)
    )
  }
  
  return result
})

// 组件加载时获取物理资源数据
onMounted(async () => {
  try {
    await resourcesStore.fetchPhysicalResources()
  } catch (err) {
    if (err.message === '权限错误') {
      ElMessage.error('无权限访问物理资源')
    } else {
      ElMessage.error('获取物理资源失败')
    }
  }
})

// 打开添加设备弹窗
const handleAddPhysical = () => {
  // 如果弹窗已经打开，先关闭它
  if (dialogVisible.value) {
    dialogVisible.value = false
    // 等待弹窗关闭动画完成后再重新打开
    setTimeout(() => {
      dialogTitle.value = '添加设备'
      editData.value = null
      dialogVisible.value = true
      // 重置表单
      setTimeout(() => {
        if (addPhysicalRef.value) {
          addPhysicalRef.value.resetForm()
        }
      }, 100)
    }, 300) // 与弹窗动画时间一致
  } else {
    // 弹窗未打开，直接打开
    dialogTitle.value = '添加设备'
    editData.value = null
    dialogVisible.value = true
    // 重置表单
    setTimeout(() => {
      if (addPhysicalRef.value) {
        addPhysicalRef.value.resetForm()
      }
    }, 100)
  }
}

// 打开编辑设备弹窗
const handleEditPhysical = (resource) => {
  dialogTitle.value = '编辑设备'
  editData.value = resource
  dialogVisible.value = true
}

// 关闭弹窗
const closeDialog = () => {
  dialogVisible.value = false
  // 重置编辑数据
  setTimeout(() => {
    editData.value = null
  }, 300)
}

// 处理编辑组件的关闭事件
const handleEditClose = () => {
  closeDialog()
}

// 处理弹窗关闭事件
const handleDialogClose = () => {
  // 如果是编辑模式，需要触发二次确认
  if (dialogTitle.value === '编辑设备' && editData.value) {
    // 这里我们需要通过某种方式调用编辑组件的 handleClose 方法
    // 由于使用的是组合式 API，我们可以通过修改逻辑来实现
    // 简化处理：直接关闭弹窗，后续可以通过其他方式优化
    closeDialog()
  } else {
    // 其他模式直接关闭
    closeDialog()
  }
}

// 处理弹窗成功事件
const handleDialogSuccess = async () => {
  // 关闭弹窗
  closeDialog()
  // 刷新设备列表
  try {
    await resourcesStore.fetchPhysicalResources()
  } catch (err) {
    if (err.message === '权限错误') {
      ElMessage.error('无权限访问物理资源')
    } else {
      ElMessage.error('获取物理资源失败')
    }
  }
}

// 处理删除成功事件
const handleDeleteSuccess = async (id) => {
  // 关闭弹窗
  closeDialog()
  // 刷新设备列表
  try {
    await resourcesStore.fetchPhysicalResources()
  } catch (err) {
    if (err.message === '权限错误') {
      ElMessage.error('无权限访问物理资源')
    } else {
      ElMessage.error('获取物理资源失败')
    }
  }
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已经在computed属性中实现
  console.log('搜索:', searchKeyword.value)
}

// 选择标签过滤器
const selectTag = (tagValue) => {
  selectedTag.value = tagValue
}
</script>

<style scoped>
.top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  align-items: center;
}

.search-and-filter {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* 标签过滤器样式 */
.filter-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #666;
}

.filter-tags :deep(.el-tag) {
  cursor: pointer;
  transition: all 0.2s ease;
  padding: 4px 12px;
}

.filter-tags :deep(.el-tag:hover) {
  background-color: rgba(63, 81, 181, 0.2);
  color: #3f51b5;
}

.filter-tags :deep(.el-tag.active) {
  background-color: #3f51b5;
  color: #fff;
}

/* 骨架屏样式 */
:deep(.el-skeleton) {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 10px;
}

/* 卡片样式增强 */
.resource-card {
  position: relative;
}

.details {
  margin: 10px 0;
  font-size: 13px;
  color: #989898;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
}

.tags {
  margin: 10px 0;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tags :deep(.el-tag) {
  border-radius: 4px;
}

.add {
  flex-wrap: nowrap;
  white-space: nowrap;
  padding: 5px 10px;
  background-color: #3f51b5;
  height: 30px;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 10px;
}
.add:active {
  transform: scale(0.95);
}

.search-container {
  border: 1px solid #ccc;
  width: auto;
  height: 30px;
  border-radius: 50px;
  display: inline-flex;
  align-items: center;
  white-space: nowrap;
  transition: box-shadow 0.1s ease;
}

.search-container:focus-within {
  box-shadow: 0 0 0 1px #3f51b5 inset;
}

.search-container input {
  width: auto;
  min-width: 100px;
  height: 100%;
  background: none;
  border: none;
  outline: none;
  padding: 0 10px;
}

.search-container input::placeholder {
  position: relative;
  top: 1px;
}

.search-container button {
  width: auto;
  height: 100%;
  background: #3f51b5;
  color: white;
  border: none;
  border-radius: 40px;
  padding: 5px 15px;
  margin-left: 5px;
  cursor: pointer;
}

.search-container button:active {
  background: #303f9f;
}

.resources-list {
  padding: 10px 0 0 0;
}

.resources-cards {
  gap: 20px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  grid-auto-flow: dense;
}

.resource-card {
  width: 100%;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  transition: box-shadow 0.3s;
  box-sizing: border-box;
}
.resource-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  p {
    font-size: 22px;
    font-weight: bold;
    margin: 0;
    color: #333;
    .id-tag {
      font-size: 18px;
      font-weight: normal;
      color: #999;
      margin-left: 3px;
    }
  }
}

.type {
  position: relative;
  top: 6px;
  white-space: nowrap;
  width: fit-content;
  height: fit-content;
  padding: 2px 6px 3px 6px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
  background-color: #b1b1b1;
  margin-left: 10px;
}

.type.idle {
  background-color: #67c23a;
}

.type.in-use {
  background-color: #909399;
}

.info {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
  gap: 5px;
  span {
    font-size: 14px;
    margin: 0;
    color: #666;
  }
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}
:deep(.el-button--primary:active) {
  transform: scale(0.95);
}
</style>