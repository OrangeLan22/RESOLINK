<template>
  <div class="tab-container scrollbar">
    <div class="tab-main">
      <NoPermission v-if="!hasPermission" />
      <div v-else class="inspection-main">
        <div class="inspection-list" v-if="inspectionList.length > 0">
          <div class="inspection-card" v-for="item in inspectionList" :key="item.id">
            <div class="card-header">
              <span class="status-tag" :class="item.status">{{ item.statusText }}</span>
              <span class="card-id">{{ item.inspectionNo }}</span>
            </div>
            <div class="card-body">
            <div class="info-row">
              <label>资源名称</label>
              <span>{{ item.resName }}</span>
            </div>
            <div class="info-row">
              <label>资源位置</label>
              <span>{{ item.location }}</span>
            </div>
            <div class="info-row">
              <label>预约人</label>
              <span>{{ item.name }}</span>
            </div>
            <div class="info-row">
              <label>工号</label>
              <span>{{ item.empId }}</span>
            </div>
            <div class="time-row">
              <div class="time-item">
                <label>开始时间</label>
                <span>{{ item.startTime }}</span>
              </div>
              <div class="time-item">
                <label>结束时间</label>
                <span>{{ item.endTime }}</span>
              </div>
            </div>
          </div>
            <div class="card-footer">
              <button class="btn btn-complete" @click="handleCheck(item.id, true)">完成</button>
              <button class="btn btn-abnormal" @click="handleCheck(item.id, false)">异常</button>
            </div>
          </div>
        </div>
        <div class="empty-state" v-else>
          <ElEmpty description="当前没有需要检查的资源" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElEmpty, ElMessageBox } from 'element-plus'
import NoPermission from "./Common/NoPermission.vue"
import { useResourcesStore } from "../stores/resources"

const resourcesStore = useResourcesStore()

const hasPermission = computed(() => resourcesStore.$state.hasPermission)

const inspectionList = ref([])

const formatTimestamp = (timestamp) => {
  const date = new Date(timestamp * 1000)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const generateInspectionNo = (appointmentDate, id) => {
  const dateStr = appointmentDate.replace(/-/g, '').substring(0, 11)
  return `RCK-${dateStr}C${id}`
}

const fetchInspectionList = async () => {
  try {
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/check/pending-check`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: token
      })
    })
    
    const result = await response.json()
    
    if (result.success) {
      inspectionList.value = result.data.map(item => ({
        ...item,
        startTime: formatTimestamp(item.startTime),
        endTime: formatTimestamp(item.endTime),
        inspectionNo: generateInspectionNo(item.appointmentDate, item.id),
        status: 'pending',
        statusText: '待检查'
      }))
    }
  } catch (error) {
    console.error('获取检查列表失败:', error)
  }
}

const handleCheck = async (id, isComplete) => {
  try {
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    let closeValue = true
    
    if (!isComplete) {
      try {
        await ElMessageBox.confirm(
          '是否禁用该资源',
          '提示',
          {
            confirmButtonText: '禁用',
            confirmButtonType: 'danger',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
      } catch {
        closeValue = false
      }
    }
    
    let apiUrl, requestData
    
    if (isComplete) {
      apiUrl = `${import.meta.env.VITE_API_BASE_URL}/api/check/check-status`
      requestData = {
        token: token,
        data: {
          id: id,
          boolean: true
        }
      }
    } else {
      apiUrl = `${import.meta.env.VITE_API_BASE_URL}/api/check/check-status`
      requestData = {
        token: token,
        data: {
          id: id,
          boolean: false,
          close: closeValue
        }
      }
    }
    
    const response = await fetch(apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(requestData)
    })
    
    const result = await response.json()
    
    if (result.success) {
      await fetchInspectionList()
    }
  } catch (error) {
    console.error('检查操作失败:', error)
  }
}

onMounted(() => {
  if (hasPermission.value) {
    fetchInspectionList()
  }
})
</script>

<style scoped>
* {
  user-select: none;
}
.tab-container {
  padding: 20px;
}
.inspection-main h2 {
  margin-bottom: 20px;
}
.inspection-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}
.inspection-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.inspection-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}
.status-tag {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}
.status-tag.pending {
  background: rgba(255, 255, 255, 0.25);
}
.card-id {
  font-size: 12px;
  opacity: 0.9;
}
.card-body {
  padding: 20px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.info-row:last-of-type {
  border-bottom: none;
}
.info-row label {
  color: #888;
  font-size: 14px;
}
.info-row span {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}
.time-row {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.time-item {
  flex: 1;
}
.time-item label {
  display: block;
  color: #888;
  font-size: 12px;
  margin-bottom: 4px;
}
.time-item span {
  color: #333;
  font-size: 13px;
}
.time-item:last-child span {
  text-align: right;
  display: block;
}
.time-item:last-child label {
  text-align: right;
}
.card-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  background: #f8f9fa;
}
.btn {
  flex: 1;
  padding: 10px 16px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-complete {
  background: #3f51b5;
  color: #fff;
}
.btn-complete:hover {
  opacity: 0.9;
}
.btn-abnormal {
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
}
.btn-abnormal:hover {
  background: #f5f5f5;
  border-color: #ccc;
}
.empty-state {
  padding: 80px 20px;
}
</style>