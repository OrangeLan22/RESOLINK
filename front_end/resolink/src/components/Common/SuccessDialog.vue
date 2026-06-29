<template>
  <!-- 自定义成功弹窗 -->
  <div v-if="visible" class="success-dialog-overlay">
    <div class="success-dialog">
      <div class="success-dialog-content">
        <p v-html="message"></p>
        <p class="password"> {{ password }} </p>
      </div>

      <div class="success-dialog-footer">
        <el-button type="primary" @click="handleClose">复制并关闭</el-button>
      </div>
    </div>

    <div class="warn">
      <span class="warn-text">密码仅显示一次，注意及时保存</span>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'

// 定义props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  useracc: {
    type: String,
    default: ''
  },
  password: {
    type: String,
    default: ''
  },
  message: {
    type: String,
    default: '用户的初始密码为'
  }
})

// 定义emit事件
const emit = defineEmits(['close'])

// 监听visible变化，重置动画状态
watch(() => props.visible, (newVal) => {
  if (newVal) {
    // 重置动画类
    const dialogOverlay = document.querySelector('.success-dialog-overlay')
    if (dialogOverlay) {
      dialogOverlay.classList.remove('fade-out')
    }
  }
})

// 关闭成功弹窗
const handleClose = async () => {
  // 复制信息到剪贴板
  const copyText = `用户名：${props.useracc}，密码：${props.password}`
  try {
    await navigator.clipboard.writeText(copyText)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    console.error('复制失败:', err)
    ElMessage.error('复制失败，请手动复制')
  }
  
  // 获取弹窗元素
  const dialogOverlay = document.querySelector('.success-dialog-overlay')
  if (dialogOverlay) {
    // 添加渐出动画类
    dialogOverlay.classList.add('fade-out')
    
    // 等待动画结束后关闭弹窗
    setTimeout(() => {
      emit('close')
    }, 300) // 与动画时间一致
  } else {
    // 无法获取元素时直接关闭
    emit('close')
  }
}
</script>

<style scoped>
/* 自定义成功弹窗样式 */
.success-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(0px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
  flex-direction: column;
  animation: dialog-overlay-fade-in 0.3s ease forwards;
}

.success-dialog-overlay.fade-out {
  animation: dialog-overlay-fade-out 0.3s ease forwards;
}

.success-dialog {
  background-color: white;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-width: 400px;
  width: 100%;
  transform: scale(0.8);
  opacity: 0;
  animation: dialog-fade-in 0.3s ease forwards;
}

.success-dialog-overlay.fade-out .success-dialog {
  animation: dialog-fade-out 0.3s ease forwards;
}

/* 渐入动画 */
@keyframes dialog-overlay-fade-in {
  to {
    backdrop-filter: blur(10px);
  }
}

@keyframes dialog-fade-in {
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 渐出动画 */
@keyframes dialog-overlay-fade-out {
  from {
    backdrop-filter: blur(10px);
  }
  to {
    backdrop-filter: blur(0px);
    opacity: 0;
  }
}

@keyframes dialog-fade-out {
  from {
    transform: scale(1);
    opacity: 1;
  }
  to {
    transform: scale(0.8);
    opacity: 0;
  }
}

.success-dialog-content {
  margin-bottom: 10px;
}

.success-dialog-content p {
  margin: 0;
  font-size: 16px;
  line-height: 1.5;
}

.success-dialog-content strong {
  color: #333;
}

.success-dialog-footer {
  display: flex;
  justify-content: center;
}

.success-dialog-footer .el-button {
  min-width: 100px;
}

.password{
  margin: 30px 0 !important;
  letter-spacing: 5px;
  text-align: center;
  font-size: 40px !important;
  font-weight: bold;
  color: #000000;
}

.warn {
  display: flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 5px;
  background-color: #eeeeee;
  width: fit-content;
  margin-top: 20px;
}

.warn-text {
  position: relative;
  top: -1px;
  font-size: 13px;
  font-weight: normal;
  color: #ff8b07;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-button--primary) {
  background-color: #3f51b5;
  border-color: #3f51b5;
}
</style>