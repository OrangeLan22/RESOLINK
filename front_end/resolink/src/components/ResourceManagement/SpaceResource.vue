<template>
  <div class="top">
    <div class="search-and-filter">
      <div class="search">
        <div class="search-container">
          <input type="text" v-model="searchKeyword" placeholder="查找空间资源">
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
    <button class="add" @click="handleAddSpace">
      添加空间
    </button>
  </div>

  <div class="resources-list">
    <el-empty v-if="filteredResources.length === 0 && !isLoading" description="暂无空间资源"></el-empty>

    <div class="resources-cards" v-else>
      <div class="resource-card" v-if="isLoading" style="width: fit-content">
        <el-skeleton animated :rows="3">
          <el-skeleton-item variant="text" style="width: 100px" />
        </el-skeleton>
      </div>

      <div class="resource-card" v-for="resource in filteredResources" :key="resource.id" v-else>
        <div class="title">
          <p>
            {{ resource.spaceName }}
            <span class="id-tag">#{{ resource.id }}</span>
          </p>
          <span :class="['type', resource.stage === 0 ? 'idle' : resource.stage === 1 ? 'in-use' : 'unavailable']">
            {{ resource.stage === 0 ? '可用' : resource.stage === 1 ? '使用中' : '不可用' }}
          </span>
        </div>

        <div class="info">
          <span>{{ resource.type }}</span>
          <el-divider direction="vertical"></el-divider>
          <span>{{ resource.location }}</span>
          <el-divider direction="vertical"></el-divider>
          <span>{{ resource.capacity }}人</span>
        </div>

        <div class="details">
          {{ resource.note || '无备注' }}
        </div>

        <div class="tags">
          <el-tag v-if="resource.publicFlag === 1" size="small" type="success">公共资源</el-tag>
          <el-tag v-else size="small">部门专属</el-tag>
          <el-tag v-if="resource.checkFlag === 1" size="small" type="warning">归还检查</el-tag>
        </div>

        <el-button type="primary" size="small" @click="handleEditSpace(resource)">
          编辑
        </el-button>
      </div>
    </div>
  </div>

  
  <!-- 添加/编辑空间弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="60%"
    :show-close="true"
    :close-on-click-modal="false"
    style="max-width: 600px;"
    @close="closeDialog"
  >
    <!-- 根据模式显示不同的组件 -->
    <AddSpaceResource 
      v-if="dialogTitle === '添加空间'"
      :key="addSpaceKey"
      @success="handleDialogSuccess" 
      @cancel="closeDialog" 
    />
    <EditSpaceResource 
      v-else-if="editData"
      :edit-data="editData"
      @success="handleDialogSuccess" 
      @cancel="closeDialog" 
      @delete-success="handleDeleteSuccess"
    />
  </el-dialog>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import {ElDialog, ElMessage, ElSkeleton, ElDivider, ElTag, ElSkeletonItem} from 'element-plus'
import AddSpaceResource from './AddSpaceResource.vue'
import EditSpaceResource from './EditSpaceResource.vue'
import { useResourcesStore } from '../../stores/resources'

// 获取资源store实例
const resourcesStore = useResourcesStore()

// 弹窗相关
const dialogVisible = ref(false)
const dialogTitle = ref('添加空间')
const editData = ref(null)
const addSpaceKey = ref(0) // 用于强制重新渲染添加空间组件
const currentComponent = ref(null) // 跟踪当前显示的组件

// 搜索相关
const searchKeyword = ref('')

// 标签过滤器相关
const filterTags = [
  { label: '全部', value: '' },
  { label: '核心业务', value: '核心业务' },
  { label: '公共辅助', value: '公共辅助' },
  { label: '特殊功能', value: '特殊功能' }
]

const selectedTag = ref('')

// 计算属性：过滤后的资源列表
const resources = computed(() => resourcesStore.resources)
const isLoading = computed(() => resourcesStore.isLoading)

// 过滤后的资源列表
const filteredResources = computed(() => {
  let result = resources.value
  
  // 按标签过滤（匹配tag字段）
  if (selectedTag.value) {
    result = result.filter(resource => resource.tag === selectedTag.value)
  }
  
  // 按搜索关键词过滤
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    result = result.filter(resource => 
      resource.spaceName.toLowerCase().includes(keyword) ||
      resource.location.toLowerCase().includes(keyword)
    )
  }
  
  return result
})

// 组件挂载时获取资源列表
onMounted(() => {
  fetchResources()
})

// 获取资源列表
const fetchResources = async () => {
  try {
    await resourcesStore.fetchResources()
  } catch (error) {
    ElMessage.error('获取资源列表失败')
    console.error('获取资源列表失败:', error)
  }
}

// 打开添加空间弹窗
const handleAddSpace = () => {
  // 如果弹窗已经打开（例如从编辑模式切换），先关闭它
  if (dialogVisible.value) {
    dialogVisible.value = false
    // 等待弹窗关闭动画完成后再重新打开
    setTimeout(() => {
      dialogTitle.value = '添加空间'
      editData.value = null
      addSpaceKey.value++ // 更新key，强制重新渲染组件
      dialogVisible.value = true
    }, 300) // 与弹窗动画时间一致
  } else {
    // 弹窗未打开，直接打开
    dialogTitle.value = '添加空间'
    editData.value = null
    addSpaceKey.value++ // 更新key，强制重新渲染组件
    dialogVisible.value = true
  }
}

// 打开编辑空间弹窗
const handleEditSpace = (resource) => {
  dialogTitle.value = '编辑空间'
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

// 处理弹窗成功事件
const handleDialogSuccess = () => {
  // 关闭弹窗
  closeDialog()
  // 刷新资源列表
  fetchResources()
}

// 处理删除成功事件
const handleDeleteSuccess = (resourceId) => {
  // 从pinia数据中删除对应的资源
  resourcesStore.resources = resourcesStore.resources.filter(resource => resource.id !== resourceId)
  // 关闭弹窗
  closeDialog()
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
  background-color: #f56c6c;
}

.type.unavailable {
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