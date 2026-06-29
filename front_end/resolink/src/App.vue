<template>
  <Login v-if="!isLoggedIn" @login-success="checkLoginStatus" />
  <div class="container" v-else>
    <div class="left">
      <Left 
        :selectedCard="selectedCard"
        @cardSelect="handleCardSelect"
      />
    </div>
    <div class="right">
      <Right 
        :tabs="tabs"
        :selectedTab="selectedTab"
        @tabSelect="handleTabSelect"
        @tabClose="handleTabClose"
      />
    </div>
  </div>
  
  <!-- 浮窗提示 -->
  <div v-if="showToast" :class="['toast', toastType]">
    <i :class="toastIcon"></i>
    <span>{{ toastMessage }}</span>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import Left from "./components/Left.vue"
import Right from "./components/Right.vue"
import Login from "./components/Login.vue";
import { useUserInfoStore } from './stores/userInfo';
import { useTokenStore } from './stores/token';

// 登录状态
const isLoggedIn = ref(false)
// 选中的卡片
const selectedCard = ref('')
// 选中的标签
const selectedTab = ref('home')
// 标签页数据
const tabs = reactive([
  {
    id: 'home',
    title: '首页',
    icon: 'house',
    closable: false // 首页不可关闭
  }
])

// 浮窗提示状态
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')
const toastIcon = ref('icon-success')

// 显示浮窗提示
const displayToast = (message, type = 'success') => {
  toastMessage.value = message
  toastType.value = type
  
  // 设置图标
  switch(type) {
    case 'success':
      toastIcon.value = 'icon-success'
      break
    case 'failure':
      toastIcon.value = 'icon-failure'
      break
    case 'error':
      toastIcon.value = 'icon-error'
      break
    default:
      toastIcon.value = 'icon-success'
  }
  
  showToast.value = true
  
  // 3秒后自动隐藏
  setTimeout(() => {
    showToast.value = false
  }, 3000)
}

// 验证token的方法
const validateToken = async () => {
  const tokenStore = useTokenStore()
  const token = (tokenStore.getToken || '').replace(/^['"]|['"]$/g, '').trim()
  if (token === 'null') return
  if (!token) return
  
  try {
    // 发送token验证请求
    const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/token/validate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ token })
    })
    
    const data = await response.json()
    if (data.success !== true) {
      // token验证失败，清除所有store数据并刷新页面
      const userInfoStore = useUserInfoStore()
      tokenStore.clearToken()
      userInfoStore.clearUserInfo()
      displayToast('token已过期，请重新登录', 'failure')
      setTimeout(() => {
        window.location.reload()
      }, 1500)
    }
  } catch (error) {
    console.error('token验证失败:', error)
    // 验证失败，清除所有store数据并刷新页面
    const userInfoStore = useUserInfoStore()
    tokenStore.clearToken()
    userInfoStore.clearUserInfo()
    displayToast('网络连接失败，请检查网络设置', 'failure')
    setTimeout(() => {
      window.location.reload()
    }, 1500)
  }
}

// 获取用户信息store
const userInfoStore = useUserInfoStore();

// 检查登录状态的方法
const checkLoginStatus = () => {
  isLoggedIn.value = userInfoStore.getIsLoggedIn
}

// 组件挂载时检查登录状态和验证token
onMounted(() => {
  checkLoginStatus()
  validateToken()
})

// 暴露检查方法给父组件
defineExpose({
  checkLoginStatus
})

// 处理卡片选择
function handleCardSelect(cardType) {
  selectedCard.value = selectedCard.value === cardType ? '' : cardType
  
  if (selectedCard.value) {
    // 检查是否已存在对应的标签
    const existingTab = tabs.find(tab => tab.id === cardType)
    
    if (!existingTab) {
      // 添加新标签
      let title = ''
      let icon = ''
      
      if (cardType === 'space-core') {
        title = '核心业务空间'
        icon = 'building'
      } else if (cardType === 'space-public') {
        title = '公共辅助空间'
        icon = 'users'
      } else if (cardType === 'space-special') {
        title = '特殊功能空间'
        icon = 'star'
      } else if (cardType === 'asset-device') {
        title = '办公设备'
        icon = 'desktop'
      } else if (cardType === 'asset-supplies') {
        title = '办公耗材'
        icon = 'pen-to-square'
      } else if (cardType === 'asset-transport') {
        title = '交通出行'
        icon = 'car'
      } else if (cardType === 'account') {
        title = '账号管理'
        icon = 'user-cog'
      } else if (cardType === 'space-inventory') {
        title = '空间资源管理'
        icon = 'building'
      } else if (cardType === 'physical-inventory') {
        title = '实物资源管理'
        icon = 'box'
      } else if (cardType === 'bus-management') {
        title = '班车管理'
        icon = 'bus'
      } else if (cardType === 'approval') {
        title = '预约审批'
        icon = 'clipboard-check'
      } else if (cardType === 'inspection') {
        title = '资源检查'
        icon = 'clipboard-list'
      } else if (cardType === 'public') {
        title = '公共信息'
        icon = 'info-circle'
      } else if (cardType === 'history') {
        title = '历史记录'
        icon = 'clock-rotate-left'
      }
      
      tabs.push({
        id: cardType,
        title: title,
        icon: icon,
        closable: true // 这些标签可以关闭
      })
    }
    
    // 选中对应的标签
    selectedTab.value = cardType
  }
}

// 处理标签选择
function handleTabSelect(tabId) {
  selectedTab.value = tabId
  
  // 如果不是首页标签，同步到左侧卡片
  if (tabId !== 'home') {
    selectedCard.value = tabId
  } else {
    selectedCard.value = ''
  }
}

// 处理标签关闭
function handleTabClose(tabId) {
  // 找到要关闭的标签的索引
  const index = tabs.findIndex(tab => tab.id === tabId)
  
  if (index > 0) { // 确保不是首页标签
    // 如果关闭的是当前选中的标签，需要选择一个新的标签
    if (selectedTab.value === tabId) {
      // 优先选择下一个标签，如果是最后一个则选择首页
      if (index < tabs.length - 1) {
        selectedTab.value = tabs[index + 1].id
      } else {
        selectedTab.value = 'home'
      }
      
      // 更新左侧卡片选中状态
      if (selectedTab.value !== 'home') {
        selectedCard.value = selectedTab.value
      } else {
        selectedCard.value = ''
      }
    }
    
    // 从标签列表中移除
    tabs.splice(index, 1)
    
    // 如果关闭的是某个卡片对应的标签，需要取消该卡片的选中状态
    if (selectedCard.value === tabId) {
      selectedCard.value = ''
    }
  }
}
</script>

<style scoped>
.container {
  display: flex;
  width: 100%;
  height: 100vh;
}

.left {
  user-select: none;
  display: flex;
  flex-direction: column;
  width: 200px;
  background-color: #3f51b5;
  box-sizing: border-box;
}

.right {
  width: calc(100% - 200px);
  background-color: #e4e4e4;
  box-sizing: border-box;
}

/* 浮窗提示样式 */
.toast {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 24px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  animation: slideInRight 0.3s ease-out;
}

.toast.success {
  background-color: rgba(46, 204, 113, 0.9);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.toast.failure {
  background-color: rgba(231, 76, 60, 0.9);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.toast.error {
  background-color: rgba(243, 156, 18, 0.9);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.toast i {
  margin-right: 8px;
  font-size: 16px;
}

/* 动画效果 */
@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 图标样式 */
.icon-success::before {
  content: "✓";
  font-weight: bold;
}

.icon-failure::before {
  content: "✗";
  font-weight: bold;
}

.icon-error::before {
  content: "!";
  font-weight: bold;
}
</style>