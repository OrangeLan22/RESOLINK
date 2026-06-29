import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAIAgentStore = defineStore('aiAgent', () => {
  // 状态
  const messages = ref([])
  const sessionId = ref('')
  const isConnected = ref(false)
  const isConnecting = ref(false)
  const ws = ref(null)
  const reconnectAttempts = ref(0)
  const maxReconnectAttempts = 5

  // 从环境变量获取 WebSocket 地址
  const agentBaseUrl = import.meta.env.VITE_AGENT_BASE_URL || 'ws://localhost:2970'

  // 获取 token 并构建 WebSocket URL
  const getWebSocketUrl = () => {
    // 从 localStorage 获取 token
    let token = localStorage.getItem('token') || ''
    
    // 清理 token（去除引号和前后空格）
    token = token.replace(/^['"]|['"]$/g, '').trim()
    
    if (!token) {
      console.warn('未找到 token，将使用匿名连接')
      return `${agentBaseUrl}/ws/chat`
    }
    
    // 构建带 token 的 WebSocket URL
    return `${agentBaseUrl}/ws/chat?token=${encodeURIComponent(token)}`
  }

  // WebSocket 连接管理
  const connectWebSocket = () => {
    if (isConnecting.value || isConnected.value) return
    
    isConnecting.value = true
    console.log('正在连接 WebSocket...')
    
    try {
      const wsUrlWithToken = getWebSocketUrl()
      console.log('WebSocket URL:', wsUrlWithToken)
      ws.value = new WebSocket(wsUrlWithToken)
      
      ws.value.onopen = () => {
        console.log('WebSocket 连接成功')
        isConnected.value = true
        isConnecting.value = false
        reconnectAttempts.value = 0
      }
      
      ws.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('收到消息:', data)
          
          // 处理连接成功消息
          if (data.type === 'CONNECT') {
            console.log('连接成功')
            isConnected.value = true // 确保连接状态正确更新
            // 不要从CONNECT消息中获取session_id，只使用MODEL_STATUS消息中的session_id
          } else if (data.type === 'MODEL_STATUS') {
            // 处理工具调用类消息
            console.log('收到模型状态消息:', data)
            
            // 从模型返回的消息中提取session_id
            const modelSessionId = data.content?.session_id
            
            // 检查session_id是否发生变化（新的对话）
            if (modelSessionId && sessionId.value && sessionId.value !== modelSessionId) {
              console.log('检测到新的session_id，开始新对话')
              
              // 保存用户当前正在输入的消息（如果有）
              const pendingUserMessage = messages.value.find(msg => msg.type === 'user' && !msg.processed)
              
              // 清空消息历史，但保留用户当前输入的消息
              messages.value = []
              
              // 如果有用户正在输入的消息，重新添加
              if (pendingUserMessage) {
                messages.value.push({
                  ...pendingUserMessage,
                  processed: true // 标记为已处理
                })
              }
              
              // 添加系统提示消息
              addBotMessage('检测到新的对话会话，已为您开启新的对话。')
            }
            
            // 更新session_id（如果模型返回了新的session_id）
            if (modelSessionId) {
              sessionId.value = modelSessionId
            }
            
            // 查找最后一个机器人消息（思考中的消息）
            const lastBotMessageIndex = messages.value.findLastIndex(msg => msg.type === 'bot')
            
            // 检查最后一个机器人消息是否已经完成（有最终回复内容）
            const lastBotMessage = lastBotMessageIndex !== -1 ? messages.value[lastBotMessageIndex] : null
            const shouldCreateNewMessage = lastBotMessage && lastBotMessage.content
            
            if (lastBotMessageIndex === -1 || shouldCreateNewMessage) {
              // 如果没有机器人消息，或者最后一个机器人消息已经完成，创建新的思考消息
              addBotMessage('', true, [], false) // isThinking为true，空思考步骤，默认收起
            } else {
              // 更新现有的机器人消息
              // 确保有thinkingSteps数组
              if (!lastBotMessage.thinkingSteps) {
                lastBotMessage.thinkingSteps = []
              }
              
              // 当收到新的思考步骤时，将之前的步骤标记为已完成
              lastBotMessage.thinkingSteps.forEach(step => {
                if (step.status !== 'completed') {
                  step.status = 'completed'
                }
              })
              
              // 添加或更新思考步骤
              const stepIndex = lastBotMessage.thinkingSteps.findIndex(step => 
                step.description === data.content.description
              )
              
              if (stepIndex === -1) {
                lastBotMessage.thinkingSteps.push({
                  description: data.content.description,
                  timestamp: data.timestamp,
                  status: 'active' // 新步骤标记为进行中
                })
              } else {
                lastBotMessage.thinkingSteps[stepIndex].timestamp = data.timestamp
                lastBotMessage.thinkingSteps[stepIndex].status = 'active' // 更新为进行中
              }
              
              // MODEL_STATUS消息只添加思考步骤，不改变思考状态（保持思考中）
              lastBotMessage.isThinking = true // 保持思考中状态
              lastBotMessage.showThinkingSteps = true // 思考中时自动显示时间线
            }
            
          } else if (data.type === 'FULL_MODEL' || data.type === 'full_model') {
            // 处理最终输出消息（兼容两种大小写）
            console.log('收到最终输出消息:', data)
            
            // 查找最后一个机器人消息
            const lastBotMessageIndex = messages.value.findLastIndex(msg => msg.type === 'bot')
            
            // 检查最后一个机器人消息是否已经完成（有最终回复内容）
            const lastBotMessage = lastBotMessageIndex !== -1 ? messages.value[lastBotMessageIndex] : null
            const shouldCreateNewMessage = lastBotMessage && lastBotMessage.content
            
            if (lastBotMessageIndex === -1 || shouldCreateNewMessage) {
              // 如果没有机器人消息，或者最后一个机器人消息已经完成，创建新的消息
              let finalContent = data.content
              if (typeof finalContent === 'object' && finalContent.content) {
                finalContent = finalContent.content
              }
              addBotMessage(finalContent, false, [], false)
            } else {
              // 更新现有的机器人消息，设置最终回复内容
              lastBotMessage.isThinking = false // 思考完成
              
              // 将所有思考步骤标记为已完成
              if (lastBotMessage.thinkingSteps) {
                lastBotMessage.thinkingSteps.forEach(step => {
                  step.status = 'completed'
                })
              }
              
              // 提取最终回复内容
              let finalContent = data.content
              if (typeof finalContent === 'object' && finalContent.content) {
                finalContent = finalContent.content
              }
              lastBotMessage.content = finalContent
              
              lastBotMessage.showThinkingSteps = false // 默认收起思考步骤
            }
            
            // 将最后一个用户消息标记为已处理
            const lastUserMessageIndex = messages.value.findLastIndex(msg => msg.type === 'user')
            if (lastUserMessageIndex !== -1) {
              messages.value[lastUserMessageIndex].processed = true
            }
            
          } else if (data.type === 'message' && data.content) {
            // 处理普通消息（兼容旧格式）
            addBotMessage(data.content)
            
            // 将最后一个用户消息标记为已处理
            const lastUserMessageIndex = messages.value.findLastIndex(msg => msg.type === 'user')
            if (lastUserMessageIndex !== -1) {
              messages.value[lastUserMessageIndex].processed = true
            }
          } else if (data.type === 'error') {
            addBotMessage(`错误: ${data.message}`)
            
            // 将最后一个用户消息标记为已处理（即使出错）
            const lastUserMessageIndex = messages.value.findLastIndex(msg => msg.type === 'user')
            if (lastUserMessageIndex !== -1) {
              messages.value[lastUserMessageIndex].processed = true
            }
          }
        } catch (error) {
          console.error('解析消息失败:', error)
        }
      }
      
      ws.value.onclose = (event) => {
        console.log('WebSocket 连接关闭:', event.code, event.reason)
        isConnected.value = false
        isConnecting.value = false
        
        if (event.code !== 1000) { // 非正常关闭
          reconnectAttempts.value++
          if (reconnectAttempts.value <= maxReconnectAttempts) {
            console.log(`将在 3 秒后尝试重新连接 (${reconnectAttempts.value}/${maxReconnectAttempts})`)
            setTimeout(() => {
              if (!isConnected.value) {
                connectWebSocket()
              }
            }, 3000)
          } else {
            console.log('已达到最大重连次数，请手动重连')
            addBotMessage('连接已断开，请点击重新连接按钮尝试重新连接。')
          }
        }
      }
      
      ws.value.onerror = (error) => {
        console.error('WebSocket 连接错误:', error)
        isConnecting.value = false
        isConnected.value = false
      }
      
    } catch (error) {
      console.error('创建 WebSocket 连接失败:', error)
      isConnecting.value = false
      addBotMessage('连接失败，请检查服务器是否正常运行。')
    }
  }

  // 发送消息到 WebSocket
  const sendMessageToWebSocket = (message) => {
    if (!ws.value || ws.value.readyState !== WebSocket.OPEN) {
      addBotMessage('连接未就绪，请稍后重试。')
      return false
    }
    
    try {
      // 获取 token（从 localStorage 或 cookie）
      let token = localStorage.getItem('token') || ''
      token = token.replace(/^['"]|['"]$/g, '').trim()
      
      const messageData = {
        type: 'CHAT',
        content: {
          message: message,
          session_id: sessionId.value,
          token: token
        },
        timestamp: Date.now()
      }
      console.log('发送消息:', messageData)
      ws.value.send(JSON.stringify(messageData))
      return true
    } catch (error) {
      console.error('发送消息失败:', error)
      addBotMessage('发送消息失败，请检查连接状态。')
      return false
    }
  }

  // 关闭 WebSocket 连接
  const closeWebSocket = () => {
    if (ws.value) {
      ws.value.close(1000, '正常关闭')
      ws.value = null
    }
    isConnected.value = false
    isConnecting.value = false
  }

  // 格式化时间
  const formatTime = (date = new Date()) => {
    return date.toLocaleTimeString('zh-CN', {
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  // 添加用户消息
  const addUserMessage = (content, processed = false) => {
    messages.value.push({
      type: 'user',
      content: content,
      time: formatTime(),
      processed: processed // 标记消息是否已处理（服务器已响应）
    })
  }

  // 添加机器人消息
  const addBotMessage = (content, isThinking = false, thinkingSteps = [], showThinkingSteps = false) => {
    messages.value.push({
      type: 'bot',
      content: content,
      time: formatTime(),
      isThinking: isThinking, // 标记是否为思考状态
      thinkingSteps: thinkingSteps, // 思考步骤数组
      showThinkingSteps: showThinkingSteps // 是否显示思考步骤
    })
  }

  // 切换思考步骤显示状态
  const toggleThinkingSteps = (index) => {
    if (messages.value[index] && messages.value[index].type === 'bot') {
      messages.value[index].showThinkingSteps = !messages.value[index].showThinkingSteps
    }
  }

  // 开始新对话
  const startNewChat = () => {
    messages.value = []
    console.log('开始新对话')
  }

  return {
    messages,
    sessionId,
    isConnected,
    isConnecting,
    connectWebSocket,
    sendMessageToWebSocket,
    closeWebSocket,
    addUserMessage,
    addBotMessage,
    toggleThinkingSteps,
    startNewChat
  }
})