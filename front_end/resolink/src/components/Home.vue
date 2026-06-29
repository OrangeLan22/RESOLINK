<template>
  <div class="tab-container scrollbar">
    <div class="tab-main flex">
      <div class="left">
        <div class="chose">
          <div class="heard-title">
            <h3
                class="item"
                :class="{ 'active': localSelectedTab === 'affair' }"
                @click="handleTabClick('affair')"
            >
              我的事务
            </h3>
            <h3
                class="item"
                :class="{ 'active': localSelectedTab === 'notice' }"
                @click="handleTabClick('notice')"
            >
              通知
              <span v-if="notificationStore.notificationCount > 0" class="badge">{{ notificationStore.notificationCount }}</span>
            </h3>
          </div>
          <div class="menu" v-if="localSelectedTab === 'notice'">
            <Menu />
          </div>

        </div>
        <div v-if="localSelectedTab === 'affair'">
          <Affair />
        </div>
        <div v-if="localSelectedTab === 'notice'">
          <Notice :notices="notificationStore.notices" />
        </div>
      </div>
      <div class="right">
        <Info />
        <TokenExpiry />
        <Mark />
      </div>
    </div>
  </div>
</template>


<script setup>
import Info from './home/info.vue'
import TokenExpiry from './home/TokenExpiry.vue'
import Mark from './home/mark.vue'
import Affair from "./home/affair.vue";
import Notice from "./home/notice.vue";
import { ref, watch, onMounted, onUnmounted } from 'vue';
import Menu from "./home/menu.vue";
import { useNotificationStore } from '../stores/notifications';
import { useUserInfoStore } from '../stores/userInfo';

// 定义props
const props = defineProps({
  selectedTab: {
    type: String,
    default: 'affair'
  }
})

// 定义emits
const emit = defineEmits(['tabSelect'])

// 本地选中的选项卡
const localSelectedTab = ref(props.selectedTab);

// 获取通知store
const notificationStore = useNotificationStore();

// 监听props变化，更新本地状态
watch(() => props.selectedTab, (newVal) => {
  localSelectedTab.value = newVal;
});

// 处理选项卡点击
function handleTabClick(tabType) {
  localSelectedTab.value = tabType;
  emit('tabSelect', tabType);
  // 点击通知选项时，清空未读标记
  if (tabType === 'notice') {
    notificationStore.markAsRead();
  }
}

// 处理预约成功事件
function handleAppointmentSuccess(event) {
  const newNotice = {
    id: Date.now(),
    title: event.detail.title,
    content: event.detail.content,
    time: event.detail.time
  };
  notificationStore.addNotice(newNotice);
  // 发送事件到父组件，更新首页标签的Badge
  window.dispatchEvent(new CustomEvent('updateNotificationCount', {
    detail: { count: notificationStore.notificationCount }
  }));
}

// 生命周期钩子
onMounted(() => {
  window.addEventListener('appointmentSuccess', handleAppointmentSuccess);
  // 获取飞书绑定状态
  getFeishuBindStatus();
});

// 获取飞书绑定状态
async function getFeishuBindStatus() {
  try {
    const token = (localStorage.getItem('token') || '').replace(/^['"]|['"]$/g, '').trim();
    const response = await fetch('http://localhost:2307/api/lark/getBindStatus', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'token': token
      },
      body: JSON.stringify({ token: token })
    });
    
    const data = await response.json();
    if (data.success) {
      if (data.isBound) {
        // 获取飞书用户信息
        const userInfoResponse = await fetch('http://localhost:2307/api/lark/user-info', {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'token': token
          }
        });
        
        const userInfoData = await userInfoResponse.json();
        if (userInfoData.success) {
          const user = userInfoData.data.user;
          console.log('飞书用户信息:', user);
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
          };
          // 更新store中的绑定状态
          const userInfoStore = useUserInfoStore();
          userInfoStore.updateFeishuBindStatus({ isBound: true, userInfo: feishuUserInfo });
        }
      }
    }
  } catch (error) {
    console.error('获取飞书绑定状态失败:', error);
  }
}

onUnmounted(() => {
  window.removeEventListener('appointmentSuccess', handleAppointmentSuccess);
});

</script>

<style scoped>
* {
  user-select: none;
}
.flex {
  display: flex;
  justify-content: space-between;
}

.left {
  margin-right: 10px;
  flex: 1;
}

.right {
  min-width: 200px;
  max-width: 300px;
  margin-left: 10px;
}

.chose {
  display: flex;
  justify-content: space-between;
  align-items: center;
  white-space: nowrap;
  margin-bottom: 20px;
}

.heard-title {
  display: flex;
}

.item {
  margin: 0 20px 0 0;
  font-weight: normal;
  font-size: 18px;
  letter-spacing: 1px;
  cursor: pointer;
  padding-bottom: 2px;
  position: relative;
  color: #333;
  transition: all 0.1s ease;
}

.item::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 0;
  height: 2px;
  background-color: rgba(63, 81, 181, 0.5);
  transition: width 0.1s ease;
}

.item:hover::after {
  width: 100%;
}

.item.active {
  font-weight: bold;
}

.item.active::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  background-color: #3f51b5;
}

.badge {
  position: absolute;
  top: -8px;
  right: -12px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  min-width: 18px;
  height: 18px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

</style>