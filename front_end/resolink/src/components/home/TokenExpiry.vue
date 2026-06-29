<template>
  <div class="token-expiry">
    <h3>距离登录过期</h3>
    <div class="expiry-info">
      <div class="expiry-item">
        <span v-if="targetTime" class="value">
          <span v-if="targetTime > Date.now()">
            {{ remainingTime }}
          </span>
          <span v-else >已过期</span>
        </span>
        <span v-else class="value">无token</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

// 解析JWT token
const parseJWT = (token) => {
  try {
    const base64Url = token.split('.')[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      window.atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    return JSON.parse(jsonPayload)
  } catch (error) {
    console.error('解析token失败:', error)
    return null
  }
}

// 获取token到期时间
const getTokenExpiry = () => {
  const token = localStorage.getItem('token')
  if (!token) return null
  
  const tokenData = parseJWT(token)
  if (!tokenData || !tokenData.exp) return null
  
  return tokenData.exp * 1000 // 转换为毫秒
}

// 响应式数据
const targetTime = ref(null)
const remainingTime = ref('')

// 定时器
let updateTimer = null

// 格式化剩余时间为 HH:MM:SS 格式
const formatRemainingTime = (milliseconds) => {
  const totalSeconds = Math.floor(milliseconds / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  
  // 补零确保两位数格式
  const formattedHours = String(hours).padStart(2, '0')
  const formattedMinutes = String(minutes).padStart(2, '0')
  const formattedSeconds = String(seconds).padStart(2, '0')
  
  return `${formattedHours}:${formattedMinutes}:${formattedSeconds}`
}

// 更新剩余时间
const updateRemainingTime = () => {
  if (targetTime.value && targetTime.value > Date.now()) {
    const timeLeft = targetTime.value - Date.now()
    remainingTime.value = formatRemainingTime(timeLeft)
  } else {
    if (updateTimer) {
      clearInterval(updateTimer)
    }
  }
}

// 初始化token信息
const initTokenInfo = () => {
  const expiry = getTokenExpiry()
  if (expiry) {
    targetTime.value = expiry
    updateRemainingTime()
    
    // 每秒更新一次剩余时间
    if (updateTimer) {
      clearInterval(updateTimer)
    }
    updateTimer = setInterval(updateRemainingTime, 1000)
  } else {
    targetTime.value = null
    if (updateTimer) {
      clearInterval(updateTimer)
    }
  }
}

// 组件挂载时初始化
onMounted(() => {
  initTokenInfo()
})

// 组件卸载时清理
onUnmounted(() => {
  if (updateTimer) {
    clearInterval(updateTimer)
  }
})
</script>

<style scoped>
.token-expiry {
  padding: 15px;
  margin-top: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 5px;
}

h3 {
  margin: 0;
  font-weight: normal;
  letter-spacing: 1px;
  font-size: 14px;
  text-align: center;
}

.expiry-info {
  font-size: 18px;
  display: flex;
  justify-content: center;
}

.expiry-item {
  display: flex;
  align-items: center;
}

</style>