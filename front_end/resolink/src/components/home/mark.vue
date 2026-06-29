<template>
  <div class="mark">
    <div class="main">
      <label>桌面通知</label>
      <div class="status">
        <span class="status-text" :class="statusClass" @click="enableNotification">{{noticeStatus}}</span>
      </div>
    </div>
    <div class="warn" v-if="isWarnVisible">
      <FontAwesomeIcon :icon="['fas', 'exclamation-triangle']"  class="warn-icon"/>
      <span class="warn-text">开启以及时获取通知</span>
    </div>
  </div>
  <div class="test-container">
    <button @click="testNotification" class="test-button">发送测试通知</button>
  </div>
</template>

<script setup>
import {ref, onMounted, computed} from "vue";
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome";
import {library} from "@fortawesome/fontawesome-svg-core";
import {faExclamationTriangle} from "@fortawesome/free-solid-svg-icons";
import {getNotificationPermission, requestNotificationPermission, sendNotification} from "../../utils/notification";

library.add(faExclamationTriangle);

// 桌面通知开启状态
const noticeStatus = ref('点此开启通知');

// 检查通知权限状态
const checkNotificationStatus = () => {
  const permission = getNotificationPermission();
  switch (permission) {
    case 'granted':
      noticeStatus.value = '已开启';
      break;
    case 'denied':
      noticeStatus.value = '已拒绝';
      break;
    default:
      noticeStatus.value = '点此开启通知';
  }
};

// 请求开启通知
const enableNotification = async () => {
  await requestNotificationPermission();
  // 获取实际的权限状态
  const permission = getNotificationPermission();
  switch (permission) {
    case 'granted':
      noticeStatus.value = '已开启';
      break;
    case 'denied':
      noticeStatus.value = '已拒绝';
      // 当用户拒绝权限时显示弹窗提示
      alert('通知权限被拒绝，请在浏览器设置中手动开启以接收通知');
      break;
    default:
      noticeStatus.value = '点此开启';
  }
};

// 计算状态文本的样式类
const statusClass = computed(() => {
  switch (noticeStatus.value) {
    case '已开启':
      return 'status-granted';
    case '已拒绝':
      return 'status-denied';
    default:
      return 'status-default';
  }
});

// 计算warn是否显示
const isWarnVisible = computed(() => {
  return noticeStatus.value !== '已开启';
});

// 测试通知功能
const testNotification = async () => {
  
  // 检查当前权限
  if (getNotificationPermission() === 'granted') {
    // 已获得权限，直接发送通知
    const success = sendNotification('Resolink', {
      body: '桌面通知已开启！',
      icon: '/vite.svg'
    });
    
    if (!success) {
      alert('通知发送失败，请检查浏览器设置');
    }
  } else {
    // 未获得权限，请求权限后再发送
    console.log('未获得通知权限，正在请求权限...');
    const granted = await requestNotificationPermission();
    
    if (granted) {
      noticeStatus.value = '已开启';
      console.log('权限请求成功，正在发送通知...');
      sendNotification('Resolink', {
        body: '桌面通知已开启！',
        icon: '/vite.svg'
      });
    } else {
      noticeStatus.value = '已拒绝';
      console.log('权限请求被拒绝');
      alert('通知权限被拒绝，请在浏览器设置中手动开启');
    }
  }
};

// 组件挂载时检查通知状态
onMounted(() => {
  checkNotificationStatus();
});
</script>

<style scoped>
.mark {
  padding: 10px 15px;
  margin-top: 15px;
  border: 1px solid #e0e0e0;
  border-radius: 5px;
  font-weight: bolder;
}

.main {
  display: flex;
  justify-content: space-between;
}

.status-text {
  font-size: 12px;
  font-weight: normal;
  padding: 2px 6px;
  border-radius: 4px;
  color: #ffffff;
  cursor: pointer;
}

.status-default {
  background-color: rgba(92, 92, 92, 0.35);
}

.status-granted {
  background-color: #4CAF50;
}

.status-denied {
  background-color: #f44336;
}

.warn {
  display: flex;
  align-items: center;
  padding: 1px 5px;
  margin-top: 3px;
  border-radius: 5px;
  background-color: #eeeeee;
  width: fit-content;
}

.warn-icon {
  font-size: 10px;
  color: #ff8b07;
}

.warn-text {
  position: relative;
  top: -1px;
  font-size: 11px;
  font-weight: normal;
  color: #ff8b07;
  display: flex;
  align-items: center;
  justify-content: center;
}
.test-container {
  text-align: right;
}

.test-button {
  margin-right: 5px;
  padding: 0;
  background-color: rgba(33, 150, 243, 0);
  color: #717171;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}
</style>