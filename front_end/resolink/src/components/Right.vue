<template>
  <div class="top">
    <label class="company-name">{{CompanyName}}</label>

    <div class="personal-info">
      <div class="info-card">
        <FontAwesomeIcon :icon="['fas', 'building-user']" class="icons"/>
        <label >{{DepartmentName}}</label>
      </div>
      <div class="divider"></div>
      <div class="info-card">
        <FontAwesomeIcon :icon="['fas', 'user']" class="icons"/>
        <label >{{UserName}}</label>
      </div>
    </div>
  </div>

  <div class="tabs">
    <button class="refresh-btn" @click="handleRefresh" title="重载当前页面">
      <FontAwesomeIcon :icon="['fas', 'arrows-rotate']" class="refresh-icon"/>
    </button>
    <div
      v-for="tab in homeTabs"
      :key="tab.id"
      class="tab"
      :class="{ 'tab-active': selectedTab === tab.id }"
      @click="handleTabClick(tab.id)"
    >
      <FontAwesomeIcon :icon="['fas', tab.icon]" class="tab-icon"/>
      <label>{{tab.title}}</label>
    </div>

    <div
      v-if="contentTabs.length > 0"
      class="tab-divider"
    ></div>

    <div
      class="scrollable-tabs"
      v-if="contentTabs.length > 0"
      @wheel="handleWheel"
    >

      <div
        @click="handleTabClick(tab.id)"
        v-for="tab in contentTabs"
        :key="tab.id"
        class="tab"
        style="margin-right: 10px"
        :class="{ 'tab-active': selectedTab === tab.id }"
      >
        <div class="tab-content">
          <FontAwesomeIcon :icon="['fas', tab.icon]" class="tab-icon"/>
          <label>{{tab.title}}</label>
        </div>

        <button
          v-if="tab.closable"
          class="close-btn"
          @click.stop="handleTabClose(tab.id)"
        >
          <FontAwesomeIcon :icon="['fas', 'xmark']" class="close-icon"/>
        </button>
      </div>
    </div>
  </div>

  <div class="content">
    <Home v-show="selectedTab === 'home'" :key="componentKeys.home" />
    <SpaceBooking v-show="selectedTab === 'space-core'" :key="componentKeys['space-core']" :tag-type="'space-core'" />
    <SpaceBooking v-show="selectedTab === 'space-public'" :key="componentKeys['space-public']" :tag-type="'space-public'" />
    <SpaceBooking v-show="selectedTab === 'space-special'" :key="componentKeys['space-special']" :tag-type="'space-special'" />
    <AssetBooking v-show="selectedTab === 'asset-device'" :key="componentKeys['asset-device']" :tag-type="'asset-device'" />
    <AssetBooking v-show="selectedTab === 'asset-supplies'" :key="componentKeys['asset-supplies']" :tag-type="'asset-supplies'" />
    <AssetBooking v-show="selectedTab === 'asset-transport'" :key="componentKeys['asset-transport']" :tag-type="'asset-transport'" />
    <AccountManagement v-show="selectedTab === 'account'" :key="componentKeys.account" />
    <SpaceResourceManagement v-show="selectedTab === 'space-inventory'" :key="componentKeys['space-inventory']" />
    <PhysicalResourceManagement v-show="selectedTab === 'physical-inventory'" :key="componentKeys['physical-inventory']" />
    <BusManagement v-show="selectedTab === 'bus-management'" :key="componentKeys['bus-management']" />
    <ApprovalManagement v-show="selectedTab === 'approval'" :key="componentKeys.approval" />
    <ResourceInspection v-show="selectedTab === 'inspection'" :key="componentKeys.inspection" />
    <PublicManagement v-show="selectedTab === 'public'" :key="componentKeys.public" />
    <History v-show="selectedTab === 'history'" :key="componentKeys.history" ref="historyRef" />
  </div>
  
  <!-- 账号设置弹窗 -->
  <AccountSettings
    v-model:visible="accountSettingsVisible"
  />
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {FontAwesomeIcon} from "@fortawesome/vue-fontawesome"
import { library } from '@fortawesome/fontawesome-svg-core'
// 引入公共信息store和用户信息store
import { usePublicInfoStore } from '../stores/publicInfo'
import { useUserInfoStore } from '../stores/userInfo'

import { faBuildingUser, faUser, faHouse, faPeopleRoof, faComputer, faXmark, faArrowsRotate, faInfoCircle, faClockRotateLeft, faGear, faBuilding, faUsers, faStar, faDesktop, faPenToSquare, faCar, faBus, faClipboardCheck, faClipboardList } from '@fortawesome/free-solid-svg-icons'

// 导入所有组件
import SpaceBooking from './SpaceBooking.vue'
import AssetBooking from './AssetBooking.vue'
import Home from './Home.vue'
import AccountManagement from './AccountManagement.vue'
import SpaceResourceManagement from './SpaceResourceManagement.vue'
import PhysicalResourceManagement from './PhysicalResourceManagement.vue'
import BusManagement from './BusManagement.vue'
import ApprovalManagement from './ApprovalManagement.vue'
import ResourceInspection from './ResourceInspection.vue'
import PublicManagement from './PublicManagement.vue'
import History from './History.vue'
import AccountSettings from './home/AccountSettings.vue'

const props = defineProps({
  tabs: {
    type: Array,
    required: true
  },
  selectedTab: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['tabSelect', 'tabClose', 'refresh'])

// 将图标添加到库中
library.add(faBuildingUser, faUser, faHouse, faPeopleRoof, faComputer, faXmark, faArrowsRotate, faInfoCircle, faClockRotateLeft, faGear, faBuilding, faUsers, faStar, faDesktop, faPenToSquare, faCar, faBus, faClipboardCheck, faClipboardList)

// 获取公共信息store和用户信息store
const publicInfoStore = usePublicInfoStore()
const userInfoStore = useUserInfoStore()

// 从store中获取companyName
const CompanyName = computed(() => {
  return publicInfoStore.publicData?.[0]?.companyName || 'CompanyName'
})

// 从用户信息store中获取部门和姓名
const DepartmentName = computed(() => {
  return userInfoStore.getUserInfo?.depName || '无部门'
})

const UserName = computed(() => {
  return userInfoStore.getUserInfo?.name || 'Name'
})

// 为每个组件设置唯一的key，用于强制重新渲染
const componentKeys = ref({
  home: 0,
  'space-core': 0,
  'space-public': 0,
  'space-special': 0,
  'asset-device': 0,
  'asset-supplies': 0,
  'asset-transport': 0,
  account: 0,
  'space-inventory': 0,
  'physical-inventory': 0,
  'bus-management': 0,
  approval: 0,
  inspection: 0,
  public: 0,
  history: 0
})

// 组件引用
const historyRef = ref(null)

// 账号设置弹窗状态
const accountSettingsVisible = ref(false)

// 处理账号设置点击
function handleAccountSettings() {
  accountSettingsVisible.value = true
}

// 计算属性：首页标签
const homeTabs = computed(() => {
  return props.tabs.filter(tab => tab.id === 'home')
})

// 计算属性：内容标签
const contentTabs = computed(() => {
  return props.tabs.filter(tab => tab.id !== 'home')
})

function handleTabClick(tabId) {
  emit('tabSelect', tabId)
}

function handleTabClose(tabId) {
  emit('tabClose', tabId)
}

// 横向滚动
function handleWheel(event) {
  // 阻止默认的纵向滚动行为
  event.preventDefault()
  // 实现横向滚动
  event.currentTarget.scrollBy({
    left: event.deltaY,
    behavior: 'smooth'
  })
}

// 刷新方法
function handleRefresh() {
  if (props.selectedTab === 'home') {
    componentKeys.value.home++
  } else if (props.selectedTab === 'space-core') {
    componentKeys.value['space-core']++
  } else if (props.selectedTab === 'space-public') {
    componentKeys.value['space-public']++
  } else if (props.selectedTab === 'space-special') {
    componentKeys.value['space-special']++
  } else if (props.selectedTab === 'asset-device') {
    componentKeys.value['asset-device']++
  } else if (props.selectedTab === 'asset-supplies') {
    componentKeys.value['asset-supplies']++
  } else if (props.selectedTab === 'asset-transport') {
    componentKeys.value['asset-transport']++
  } else if (props.selectedTab === 'account') {
    componentKeys.value.account++
  } else if (props.selectedTab === 'space-inventory') {
    componentKeys.value['space-inventory']++
  } else if (props.selectedTab === 'physical-inventory') {
    componentKeys.value['physical-inventory']++
  } else if (props.selectedTab === 'bus-management') {
    componentKeys.value['bus-management']++
  } else if (props.selectedTab === 'approval') {
    componentKeys.value.approval++
  } else if (props.selectedTab === 'inspection') {
    componentKeys.value.inspection++
  } else if (props.selectedTab === 'public') {
    componentKeys.value.public++
  } else if (props.selectedTab === 'history') {
    // 对于历史记录组件，直接调用其刷新方法而不是重新渲染
    if (historyRef.value && historyRef.value.refresh) {
      historyRef.value.refresh()
    } else {
      componentKeys.value.history++
    }
  }
  
  // 触发刷新事件，通知父组件
  emit('refresh', props.selectedTab)
}
</script>

<style scoped>
.company-name,
.personal-info label,
.tab label {
  white-space: nowrap;
  text-overflow: ellipsis;
}

.top {
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #ffffff;
  height: 35px;
  padding: 10px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
}

.company-name {
  font-size: 19px;
  font-weight: bold;
  color: #252525;
}

.personal-info {
  display: flex;
  align-items: center;
  justify-items: left;
}

.info-card {
  display: flex;
  align-items: center;
  label {
    font-size: 15px;
    color: #252525;
  }
}

.divider {
  width: 2px;
  height: 20px;
  background-color: #ccc;
  margin: 0 15px;
}

.icons {
  margin-right: 5px;
  color: rgba(0, 0, 0, 0.5);
}

.tabs {
  display: flex;
  align-items: center;
  height: 30px;
  background-color: #ffffff;
  padding: 5px 20px;
  box-shadow: 0 2px 3px rgba(0, 0, 0, 0.1);
  flex-wrap: nowrap;
  align-content: flex-start;
  position: relative;
  z-index: 10;
}

.scrollable-tabs {
  flex: 1;
  height: 100%;
  overflow-x: hidden;
  overflow-y: hidden;
  display: flex;
  align-items: center;
  min-width: 0;
}

.tab {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 5px 8px;
  border-radius: 3px;
  background: rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
  cursor: pointer;
  flex-shrink: 0;

  .tab-content {
    display: flex;
    align-items: center;
  }

  label {
    user-select: none;
    font-size: 14px;
    color: rgba(0, 0, 0, 0.8);
    cursor: pointer;
  }

  .tab-icon {
    font-size: 12px;
    margin-right: 5px;
    color: rgba(0, 0, 0, 0.5);
  }

  .close-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 16px;
    height: 16px;
    margin-left: 5px;
    padding: 0;
    border: none;
    background: transparent;
    cursor: pointer;
    border-radius: 2px;

    .close-icon {
      font-size: 10px;
      color: rgba(0, 0, 0, 0.5);
    }

    &:hover {
      background: rgba(255, 0, 0, 0.1);

      .close-icon {
        color: #d32f2f;
      }
    }
  }
}

.tab:not(.tab-active):hover {
  background: rgba(63, 81, 181, 0.3);
  label {
    color: rgba(0, 0, 0, 0.8);
  }
  .tab-icon {
    color: rgba(0, 0, 0, 0.5);
  }
}

.tab-active {
  background: rgba(63, 81, 181, 0.9);
  label {
    color: #fff;
  }
  .tab-icon {
    color: #fff;
  }

  .close-btn {
    .close-icon {
      color: rgba(255, 255, 255, 0.8);
    }

    &:hover {
      background: rgba(255, 255, 255, 0.2);

      .close-icon {
        color: #fff;
      }
    }
  }
}

.tab-divider {
  width: 1px;
  height: 20px;
  background-color: #ccc;
  margin: 0 10px;
  flex-shrink: 0;
}

.content {
  height: calc(100vh - 95.667px);
}

.refresh-btn {
  margin-right: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.refresh-btn:hover {
  background: rgba(63, 81, 181, 0.1);
}

.refresh-icon {
  font-size: 16px;
  color: rgba(0, 0, 0, 0.5);
  transition: all 0.2s ease;
}

.refresh-btn:hover .refresh-icon {
  color: rgba(63, 81, 181, 0.9);
  transform: rotate(180deg);
}

.refresh-btn:active .refresh-icon {
  transform: rotate(360deg) scale(0.9);
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background-color: #ff4d4f;
  color: white;
  border-radius: 10px;
  min-width: 16px;
  height: 16px;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.account-settings {
  cursor: pointer;
  transition: all 0.2s ease;
}

.account-settings:hover {
  background: rgba(63, 81, 181, 0.1);
}
</style>