<template>
  <div class="agent">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="header-left">
        <div class="bot-avatar">
          <svg class="bot-icon" viewBox="0 0 24 24" width="32" height="32">
            <path fill="currentColor" d="M12,2A2,2 0 0,1 14,4C14,4.74 13.6,5.39 13,5.73V7H14A7,7 0 0,1 21,14H22A1,1 0 0,1 23,15V18A1,1 0 0,1 22,19H21V20A2,2 0 0,1 19,22H5A2,2 0 0,1 3,20V19H2A1,1 0 0,1 1,18V15A1,1 0 0,1 2,14H3A7,7 0 0,1 10,7H11V5.73C10.4,5.39 10,4.74 10,4A2,2 0 0,1 12,2M7.5,13A2.5,2.5 0 0,0 5,15.5A2.5,2.5 0 0,0 7.5,18A2.5,2.5 0 0,0 10,15.5A2.5,2.5 0 0,0 7.5,13M16.5,13A2.5,2.5 0 0,0 14,15.5A2.5,2.5 0 0,0 16.5,18A2.5,2.5 0 0,0 19,15.5A2.5,2.5 0 0,0 16.5,13Z"/>
          </svg>
        </div>
        <div class="header-info">
          <div class="bot-name">RESOLINK 智能体</div>
          <div class="status">
            <span :class="['status-dot', agentStore.isConnected ? 'online' : 'offline']"></span>
            <span class="status-text">{{ agentStore.isConnected ? '在线' : '离线' }}</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <button v-if="!agentStore.isConnected" class="reconnect-btn" @click="connectWebSocket" :disabled="agentStore.isConnecting">
          <svg class="reconnect-icon" viewBox="0 0 24 24" width="18" height="18">
            <path fill="currentColor" d="M17.65,6.35C16.2,4.9 14.21,4 12,4A8,8 0 0,0 4,12A8,8 0 0,0 12,20C15.73,20 18.84,17.45 19.73,14H17.65C16.83,16.33 14.61,18 12,18A6,6 0 0,1 6,12A6,6 0 0,1 12,6C13.66,6 15.14,6.69 16.22,7.78L13,11H20V4L17.65,6.35Z"/>
          </svg>
          {{ agentStore.isConnecting ? '连接中...' : '重新连接' }}
        </button>
        <button class="new-chat-btn" @click="startNewChat">
          <svg class="new-chat-icon" viewBox="0 0 24 24" width="18" height="18">
            <path fill="currentColor" d="M19,13H13V19H11V13H5V11H11V5H13V11H19V13Z"/>
          </svg>
          新对话
        </button>
      </div>
    </div>

    <!-- 聊天内容区域 -->
    <div class="chat-content scrollbar" ref="chatContent">
      <div class="welcome-message" v-if="agentStore.messages.length === 0">
        <div class="welcome-icon">
          <svg viewBox="0 0 24 24" width="48" height="48">
            <path fill="currentColor" d="M12,2A2,2 0 0,1 14,4C14,4.74 13.6,5.39 13,5.73V7H14A7,7 0 0,1 21,14H22A1,1 0 0,1 23,15V18A1,1 0 0,1 22,19H21V20A2,2 0 0,1 19,22H5A2,2 0 0,1 3,20V19H2A1,1 0 0,1 1,18V15A1,1 0 0,1 2,14H3A7,7 0 0,1 10,7H11V5.73C10.4,5.39 10,4.74 10,4A2,2 0 0,1 12,2M7.5,13A2.5,2.5 0 0,0 5,15.5A2.5,2.5 0 0,0 7.5,18A2.5,2.5 0 0,0 10,15.5A2.5,2.5 0 0,0 7.5,13M16.5,13A2.5,2.5 0 0,0 14,15.5A2.5,2.5 0 0,0 16.5,18A2.5,2.5 0 0,0 19,15.5A2.5,2.5 0 0,0 16.5,13Z"/>
          </svg>
        </div>
        <div class="welcome-text">
          <h3>您好！我是预约助手</h3>
          <p>我可以帮助您进行各种预约服务，请问有什么可以帮您的吗？</p>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="messages-container" v-else ref="messagesContainer">
        <div
          v-for="(message, index) in agentStore.messages"
          :key="index"
          :class="['message', message.type === 'user' ? 'user-message' : 'bot-message']"
        >
          <div class="message-content">
            <!-- 机器人消息的思考状态标题 -->
            <div v-if="message.type === 'bot' && (message.isThinking || message.thinkingSteps?.length > 0)" class="thinking-header">
              <div class="thinking-status-container">
                <span class="thinking-status">
                  {{ message.isThinking ? '思考中...' : '思考完成' }}
                </span>
                <div v-if="message.isThinking" class="thinking-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
              
              <!-- 思考步骤展开/收起按钮 -->
              <button 
                v-if="!message.isThinking && message.thinkingSteps?.length > 0"
                class="toggle-steps-btn"
                @click="toggleThinkingSteps(index)"
              >
                {{ message.showThinkingSteps ? '收起' : '展开' }}
              </button>
            </div>
            
            <!-- 思考步骤（可展开收起） -->
            <div 
              v-if="message.type === 'bot' && message.thinkingSteps?.length > 0 && message.showThinkingSteps" 
              class="thinking-steps"
            >
              <div
                v-for="(step, stepIndex) in message.thinkingSteps"
                :key="stepIndex"
                class="thinking-step"
              >
                <div class="step-icon" :class="{ 'active': step.status === 'active', 'completed': step.status === 'completed' }">
                  {{ step.status === 'active' ? '●' : '✓' }}
                </div>
                <span class="step-text" :class="{ 'active': step.status === 'active', 'completed': step.status === 'completed' }">{{ step.description }}</span>
              </div>
            </div>
            
            <!-- 最终回复内容 -->
            <div v-if="message.type === 'bot' && message.content" class="final-response">
              <div v-html="renderMarkdown(message.content)"></div>
            </div>
            
            <!-- 用户消息内容 -->
            <div v-if="message.type === 'user'">
              {{ message.content }}
            </div>
          </div>
          <div class="message-time">{{ message.time }}</div>
        </div>
      </div>
    </div>

    <!-- 语音识别状态指示器 -->
    <div v-if="isRecording" class="recording-indicator">
      <div class="pulse-animation"></div>
      <span class="recording-text">正在聆听...</span>
      <span v-if="interimTranscript" class="interim-text">{{ interimTranscript }}</span>
    </div>

    <!-- 输入区域 -->
    <div class="bottom">
      <div class="text-input">
        <div
          class="voice"
          @mousedown="startRecording"
          @mouseup="stopRecording"
          @mouseleave="stopRecording"
          @touchstart="startRecording"
          @touchend="stopRecording"
        >
          <svg
            v-if="!isRecording"
            class="mic-icon"
            viewBox="0 0 24 24"
            width="20"
            height="20"
          >
            <path fill="currentColor" d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
            <path fill="currentColor" d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
          </svg>
          <svg
            v-else
            class="mic-icon recording"
            viewBox="0 0 24 24"
            width="20"
            height="20"
          >
            <path fill="currentColor" d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z"/>
            <path fill="currentColor" d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z"/>
          </svg>
        </div>
        <div class="text-input-box">
          <input
            type="text"
            placeholder="输入您的需求..."
            class="input-field"
            v-model="inputText"
            @keyup.enter="sendMessage"
          />
        </div>
        <div
          class="send-button"
          @click="sendMessage"
        >
          <svg
            class="send-icon"
            viewBox="0 0 24 24"
            width="18"
            height="18"
          >
            <path fill="currentColor" d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import MarkdownIt from 'markdown-it'
import { useAIAgentStore } from '../../stores/aiAgent'

// 初始化 markdown-it 实例
const md = new MarkdownIt({
  html: false, // 禁用 HTML 标签
  breaks: true, // 将 \n 转换为 <br>
  linkify: true, // 自动将 URL 转换为链接
  typographer: true // 启用一些排版替换
})

// Markdown 渲染函数
const renderMarkdown = (content) => {
  if (!content) return ''
  return md.render(content)
}

// 使用全局 AI 状态
const agentStore = useAIAgentStore()
const { connectWebSocket, sendMessageToWebSocket, addUserMessage, addBotMessage, toggleThinkingSteps, startNewChat } = agentStore

const isRecording = ref(false)
const inputText = ref('')
const recognition = ref(null)
const isSpeechSupported = ref(false)
const interimTranscript = ref('')
const chatContent = ref(null) // 聊天内容容器引用

// 滚动到底部
const scrollToBottom = () => {
  if (chatContent.value) {
    // 使用 nextTick 确保 DOM 已经更新
    nextTick(() => {
      chatContent.value.scrollTop = chatContent.value.scrollHeight
    })
  }
}

// 监听消息变化，自动滚动到底部
import { watch } from 'vue'

watch(() => agentStore.messages.length, () => {
  scrollToBottom()
})

// 发送消息
const sendMessage = () => {
  if (inputText.value.trim()) {
    // 添加用户消息，标记为未处理状态
    addUserMessage(inputText.value.trim(), false)
    
    // 使用 WebSocket 发送消息
    if (sendMessageToWebSocket(inputText.value.trim())) {
      inputText.value = ''
    }
  }
}

// 组件挂载时初始化语音识别和 WebSocket 连接
onMounted(() => {
  // 初始化 WebSocket 连接
  connectWebSocket()
  
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition

  if (SpeechRecognition) {
    isSpeechSupported.value = true
    recognition.value = new SpeechRecognition()

    // 配置语音识别
    recognition.value.continuous = false
    recognition.value.interimResults = true
    recognition.value.lang = 'zh-CN'

    // 处理识别结果
    recognition.value.onresult = (event) => {
      let finalTranscript = ''
      interimTranscript.value = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          finalTranscript += transcript
        } else {
          interimTranscript.value += transcript
        }
      }

      if (finalTranscript) {
        inputText.value = finalTranscript
        interimTranscript.value = ''
      }
    }

    // 处理识别结束
    recognition.value.onend = () => {
      if (isRecording.value) {
        // 如果仍在录音状态，重新开始识别（用于连续录音）
        recognition.value.start()
      } else {
        console.log('语音识别结束')
      }
    }

    // 处理错误
    recognition.value.onerror = (event) => {
      console.error('语音识别错误:', event.error)
      isRecording.value = false
    }
  }
})

// 组件卸载时停止识别
onUnmounted(() => {
  if (recognition.value && isRecording.value) {
    recognition.value.stop()
  }
})

const startRecording = () => {
  if (!isSpeechSupported.value) {
    alert('您的浏览器不支持语音识别功能，请使用 Chrome 浏览器')
    return
  }

  isRecording.value = true
  interimTranscript.value = ''

  try {
    recognition.value.start()
    console.log('开始语音识别...')
  } catch (error) {
    console.error('启动语音识别失败:', error)
    isRecording.value = false
  }
}

const stopRecording = () => {
  if (isRecording.value && recognition.value) {
    isRecording.value = false
    try {
      recognition.value.stop()
      console.log('停止语音识别')
    } catch (error) {
      console.error('停止语音识别失败:', error)
    }
  }
}
</script>

<style scoped>
.agent {
  margin: 20px 0;
  display: flex;
  flex-direction: column;
  height: calc(100% - 40px);
  background: linear-gradient(145deg, #f0f0f0, #ffffff);
  border-radius: 20px;
  border: #e1e1e1 2px solid;
  overflow: hidden;
}

/* 聊天头部样式 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  background: linear-gradient(145deg, #ffffff, #f0f0f0);
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bot-avatar {
  width: 48px;
  height: 48px;
  background: linear-gradient(145deg, #3f51b5, #66BB6A);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    3px 3px 6px #d9d9d9,
    -3px -3px 6px #ffffff;
}

.bot-icon {
  color: white;
  position: relative;
  top: -2px;
}

.header-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bot-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #4CAF50;
  animation: pulse 2s infinite;
}

.status-dot.offline {
  background: #f44336;
}

.status-text {
  font-size: 12px;
  color: #666;
  position: relative;
  top: -1px;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 重新连接按钮样式 */
.reconnect-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(145deg, #ff9800, #ffb74d);
  border: none;
  border-radius: 20px;
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 
    3px 3px 6px #d9d9d9,
    -3px -3px 6px #ffffff;
  margin-right: 10px;
}

.reconnect-btn:hover:not(:disabled) {
  background: linear-gradient(145deg, #f57c00, #ff9800);
  transform: translateY(-1px);
}

.reconnect-btn:active:not(:disabled) {
  box-shadow: 
    inset 2px 2px 4px #d9d9d9,
    inset -2px -2px 4px #ffffff;
  transform: translateY(0);
}

.reconnect-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.reconnect-icon {
  color: white;
  transition: color 0.2s ease;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(145deg, #e6e6e6, #ffffff);
  border: none;
  border-radius: 20px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow:
    3px 3px 6px #d9d9d9,
    -3px -3px 6px #ffffff;
}

.new-chat-btn:hover {
  color: #4CAF50;
  transform: translateY(-1px);
}

.new-chat-btn:active {
  box-shadow:
    inset 2px 2px 4px #d9d9d9,
    inset -2px -2px 4px #ffffff;
}

.new-chat-icon {
  color: currentColor;
}

/* 聊天内容区域样式 */
.chat-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
  scroll-behavior: smooth;
}

.welcome-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  height: 100%;
  gap: 20px;
}

.welcome-icon {
  min-width: 80px;
  min-height: 80px;
  max-width: 80px;
  max-height: 80px;
  background: linear-gradient(145deg, #3f51b5, #66BB6A);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    5px 5px 10px #d9d9d9,
    -5px -5px 10px #ffffff;
}

.welcome-icon svg {
  color: white;
  position: relative;
  top: -4px;
}

.welcome-text h3 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.welcome-text p {
  font-size: 16px;
  color: #666;
  line-height: 1.5;
}

/* 消息容器 */
.messages-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
  animation: fadeInUp 0.3s ease;
}

.user-message {
  align-self: flex-end;
  background: linear-gradient(145deg, #4CAF50, #66BB6A);
  color: white;
  border-bottom-right-radius: 4px;
}

.bot-message {
  align-self: flex-start;
  background: linear-gradient(145deg, #e6e6e6, #ffffff);
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow:
    3px 3px 6px #d9d9d9,
    -3px -3px 6px #ffffff;
}

.message-content {
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 4px;
}

.message-time {
  font-size: 11px;
  opacity: 0.7;
  text-align: right;
}

.bot-message .message-time {
  text-align: left;
}

/* 输入区域样式 */
.bottom {
  margin: 20px;
  height: 45px;
  border-radius: 50px;
  background: #f0f0f0;
  width: calc(100% - 40px);
  box-shadow:
    8px 8px 16px #d9d9d9,
    -8px -8px 16px #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.text-input {
  height: 45px;
  display: flex;
  align-items: center;
}

.voice {
  height: 35px;
  width: 35px;
  background: linear-gradient(145deg, #e6e6e6, #ffffff);
  border-radius: 50%;
  position: relative;
  left: 10px;
  box-shadow:
    3px 3px 6px #d9d9d9,
    -3px -3px 6px #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice:active {
  box-shadow:
    inset 2px 2px 4px #d9d9d9,
    inset -2px -2px 4px #ffffff;
}

.voice.recording {
  background: linear-gradient(145deg, #4CAF50, #66BB6A);
}

.voice.recording .mic-icon {
  color: white;
}

.mic-icon {
  color: #666;
  transition: color 0.2s ease;
}

.recording {
  color: #38c300;
}

.text-input-box {
  flex: 1;
  margin: 0 10px;
}

.input-field {
  margin-left: 10px;
  width: calc(100% - 50px);
  height: 35px;
  border: none;
  outline: none;
  background: linear-gradient(145deg, #e8e8e8, #ffffff);
  border-radius: 20px;
  padding: 0 15px;
  font-size: 14px;
  color: #333;
  box-shadow:
    inset 3px 3px 6px #d9d9d9,
    inset -3px -3px 6px #ffffff;
  transition: all 0.2s ease;
}

.input-field:focus {
  box-shadow:
    inset 2px 2px 4px #d9d9d9,
    inset -2px -2px 4px #ffffff;
}

.input-field::placeholder {
  color: #999;
}

.send-button {
  height: 35px;
  width: 35px;
  background: linear-gradient(145deg, #e6e6e6, #ffffff);
  border-radius: 50%;
  position: relative;
  right: 10px;
  box-shadow:
    3px 3px 6px #d9d9d9,
    -3px -3px 6px #ffffff;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-button:hover .send-icon {
  color: #38c30e;
}

.send-button:active {
  box-shadow:
    inset 2px 2px 4px #d9d9d9,
    inset -2px -2px 4px #ffffff;
}

.send-icon {
  color: #666;
  transition: color 0.2s ease;
}

/* 语音识别状态指示器 */
.recording-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  margin: 10px 20px;
  padding: 15px;
  background: linear-gradient(145deg, #f0f0f0, #ffffff);
  border-radius: 15px;
  box-shadow:
    5px 5px 10px #d9d9d9,
    -5px -5px 10px #ffffff;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

.pulse-animation {
  width: 20px;
  height: 20px;
  background: #ff4444;
  border-radius: 50%;
  margin-bottom: 10px;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 0.7;
  }
  50% {
    transform: scale(1.2);
    opacity: 1;
  }
  100% {
    transform: scale(0.8);
    opacity: 0.7;
  }
}

.recording-text {
  color: #666;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 5px;
}

.interim-text {
  color: #4CAF50;
  font-size: 16px;
  font-weight: 500;
  text-align: center;
  max-width: 300px;
  word-break: break-word;
  animation: pulse 1.5s infinite;
}

/* 思考框样式 */
.thinking-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  padding: 6px 0;
}

.thinking-status-container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thinking-status {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.thinking-dots {
  display: flex;
  gap: 2px;
}

.thinking-dots span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #999;
  animation: thinkingBounce 1.4s ease-in-out infinite both;
}

.thinking-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.thinking-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes thinkingBounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.toggle-steps-btn {
  background: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 12px;
  padding: 2px 8px;
  font-size: 10px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toggle-steps-btn:hover {
  background: #e0e0e0;
  border-color: #ccc;
}

.thinking-steps {
  margin: 8px 0;
  padding: 0;
}

.thinking-step {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 11px;
  color: #888;
}

.thinking-step:last-child {
  margin-bottom: 0;
}

.step-icon {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #4CAF50;
  color: white;
  font-size: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-text {
  line-height: 1.3;
}

.final-response {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #f0f0f0;
}

/* Markdown 内容样式 */
.final-response :deep() p {
  margin: 0.5em 0;
  line-height: 1.6;
}

.final-response :deep() strong {
  font-weight: 600;
  color: #333;
}

.final-response :deep() em {
  font-style: italic;
  color: #555;
}

.final-response :deep() code {
  background: #f5f5f5;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 0.9em;
  color: #d63384;
}

.final-response :deep() pre {
  background: #f8f9fa;
  padding: 0.8em;
  border-radius: 6px;
  overflow-x: auto;
  margin: 0.8em 0;
  border-left: 4px solid #007bff;
}

.final-response :deep() pre code {
  background: none;
  padding: 0;
  color: #333;
}

.final-response :deep() ul, .final-response :deep() ol {
  margin: 0.5em 0;
  padding-left: 1.5em;
}

.final-response :deep() li {
  margin: 0.3em 0;
  line-height: 1.5;
}

.final-response :deep() blockquote {
  border-left: 4px solid #ddd;
  margin: 0.8em 0;
  padding-left: 1em;
  color: #666;
  font-style: italic;
}

.final-response :deep() a {
  color: #007bff;
  text-decoration: none;
}

.final-response :deep() a:hover {
  text-decoration: underline;
}

.final-response :deep() table {
  border-collapse: collapse;
  width: 100%;
  margin: 0.8em 0;
}

.final-response :deep() th, .final-response :deep() td {
  border: 1px solid #ddd;
  padding: 0.5em;
  text-align: left;
}

.final-response :deep() th {
  background: #f8f9fa;
  font-weight: 600;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>