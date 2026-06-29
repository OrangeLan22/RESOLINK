<template>
  <div class="tab-container scrollbar">
    
    <!-- 无权限提示页面 -->
    <NoPermission v-if="!hasPermission" />
    
    <!-- 正常内容页面 -->
    <div v-else class="tab-main flex">
      <div class="top-menu">
        <h3
            class="item"
            :class="{ 'active': localSelectedTab === 'space' }"
            @click="handleTabClick('space')"
        >
          空间资源
        </h3>
        <h3
            class="item"
            :class="{ 'active': localSelectedTab === 'physical' }"
            @click="handleTabClick('physical')"
        >
          实物资源
        </h3>
      </div>

      <div class="content">
        <div v-if="localSelectedTab === 'space'">
          <SpaceResource />
        </div>
        <div v-if="localSelectedTab === 'physical'">
          <PhysicalResource />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {watch, ref, computed, onMounted} from "vue";
import { ElMessage } from "element-plus";
import SpaceResource from "./ResourceManagement/SpaceResource.vue";
import PhysicalResource from "./ResourceManagement/PhysicalResource.vue";
import NoPermission from "./Common/NoPermission.vue";
import { useResourcesStore } from "../stores/resources";

// 获取资源store实例
const resourcesStore = useResourcesStore()

// 权限检查
const hasPermission = computed(() => resourcesStore.$state.hasPermission)

// 定义props
const props = defineProps({
  selectedTab: {
    type: String,
    default: 'space'
  }
})

// 本地选中的选项卡
const localSelectedTab = ref(props.selectedTab);

// 定义emits
const emit = defineEmits(['tabSelect'])

// 组件挂载时获取资源数据
onMounted(() => {
  fetchResources()
})

// 获取资源列表
const fetchResources = async () => {
  try {
    // 同时获取空间资源和实物资源数据
    await Promise.all([
      resourcesStore.fetchResources(),
      resourcesStore.fetchPhysicalResources()
    ])
  } catch (error) {
    // 错误已经在store中处理，这里不需要额外处理
    console.error('获取资源数据失败:', error)
  }
}

// 监听props变化，更新本地状态
watch(() => props.selectedTab, (newVal) => {
  localSelectedTab.value = newVal;
});

// 处理选项卡点击
function handleTabClick(tabType) {
  localSelectedTab.value = tabType;
  emit('tabSelect', tabType);
}
</script>

<style scoped>
* {
  user-select: none;
}
.flex {
  display: flex;
  flex-direction: column;
}

.top-menu {
  display: flex;
  gap: 20px;
  h3 {
    margin: 0;
  }
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

.content {
  margin-top: 20px;
}
</style>