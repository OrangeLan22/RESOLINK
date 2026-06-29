<template>
  <!-- 账号设置弹窗 -->
  <el-dialog
    v-model="accountSettingsVisible"
    title="账号设置"
    width="400px"
  >
    <div class="account-settings-content">
      <!-- 飞书绑定卡片 -->
      <div class="binding-card">
        <div class="card-left" v-if="!isLoading">
          <div class="avatar-container">
            <img v-if="feishuBound" :src="feishuUser.avatar" alt="飞书头像" class="avatar">
            <div v-else class="feishu-icon-container">
              <img src="../../assets/feishu.png" alt="飞书" class="feishu-icon"/>
            </div>
          </div>
          <div class="user-info">
            <div v-if="!feishuBound" class="service-name">飞书</div>
            <div v-if="feishuBound" class="user-name">{{feishuUser.name}}</div>
            <div class="binding-status">{{feishuBound ? '已绑定飞书' : '未绑定'}}</div>
          </div>
        </div>
        <!-- 加载骨架 -->
        <div class="card-left loading-skeleton" v-else>
          <div class="avatar-skeleton"></div>
          <div class="info-skeleton">
            <div class="name-skeleton"></div>
            <div class="status-skeleton"></div>
          </div>
        </div>
        <div class="card-right">
          <span class="action-text" @click="feishuBound ? showUnbindConfirm() : handleFeishuBind()" v-if="!isLoading">{{feishuBound ? '解绑' : '绑定'}}&gt;</span>
          <span class="loading-text" v-else>加载中...</span>
        </div>
      </div>
      
      <!-- 解绑确认对话框 -->
      <el-dialog
        v-model="unbindConfirmVisible"
        title="确认解绑"
        width="300px"
      >
        <p>确定要解绑飞书账号吗？</p>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="unbindConfirmVisible = false">取消</el-button>
            <el-button type="danger" @click="handleFeishuUnbind">确定</el-button>
          </span>
        </template>
      </el-dialog>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserInfoStore } from '../../stores/userInfo'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:visible'])

const accountSettingsVisible = ref(props.visible)

// 获取用户信息store
const userInfoStore = useUserInfoStore()

// 从store中获取飞书绑定状态
const feishuBound = computed(() => userInfoStore.feishuBindStatus.isBound)
const feishuUser = computed(() => userInfoStore.feishuBindStatus.userInfo || { name: '', avatar: '' })

// 加载状态
const isLoading = ref(false)

// 解绑确认对话框状态
const unbindConfirmVisible = ref(false)

// 显示解绑确认对话框
function showUnbindConfirm() {
  unbindConfirmVisible.value = true
}

// 监听visible变化
watch(() => props.visible, (newVal) => {
  accountSettingsVisible.value = newVal
  if (newVal) {
    // 当弹窗打开时，获取飞书绑定状态
    getBindStatus()
  }
})

// 监听弹窗关闭
watch(accountSettingsVisible, (newVal) => {
  emit('update:visible', newVal)
})

// 组件挂载时获取绑定状态
onMounted(() => {
  if (props.visible) {
    getBindStatus()
  }
})

// 获取绑定状态
async function getBindStatus() {
  isLoading.value = true
  try {
    const token = (localStorage.getItem('token') || '').replace(/^['"]|['"]$/g, '').trim()
    const response = await fetch('http://localhost:2307/api/lark/getBindStatus', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      body: JSON.stringify({ token: token })
    })
    
    const data = await response.json()
    if (data.success) {
      if (data.isBound) {
        // 获取飞书用户信息
        await getLarkUserInfo()
      } else {
        // 更新store中的绑定状态
        userInfoStore.updateFeishuBindStatus({ isBound: false, userInfo: null })
      }
    } else {
      // API返回失败时，直接设置为未绑定状态，不显示错误提示
      userInfoStore.updateFeishuBindStatus({ isBound: false, userInfo: null })
    }
  } catch (error) {
    console.error('发生错误:', error)
    // 网络错误时，直接设置为未绑定状态，不显示错误提示
    userInfoStore.updateFeishuBindStatus({ isBound: false, userInfo: null })
  } finally {
    isLoading.value = false
  }
}

// 获取飞书用户信息
async function getLarkUserInfo() {
  try {
    const token = (localStorage.getItem('token') || '').replace(/^['"]|['"]$/g, '').trim()
    const response = await fetch('http://localhost:2307/api/lark/user-info', {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      }
    })
    
    const data = await response.json()
    console.log('飞书用户信息API返回:', data)
    if (data.success) {
      const user = data.data.user
      console.log('飞书用户信息:', user)
      // 处理头像信息，获取合适尺寸的头像URL
      let avatarUrl = ''
      if (user.avatar && typeof user.avatar === 'object') {
        // 优先使用240x240尺寸的头像
        avatarUrl = user.avatar.avatar_240 || user.avatar.avatar_640 || user.avatar.avatar_72 || user.avatar.avatar_origin || ''
        // 移除可能存在的反引号
        avatarUrl = avatarUrl.replace(/[`]/g, '').trim()
      } else if (user.avatar_url) {
        avatarUrl = user.avatar_url
      }
      
      const feishuUserInfo = {
        name: user.name || user.display_name || '',
        avatar: avatarUrl
      }
      // 更新store中的绑定状态
      userInfoStore.updateFeishuBindStatus({ isBound: true, userInfo: feishuUserInfo })
    } else {
      ElMessage.error('获取飞书用户信息失败: ' + data.message)
    }
  } catch (error) {
    console.error('发生错误:', error)
  }
}

// 处理绑定飞书
async function handleFeishuBind() {
  try {
    const token = (localStorage.getItem('token') || '').replace(/^['"]|['"]$/g, '').trim()
    const response = await fetch('http://localhost:2307/api/lark/generateAuthUrl', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      body: JSON.stringify({ token: token })
    })
    
    const data = await response.json()
    if (data.success) {
      // 跳转到飞书授权页面
      window.open(data.authUrl, '_blank')
      // 关闭当前弹窗
      accountSettingsVisible.value = false
    } else {
      ElMessage.error('生成授权URL失败: ' + data.message)
    }
  } catch (error) {
    console.error('发生错误:', error)
    ElMessage.error('网络错误，请检查网络连接')
  }
}

// 处理解绑飞书
async function handleFeishuUnbind() {
  try {
    const token = (localStorage.getItem('token') || '').replace(/^['"]|['"]$/g, '').trim()
    const response = await fetch('http://localhost:2307/api/lark/unbind', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      body: JSON.stringify({ token: token })
    })
    
    const data = await response.json()
    if (data.success) {
      // 更新store中的绑定状态
      userInfoStore.clearFeishuBindStatus()
      // 关闭确认对话框
      unbindConfirmVisible.value = false
      ElMessage.success('飞书账号解绑成功')
    } else {
      // 关闭确认对话框
      unbindConfirmVisible.value = false
      ElMessage.error('解绑失败: ' + data.message)
    }
  } catch (error) {
    console.error('发生错误:', error)
    ElMessage.error('网络错误，请检查网络连接')
  }
}
</script>

<style scoped>
.account-settings-content {
  padding: 20px 0;
}

.binding-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.binding-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #3f51b5;
}

.card-left {
  display: flex;
  align-items: center;
}

.avatar-container {
  margin-right: 15px;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.feishu-icon-container {
  width: 50px;
  height: 50px;
  background-color: transparent;
  border-radius: 50%;
  border: 1px solid #d1d1d1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.feishu-icon {
  width: 35px;
  height: 35px;
  object-fit: contain;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.service-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.binding-status {
  font-size: 12px;
  color: #999;
}

.card-right {
  color: #3f51b5;
  font-size: 14px;
}

.action-text {
  font-weight: 500;
}

/* 加载骨架样式 */
.loading-skeleton {
  display: flex;
  align-items: center;
}

.avatar-skeleton {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s ease-in-out infinite;
  margin-right: 15px;
}

.info-skeleton {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.name-skeleton {
  width: 120px;
  height: 16px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s ease-in-out infinite;
  border-radius: 4px;
}

.status-skeleton {
  width: 80px;
  height: 12px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: loading 1.5s ease-in-out infinite;
  border-radius: 4px;
}

.loading-text {
  color: #999;
  font-size: 14px;
}

@keyframes loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>