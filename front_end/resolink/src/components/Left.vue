<template>
  <div class="left-container">
    <!-- logo容器 -->
    <div class="logo-container">
      <div class="logo">
        <h2>Resolink</h2>
      </div>
    </div>
    
    <!-- 菜单容器外层 - 用于放置渐变遮罩 -->
    <div class="menu-wrapper">
      <!-- 滚动渐变遮罩 - 提示用户上方有内容 -->
      <div 
        class="scroll-gradient top"
        :class="{ 'hide': scrollTop === 0 }"
      ></div>
      <!-- 滚动渐变遮罩 - 提示用户下方有内容 -->
      <div 
        class="scroll-gradient bottom"
        :class="{ 'hide': isAtBottom }"
      ></div>
      
      <!-- 菜单容器内层 - 实际滚动区域 -->
      <div 
        class="menu-container"
        ref="menuContainer"
        @scroll="handleScroll"
      >
        <div class="menu-content">
          <div
            id="space"
            class="card"
            @click="toggleSpaceOptions"
          >
            <div class="father-card">
              <div>
                <FontAwesomeIcon :icon="['fas', 'people-roof']" class="icons"/>
                <label>空间资源预约</label>
              </div>
              <FontAwesomeIcon
                :icon="['fas', spaceOptionsExpanded ? 'chevron-down' : 'chevron-right']"
                class="expand-icon"
              />
            </div>
          </div>

          <!-- 空间资源预约展开列表 -->
          <div v-if="spaceOptionsExpanded" class="space-options">
            <div
                class="card"
                :class="{ 'card-active': selectedCard === 'space-core' }"
                @click="handleCardClick('space-core')"
            >
              <FontAwesomeIcon :icon="['fas', 'building']" class="icons"/>
              <label>核心业务空间</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'space-public' }"
              @click="handleCardClick('space-public')"
            >
              <FontAwesomeIcon :icon="['fas', 'users']" class="icons"/>
              <label>公共辅助空间</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'space-special' }"
              @click="handleCardClick('space-special')"
            >
              <FontAwesomeIcon :icon="['fas', 'star']" class="icons"/>
              <label>特殊功能空间</label>
            </div>
          </div>

          <div
            id="asset"
            class="card"
            @click="toggleAssetOptions"
          >
            <div class="father-card">
              <div>
                <FontAwesomeIcon :icon="['fas', 'computer']" class="icons"/>
                <label>实物资产预约</label>
              </div>
              <FontAwesomeIcon
                :icon="['fas', assetOptionsExpanded ? 'chevron-down' : 'chevron-right']"
                class="expand-icon"
              />
            </div>
          </div>

          <!-- 实物资产预约展开列表 -->
          <div v-if="assetOptionsExpanded" class="asset-options">
            <div
                class="card"
                :class="{ 'card-active': selectedCard === 'asset-device' }"
                @click="handleCardClick('asset-device')"
            >
              <FontAwesomeIcon :icon="['fas', 'desktop']" class="icons"/>
              <label>办公设备</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'asset-supplies' }"
              @click="handleCardClick('asset-supplies')"
            >
              <FontAwesomeIcon :icon="['fas', 'pen-to-square']" class="icons"/>
              <label>办公耗材</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'asset-transport' }"
              @click="handleCardClick('asset-transport')"
            >
              <FontAwesomeIcon :icon="['fas', 'car']" class="icons"/>
              <label>交通出行</label>
            </div>
          </div>

          <!-- 管理员选项 -->
          <div
            id="admin"
            class="card"
            @click="toggleAdminOptions"
          >
            <div class="father-card">
              <div>
                <FontAwesomeIcon :icon="['fas', 'user-shield']" class="icons"/>
                <label>管理员选项</label>
              </div>
              <FontAwesomeIcon
                :icon="['fas', adminOptionsExpanded ? 'chevron-down' : 'chevron-right']"
                class="expand-icon"
              />
            </div>
          </div>

          <!-- 管理员选项展开列表 -->
          <div v-if="adminOptionsExpanded" class="admin-options">
            <div
                class="card"
                :class="{ 'card-active': selectedCard === 'public' }"
                @click="handleCardClick('public')"
            >
              <FontAwesomeIcon :icon="['fas', 'info-circle']" class="icons"/>
              <label>公共信息</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'account' }"
              @click="handleCardClick('account')"
            >
              <FontAwesomeIcon :icon="['fas', 'user-cog']" class="icons"/>
              <label>账号管理</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'space-inventory' }"
              @click="handleCardClick('space-inventory')"
            >
              <FontAwesomeIcon :icon="['fas', 'building']" class="icons"/>
              <label>空间资源管理</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'physical-inventory' }"
              @click="handleCardClick('physical-inventory')"
            >
              <FontAwesomeIcon :icon="['fas', 'box']" class="icons"/>
              <label>实物资源管理</label>
            </div>

<!--            <div-->
<!--              class="card"-->
<!--              :class="{ 'card-active': selectedCard === 'bus-management' }"-->
<!--              @click="handleCardClick('bus-management')"-->
<!--            >-->
<!--              <FontAwesomeIcon :icon="['fas', 'bus']" class="icons"/>-->
<!--              <label>班车管理</label>-->
<!--            </div>-->

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'approval' }"
              @click="handleCardClick('approval')"
            >
              <FontAwesomeIcon :icon="['fas', 'clipboard-check']" class="icons"/>
              <label>预约审批</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'inspection' }"
              @click="handleCardClick('inspection')"
            >
              <FontAwesomeIcon :icon="['fas', 'clipboard-list']" class="icons"/>
              <label>资源检查</label>
            </div>

            <div
              class="card"
              :class="{ 'card-active': selectedCard === 'history' }"
              @click="handleCardClick('history')"
            >
              <FontAwesomeIcon :icon="['fas', 'clock-rotate-left']" class="icons"/>
              <label>历史记录</label>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import { library } from '@fortawesome/fontawesome-svg-core'
import { faPeopleRoof, faComputer, faChevronDown, faChevronRight, faUserCog, faBox, faUserShield, faInfoCircle, faClockRotateLeft, faBuilding, faUsers, faStar, faDesktop, faPenToSquare, faCar, faBus, faClipboardCheck, faClipboardList } from '@fortawesome/free-solid-svg-icons'

// 定义props
const props = defineProps({
  selectedCard: {
    type: String,
    default: ''
  }
})

// 定义emits
const emit = defineEmits(['cardSelect'])

// 管理员选项展开状态
const adminOptionsExpanded = ref(true)
// 空间资源预约展开状态
const spaceOptionsExpanded = ref(true)
// 实物资产预约展开状态
const assetOptionsExpanded = ref(true)

// 菜单容器引用
const menuContainer = ref(null)
// 滚动位置
const scrollTop = ref(0)
// 是否在底部
const isAtBottom = ref(false)

// 检查是否滚动到底部
const checkIsAtBottom = () => {
  if (!menuContainer.value) {
    isAtBottom.value = false
    return
  }
  const container = menuContainer.value
  // 滚动到底部的判断：scrollTop + 可视高度 >= 总高度
  isAtBottom.value = container.scrollTop + container.clientHeight >= container.scrollHeight - 1
}

// 将图标添加到库中
library.add(faPeopleRoof, faComputer, faChevronDown, faChevronRight, faUserCog, faBox, faUserShield, faInfoCircle, faClockRotateLeft, faBuilding, faUsers, faStar, faDesktop, faPenToSquare, faCar, faBus, faClipboardCheck, faClipboardList)

// 处理卡片点击
function handleCardClick(cardType) {
  emit('cardSelect', cardType)
}

// 切换管理员选项展开/收缩
function toggleAdminOptions() {
  adminOptionsExpanded.value = !adminOptionsExpanded.value
  // 菜单展开/收缩后检查滚动状态
  setTimeout(checkIsAtBottom, 100)
}

// 切换空间资源预约展开/收缩
function toggleSpaceOptions() {
  spaceOptionsExpanded.value = !spaceOptionsExpanded.value
  // 菜单展开/收缩后检查滚动状态
  setTimeout(checkIsAtBottom, 100)
}

// 切换实物资产预约展开/收缩
function toggleAssetOptions() {
  assetOptionsExpanded.value = !assetOptionsExpanded.value
  // 菜单展开/收缩后检查滚动状态
  setTimeout(checkIsAtBottom, 100)
}

// 处理滚动事件
function handleScroll() {
  if (menuContainer.value) {
    scrollTop.value = menuContainer.value.scrollTop
    checkIsAtBottom()
  }
}

// 组件挂载时初始化检查
onMounted(() => {
  checkIsAtBottom()
})
</script>

<style scoped>
.left-container {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo-container {
  flex-shrink: 0;
}

.logo {
  text-align: center;
  padding: 10px 5px;
  border-bottom: 1px solid #b1b1b1;
}

h2 {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 35px;
  color: #fff;
  margin: 0;
}

/* 菜单容器外层 - 用于放置渐变遮罩 */
.menu-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
}

/* 菜单容器内层 - 实际滚动区域 */
.menu-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 隐藏滚动条 */
.menu-container::-webkit-scrollbar {
  display: none;
}

.menu-container {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* 滚动渐变遮罩 - 固定在menu-wrapper上，不随内容滚动 */
.scroll-gradient {
  position: absolute;
  left: 0;
  right: 0;
  height: 30px;
  pointer-events: none;
  transition: opacity 0.3s ease;
  z-index: 10;
}

/* 顶部渐变：从透明渐变到背景色，提示用户上方有内容 */
.scroll-gradient.top {
  top: 0;
  height: 40px;
  background: linear-gradient(to top, transparent, #3f51b5);
}

/* 底部渐变：从透明渐变到背景色，提示用户下方有内容 */
.scroll-gradient.bottom {
  bottom: 0;
  height: 40px;
  background: linear-gradient(to bottom, transparent, #3f51b5);
}

/* 当滚动到顶部或底部时隐藏对应方向的渐变 */
.scroll-gradient.hide {
  opacity: 0;
}

.menu-content {
  padding: 0;
}

.icons {
  margin-right: 8px;
  color: white;
}

.card {
  cursor: pointer;
  padding: 10px 15px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  label {
    cursor: pointer;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
  }
}

.card:not(.card-active):hover {
  background-color: rgba(255, 255, 255, 0.08);
}

/* 添加选中状态的样式 */
.card-active {
  background-color: rgba(255, 255, 255, 0.15);
}

/* 展开/收缩图标样式 */
.expand-icon {
  color: white;
  font-size: 12px;
}

.father-card {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>