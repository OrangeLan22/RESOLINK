<template xmlns="http://www.w3.org/1999/html">
  <div class="tab-container scrollbar">
    <div class="tab-main">
      <div class="tools-container flex">
        <div class="top-tools">

          <div class="search-container">
            <input type="text" v-model="searchKeyword" placeholder="通过设备名称查找">
            <button>搜索</button>
          </div>

          <div class="time-container">
            <span>预约时间</span>
            <el-date-picker
                v-model="reservationTime"
                type="datetime"
                placeholder="选择日期时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="height: 30px;"
                @change="handleTimeChange"
            />
          </div>

        </div>

        <div class="filter-container" @click.stop>
          <button class="filter-btn" @click="togglePopover">
            <el-icon style="font-size: 16px;"><Operation /></el-icon>
            过滤器
          </button>
          
          <div v-if="popoverVisible" class="custom-popover">
            <div class="filter-content">
              <div class="filter-item">
                <span>仅查看本部门资源</span>
                <el-switch v-model="filterOptions.deptOnly" default-active-value="true" />
              </div>
              <div class="filter-item">
                <span>仅查看有效资源</span>
                <el-switch v-model="filterOptions.validOnly" default-active-value="true" />
              </div>
              <div class="filter-actions">
                <el-button type="primary" @click="resetFilters">重置过滤器</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="resources-container">
        <div class="resource-list">
          <div v-if="isLoading" class="loading">
            <p>加载中...</p>
          </div>
          <div v-else-if="resources.length === 0" class="empty">
            <el-empty description="暂无可用资源"></el-empty>
          </div>
          <div v-else class="resources-cards">
            <div class="resource-card" v-for="resource in resources" :key="resource.id">
              <div class="title">
                <p>
                  {{ resource.name }}
                  <span class="id-tag">#{{ resource.id }}</span>
                </p>
                <span :class="['type', resource.stage === 0 ? 'idle' : resource.stage === 1 ? 'in-use' : 'unavailable']">
                  {{ resource.stage === 0 ? '可用' : resource.stage === 1 ? '可用' : '不可用' }}
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
                <el-tag v-if="resource.public === 1" size="small" type="success">公共资源</el-tag>
                <el-tag v-else size="small">部门专属</el-tag>
                <el-tag v-if="resource.check === 1" size="small" type="warning">归还检查</el-tag>
              </div>

              <div v-if="resource.max_available_minutes" class="available-time">
                <span>可预约时长：{{ formatDuration(resource.max_available_minutes) }}</span>
              </div>

              <el-button
                  v-if="resource.stage === 0"
                  :type="'primary'"
                  size="small"
                  @click="handleReservation(resource)"
              >
                预约
              </el-button>
              <button
                  v-else
                  class="disabled-btn"
                  :disabled="true"
                  @click="handleReservation(resource)"
              >
                不可预约
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 预约弹窗 -->
    <AssetReservation
      v-model:visible="reservationVisible"
      :resource="selectedResource"
      @success="handleReservationSuccess"
      @cancel="handleReservationCancel"
    />

    <!-- AI助手按钮 -->
    <div 
      class="ai-assistant-btn" 
      @click="showAIDrawer = true"
    >
      <el-icon><ChatDotRound /></el-icon>
      <span>AI助手</span>
    </div>

    <!-- AI助手抽屉 -->
    <el-drawer
      v-model="showAIDrawer"
      title="RESOLINK 智能体"
      direction="rtl"
      size="600px"
      :show-close="true"
      :withHeader="false"
    >
    <AiAgent></AiAgent>
    </el-drawer>
  </div>
</template>

<script setup>
import {ref, onMounted, defineProps} from "vue";
import {ElSwitch, ElButton, ElDatePicker, ElIcon, ElMessage, ElDivider, ElTag, ElDrawer, ElInput} from "element-plus";
import {Operation, ChatDotRound} from "@element-plus/icons-vue";
import AssetReservation from './AssetBooking/AssetReservation.vue';
import AiAgent from "./Agent/AiAgent.vue";

const props = defineProps({
  tagType: {
    type: String,
    default: ''
  }
})

const searchKeyword = ref('')
const filterOptions = ref({
  deptOnly: true,
  validOnly: true
})
const reservationTime = ref('')
const popoverVisible = ref(false)
const resources = ref([])
const isLoading = ref(false)
const reservationVisible = ref(false)
const selectedResource = ref(null)

// AI助手相关变量
const showAIDrawer = ref(false)
const aiMessage = ref('')
const aiMessages = ref([
  {
    type: 'ai',
    content: '您好！请问您需要什么样的设备？我可以根据您的需求帮您推荐合适的实物资产。'
  }
])

// 发送AI消息
const sendAIMessage = () => {
  if (!aiMessage.value.trim()) return
  
  // 添加用户消息
  aiMessages.value.push({
    type: 'user',
    content: aiMessage.value
  })
  
  // 模拟AI回复
  setTimeout(() => {
    aiMessages.value.push({
      type: 'ai',
      content: '收到您的消息！目前AI功能正在开发中，暂时无法提供智能回复。您可以手动浏览和选择适合的设备。'
    })
    
    // 滚动到底部
    setTimeout(() => {
      const chatContainer = document.querySelector('.chat-messages')
      if (chatContainer) {
        chatContainer.scrollTop = chatContainer.scrollHeight
      }
    }, 100)
  }, 500)
  
  aiMessage.value = ''
}

const togglePopover = () => {
  const wasVisible = popoverVisible.value
  popoverVisible.value = !popoverVisible.value
  // 只有在关闭过滤器时才更新API
  if (wasVisible && !popoverVisible.value) {
    fetchAvailableResources()
  }
}

const handleTimeChange = () => {
  // 当用户选择或删除预约时间时更新API
  fetchAvailableResources()
}

const handleReservation = (resource) => {
  selectedResource.value = resource
  reservationVisible.value = true
}

const handleReservationSuccess = () => {
  // 预约成功后的处理
  console.log('预约成功')
  reservationVisible.value = false
  selectedResource.value = null
}

const handleReservationCancel = () => {
  // 取消预约的处理
  reservationVisible.value = false
  selectedResource.value = null
}

const resetFilters = () => {
  filterOptions.value = {
    deptOnly: true,
    validOnly: true
  }
  // 重置后立即更新API
  fetchAvailableResources()
}

const formatDuration = (minutes) => {
  if (minutes === 99999) return '无限制'
  
  const days = Math.floor(minutes / (24 * 60))
  const hours = Math.floor((minutes % (24 * 60)) / 60)
  const mins = minutes % 60
  
  let result = ''
  if (days > 0) result += `${days}天`
  if (hours > 0) result += `${hours}时`
  if (mins > 0 || (days === 0 && hours === 0)) result += `${mins}分`
  
  return result
}

const fetchAvailableResources = async () => {
  isLoading.value = true
  try {
    let token = localStorage.getItem('token')
    // 去除可能的多余引号
    if (token) {
      token = token.replace(/^"|"$/g, '')
    }
    
    // 根据页面类型确定tag
    const tagMap = {
      'asset-device': '办公设备',
      'asset-supplies': '办公耗材',
      'asset-transport': '交通出行'
    }
    
    const data = {
      type: 'physical',
      islocal: filterOptions.value.deptOnly,
      iseffective: filterOptions.value.validOnly,
      tag: tagMap[props.tagType] || ''
    }
    
    if (reservationTime.value) {
      const timestamp = Math.floor(new Date(reservationTime.value).getTime() / 1000)
      data.start_time = timestamp.toString()
    }
    
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
    const response = await fetch(`${apiBaseUrl}/api/appointment/available-resources`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json;charset=UTF-8'
      },
      body: JSON.stringify({token, data})
    })
    
    const result = await response.json()
    
    if (result.success) {
      resources.value = result.data
    } else if (response.status === 403) {
      ElMessage.error('没有权限获取资源')
    } else {
      ElMessage.error(result.message || '获取资源失败')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后重试')
    console.error('获取资源失败:', error)
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时获取资源
onMounted(() => {
  fetchAvailableResources()
})


</script>

<style scoped>
* {
  user-select: none;
}

.flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tools-container {
  margin-bottom: 20px;
}

.resources-container {
  width: 100%;
}

.loading,
.empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
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

.available-time {
  margin: 10px 0;
  font-size: 13px;
  color: #3f51b5;
  font-weight: 500;
}

.top-tools {
  display: flex;
  align-items: center;
}

.time-container {
  display: flex;
  align-items: center;
  margin-left: 20px;
}

.time-container span {
  margin-right: 10px;
  font-size: 14px;
  color: #333333;
}

.filter-container {
  display: flex;
  align-items: center;
  position: relative;
}

.custom-popover {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 8px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  width: 300px;
}

.custom-popover::before {
  content: '';
  position: absolute;
  top: -6px;
  right: 20px;
  width: 12px;
  height: 12px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-bottom: none;
  border-right: none;
  transform: rotate(45deg);
  z-index: -1;
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

.filter-btn {
  text-wrap: nowrap;
  background-color: #3f51b5;
  color: #ffffff;
  height: 30px;
  border-radius: 5px;
  border: 0;
  padding: 5px 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}
.filter-btn:hover {
  background-color: #303f9f;
}
.filter-btn:active {
  transform: scale(0.95);
}

.filter-content {
  padding: 20px;
  span {
   font-size: 15px;
  }
}

.filter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  color: #333333;
}

.filter-actions {
  margin-top: 20px;
  text-align: right;
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}
:deep(.el-button--primary:active) {
  transform: scale(0.95);
}

:deep(.el-date-editor.is-focus .el-input__wrapper) {
box-shadow: 0 0 0 1px #3f51b5 inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3f51b5 inset;
}

.disabled-btn {
  background-color: #909399;
  color:#ffffff;
  cursor: not-allowed;
  border-radius: calc(var(--el-border-radius-base) - 1px);
  font-size: 12px;
  padding-left: 11px;
  padding-right: 11px;
  border: 0;
  height: 24px;
}

/* AI助手按钮样式 */
.ai-assistant-btn {
  position: absolute;
  bottom: 50px;
  right: 50px;
  z-index: 1000;
  background: linear-gradient(135deg, #3f51b5, #303f9f);
  color: white;
  border: none;
  border-radius: 25px;
  padding: 12px 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 12px rgba(63, 81, 181, 0.3);
  transition: all 0.3s ease;
  font-size: 14px;
  font-weight: 500;
  user-select: none;
}

.ai-assistant-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(63, 81, 181, 0.4);
}

.ai-assistant-btn:active {
  transform: translateY(0);
}

/* AI抽屉内容样式 */
.ai-drawer-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-welcome {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8f9fa;
}

.ai-welcome h3 {
  margin: 0 0 10px 0;
  color: #333;
  font-size: 18px;
}

.ai-welcome p {
  margin: 0 0 10px 0;
  color: #666;
  font-size: 14px;
}

.ai-welcome ul {
  margin: 0;
  padding-left: 20px;
  color: #666;
  font-size: 13px;
}

.ai-welcome li {
  margin-bottom: 5px;
}

.ai-chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: calc(100% - 200px);
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  max-height: 400px;
}

.message {
  margin-bottom: 15px;
  display: flex;
}

.message-content {
  max-width: 80%;
  padding: 10px 15px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.4;
  word-wrap: break-word;
}

.ai-message {
  justify-content: flex-start;
}

.ai-message .message-content {
  background: #f1f3f8;
  color: #333;
  border-bottom-left-radius: 4px;
}

.user-message {
  justify-content: flex-end;
}

.user-message .message-content {
  background: #3f51b5;
  color: white;
  border-bottom-right-radius: 4px;
}

.chat-input {
  padding: 20px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.chat-input :deep(.el-input-group__append) {
  background: #3f51b5;
  color: white;
  border: none;
}

.chat-input :deep(.el-input-group__append .el-button) {
  color: white;
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

.filter-btn {
  text-wrap: nowrap;
  background-color: #3f51b5;
  color: #ffffff;
  height: 30px;
  border-radius: 5px;
  border: 0;
  padding: 5px 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}
.filter-btn:hover {
  background-color: #303f9f;
}
.filter-btn:active {
  transform: scale(0.95);
}

.filter-content {
  padding: 20px;
  span {
    font-size: 15px;
  }
}

.filter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  color: #333333;
}

.filter-actions {
  margin-top: 20px;
  text-align: right;
}

:deep(.el-switch.is-checked .el-switch__core) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}
:deep(.el-button--primary:active) {
  transform: scale(0.95);
}

:deep(.el-date-editor.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 1px #3f51b5 inset;
}
:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #3f51b5 inset;
}

.disabled-btn {
  background-color: #909399;
  color:#ffffff;
  cursor: not-allowed;
  border-radius: calc(var(--el-border-radius-base) - 1px);
  font-size: 12px;
  padding-left: 11px;
  padding-right: 11px;
  border: 0;
  height: 24px;
}
:deep(.el-drawer__title) {
  font-size: 18px;
  font-weight: bold;
}
:deep(.el-drawer__body) {
  padding-top: 0;
  padding-bottom: 0;
}
</style>