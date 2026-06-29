<template>
  <div class="tab-container scrollbar">
    <div class="tab-main">
      <div class="approval-list" v-if="approvalList.length > 0">
        <div class="approval-card" v-for="item in approvalList" :key="item.id">
          <div class="card-header">
            <span class="status-tag" :class="item.status">{{ item.statusText }}</span>
            <span class="card-id">{{ item.appointmentNo }}</span>
          </div>
          <div class="card-body">
            <div class="info-row">
              <label>预约人</label>
              <span>{{ item.name }}</span>
            </div>
            <div class="info-row">
              <label>工号</label>
              <span>{{ item.emp_id }}</span>
            </div>
            <div class="info-row">
              <label>资源名称</label>
              <span>{{ item.res_name }}</span>
            </div>
            <div class="info-row">
              <label>资源位置</label>
              <span>{{ item.location }}</span>
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
            <button class="btn btn-approve" @click="handleApproval(item.id, true)">通过</button>
            <button class="btn btn-reject" @click="handleApproval(item.id, false)">拒绝</button>
          </div>
        </div>
      </div>
      <div class="empty-state" v-else>
        <ElEmpty description="当前没有需要审批的资源预约申请" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElEmpty } from 'element-plus'

const approvalList = ref([])

const formatTimestamp = (timestamp) => {
  const date = new Date(timestamp * 1000)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const generateAppointmentNo = (appointmentDate, id) => {
  const dateStr = appointmentDate.replace(/-/g, '').substring(0, 11)
  return `APR-${dateStr}B${id}`
}

const fetchApprovalList = async () => {
  try {
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/get-pending-approvals`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      }
    })
    
    const result = await response.json()
    
    if (result.success) {
      approvalList.value = result.data.map(item => ({
        ...item,
        startTime: formatTimestamp(item.start_time),
        endTime: formatTimestamp(item.end_time),
        appointmentNo: generateAppointmentNo(item.appointment_date, item.id),
        status: 'pending',
        statusText: '待审批'
      }))
    }
  } catch (error) {
    console.error('获取待审批列表失败:', error)
  }
}

const handleApproval = async (id, isApprove) => {
  try {
    let token = localStorage.getItem('token') || ''
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/resource-mgm/approve-appointment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        token: token,
        data: {
          id: id,
          boolean: isApprove
        }
      })
    })
    
    const result = await response.json()
    
    if (result.success) {
      await fetchApprovalList()
    }
  } catch (error) {
    console.error('审批操作失败:', error)
  }
}

onMounted(() => {
  fetchApprovalList()
})
</script>

<style scoped>
* {
  user-select: none;
}
.flex {
  display: flex;
  justify-content: space-between;
}
.tab-container {
  padding: 20px;
}
.approval-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}
.approval-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.approval-card:hover {
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
.btn-approve {
  background: #3f51b5;
  color: #fff;
}
.btn-approve:hover {
  opacity: 0.9;
}
.btn-reject {
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
}
.btn-reject:hover {
  background: #f5f5f5;
  border-color: #ccc;
}
.empty-state {
  padding: 80px 20px;
}
</style>