<template>
  <div class="super-agent-container">
    <div class="header">
      <div class="back-button" @click="goHome">← ホームに戻る</div>
      <h1 class="title">AI スーパーエージェント</h1>
      <div class="placeholder"></div>
    </div>

    <div class="content-wrapper">
      <div class="chat-area">
        <div class="chat-messages" ref="messagesContainer">
          <div
              v-for="(message, index) in chatStore.manusAppMessages"
              :key="index"
              :class="['message', message.type]"
          >
            <div class="message-content">
              <div class="message-avatar">
                <span v-if="message.type === 'user'">👤</span>
                <span v-else>🤖</span>
              </div>
              <div class="message-text">
                <div class="message-bubble" :class="message.type">
                  <div v-if="message.type === 'ai' && message.steps" class="steps-container">
                    <div v-for="(step, stepIndex) in message.steps" :key="stepIndex" class="step-item">
                      <div class="step-header">
                        <span class="step-number">Step {{ stepIndex + 1 }}</span>
                        <span class="step-status" :class="step.status">{{ step.statusText }}</span>
                      </div>
                      <div class="step-content">{{ step.content }}</div>
                    </div>
                    <div v-if="message.finalResult" class="final-result">
                      <div class="result-header">🎯 最終結果</div>
                      <div class="result-content">{{ message.finalResult }}</div>
                    </div>
                  </div>
                  <div v-else>{{ message.content }}</div>
                </div>
                <div class="message-time">
                  {{ formatTime(message.timestamp) }}
                </div>
              </div>
            </div>
          </div>

          <div v-if="isLoading" class="message ai">
            <div class="message-content">
              <div class="message-avatar">🤖</div>
              <div class="message-text">
                <div class="message-bubble typing">
                  <div class="typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-container">
          <div class="chat-input-wrapper">
            <textarea
                v-model="inputMessage"
                @keydown.enter.prevent="sendMessage"
                :disabled="isLoading"
                placeholder="何でもお聞きください。タスクのサポートや質問にお答えします..."
                class="chat-input"
                rows="1"
                ref="inputRef"
            ></textarea>
            <button
                @click="sendMessage"
                :disabled="!inputMessage.trim() || isLoading"
                class="send-button"
            >
              送信
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import {ref, onMounted, nextTick, onUnmounted} from 'vue'
import {useRouter} from 'vue-router'
import {useChatStore} from '../stores/chatStore'
import {connectToManusSSE, closeSSEConnection} from '../api/chatService'

export default {
  name: 'ManusAppChat',
  setup() {
    const router = useRouter()
    const chatStore = useChatStore()
    const inputMessage = ref('')
    const isLoading = ref(false)
    const messagesContainer = ref(null)
    const inputRef = ref(null)
    let currentSSEConnection = null
    let hasReceivedData = false

    // 初始化聊天
    const initChat = () => {
      if (chatStore.manusAppMessages.length === 0) {
        chatStore.initManusAppChat()
      }
    }

    // 处理Manus的步骤信息
    const processManusStep = (data) => {
      console.log('Processing Manus step:', data)

      // 检查是否是步骤信息
      if (data.startsWith('Step ')) {
        // 提取步骤内容
        const stepMatch = data.match(/^Step \d+: (.+)$/)
        if (stepMatch) {
          const stepContent = stepMatch[1]

          // 处理 "completed - no act" 格式
          if (stepContent.includes('completed - no act')) {
            return {
              type: 'step',
              status: 'completed',
              statusText: '完了',
              content: 'ステップ完了（アクション不要）'
            }
          }

          // 处理 "completed" 格式
          if (stepContent.includes('completed')) {
            return {
              type: 'step',
              status: 'completed',
              statusText: '完了',
              content: 'ステップ完了'
            }
          }

          // 处理工具调用结果 - 匹配 "工具名: 结果" 格式
          const toolCallMatch = stepContent.match(/^(\w+):\s*"(.+)"$/)
          if (toolCallMatch) {
            const toolName = toolCallMatch[1]
            const result = toolCallMatch[2]

            // 特殊处理 doTerminate
            if (toolName === 'doTerminate') {
              return {
                type: 'step',
                status: 'terminated',
                statusText: '終了',
                content: 'タスクが終了しました'
              }
            }

            // 其他工具调用
            return {
              type: 'step',
              status: 'tool',
              statusText: 'ツール実行',
              content: `${toolName}: ${result}`
            }
          }

          // 如果是思考过程，简化显示
          if (stepContent.includes('的思考：')) {
            const thoughtMatch = stepContent.match(/(.+?)的思考：(.+)/)
            if (thoughtMatch) {
              return {
                type: 'step',
                status: 'thinking',
                statusText: '思考中',
                content: thoughtMatch[2]
              }
            }
          }

          // 处理其他常见的步骤状态
          if (stepContent.includes('thinking') || stepContent.includes('思考')) {
            return {
              type: 'step',
              status: 'thinking',
              statusText: '思考中',
              content: stepContent
            }
          }

          if (stepContent.includes('action') || stepContent.includes('行动')) {
            return {
              type: 'step',
              status: 'action',
              statusText: '実行中',
              content: stepContent
            }
          }

          if (stepContent.includes('observation') || stepContent.includes('观察')) {
            return {
              type: 'step',
              status: 'observation',
              statusText: '観察中',
              content: stepContent
            }
          }

          // 其他步骤信息
          return {
            type: 'step',
            status: 'processing',
            statusText: '処理中',
            content: stepContent
          }
        }
      }

      // 检查是否是执行结束信息
      if (data.includes('执行结束') || data.includes('Terminated')) {
        return {
          type: 'final',
          content: 'タスクが完了しました'
        }
      }

      // 检查是否是错误信息
      if (data.includes('执行错误') || data.includes('error')) {
        return {
          type: 'error',
          content: data
        }
      }

      // 检查是否是assistantMessage的内容（非Step格式的AI回复）
      if (!data.startsWith('Step ') && !data.includes('执行结束') && !data.includes('执行错误')) {
        // 这可能是assistantMessage.getText()的内容，直接返回
        return {
          type: 'response',
          content: data
        }
      }

      // 默认返回原数据
      return {
        type: 'unknown',
        content: data
      }
    }

    // 发送消息
    const sendMessage = async () => {
      if (!inputMessage.value.trim() || isLoading.value) return

      const message = inputMessage.value.trim()
      inputMessage.value = ''
      hasReceivedData = false

      // 添加用户消息
      chatStore.addManusAppMessage({
        type: 'user',
        content: message,
        timestamp: new Date()
      })

      // 添加AI消息占位符，包含步骤信息
      chatStore.addManusAppMessage({
        type: 'ai',
        content: '',
        steps: [],
        finalResult: '',
        timestamp: new Date()
      })

      isLoading.value = true
      scrollToBottom()

      try {
        // 建立SSE连接
        currentSSEConnection = connectToManusSSE(
            message,
            (data) => {
              hasReceivedData = true
              const processedData = processManusStep(data)

              if (processedData) {
                const lastMessage = chatStore.manusAppMessages[chatStore.manusAppMessages.length - 1]

                if (processedData.type === 'step') {
                  // 添加步骤信息
                  if (!lastMessage.steps) {
                    lastMessage.steps = []
                  }
                  lastMessage.steps.push(processedData)
                } else if (processedData.type === 'final') {
                  // 设置最终结果
                  lastMessage.finalResult = processedData.content
                } else if (processedData.type === 'response') {
                  // 添加AI回复内容
                  lastMessage.content += processedData.content
                } else if (processedData.type === 'error') {
                  // 添加错误信息
                  lastMessage.content += `❌ ${processedData.content}`
                }

                scrollToBottom()
              }
            },
            (error) => {
              console.error('SSE Error:', error)
              isLoading.value = false

              // 只有在真正出错时才显示错误信息
              if (error.message && !error.message.includes('completed normally')) {
                const lastMessage = chatStore.manusAppMessages[chatStore.manusAppMessages.length - 1]
                lastMessage.content = '申し訳ございません。エラーが発生しました。'
              }
            },
            () => {
              // 连接完成
              isLoading.value = false

              // 如果没有收到任何数据，显示提示信息
              if (!hasReceivedData) {
                const lastMessage = chatStore.manusAppMessages[chatStore.manusAppMessages.length - 1]
                lastMessage.content = '申し訳ございません。サーバーから応答がありませんでした。'
              }

              scrollToBottom()
            }
        )
      } catch (error) {
        console.error('Connection Error:', error)
        isLoading.value = false
        const lastMessage = chatStore.manusAppMessages[chatStore.manusAppMessages.length - 1]
        lastMessage.content = '申し訳ございません。接続エラーが発生しました。'
      }
    }

    // 滚动到底部
    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
        }
      })
    }

    // 格式化时间
    const formatTime = (timestamp) => {
      return timestamp.toLocaleTimeString('ja-JP', {
        hour: '2-digit',
        minute: '2-digit'
      })
    }

    // 返回主页
    const goHome = () => {
      if (currentSSEConnection) {
        closeSSEConnection(currentSSEConnection)
      }
      router.push('/')
    }

    // 清空聊天
    const clearChat = () => {
      if (currentSSEConnection) {
        closeSSEConnection(currentSSEConnection)
      }
      chatStore.clearManusAppChat()
      initChat()
    }

    // 自动调整输入框高度
    const adjustInputHeight = () => {
      if (inputRef.value) {
        inputRef.value.style.height = 'auto'
        inputRef.value.style.height = inputRef.value.scrollHeight + 'px'
      }
    }

    onMounted(() => {
      initChat()
      scrollToBottom()

      // 监听输入框变化
      if (inputRef.value) {
        inputRef.value.addEventListener('input', adjustInputHeight)
      }
    })

    onUnmounted(() => {
      if (currentSSEConnection) {
        closeSSEConnection(currentSSEConnection)
      }
    })

    return {
      chatStore,
      inputMessage,
      isLoading,
      messagesContainer,
      inputRef,
      sendMessage,
      formatTime,
      goHome,
      clearChat
    }
  }
}
</script>

<style scoped>
.super-agent-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f9fbff;
}

.header {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 16px 24px;
  background-color: #3f51b5;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}

.back-button {
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  transition: opacity 0.2s;
  justify-self: start;
}

.back-button:hover {
  opacity: 0.8;
}

.title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
  text-align: center;
  justify-self: center;
}

.placeholder {
  width: 1px;
  justify-self: end;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.chat-area {
  flex: 1;
  padding: 16px;
  overflow: hidden;
  position: relative;
  min-height: calc(100vh - 56px - 180px);
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.message {
  display: flex;
  margin-bottom: 1rem;
}

.message.user {
  justify-content: flex-end;
}

.message.ai {
  justify-content: flex-start;
}

.message-content {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  max-width: 70%;
}

.message.user .message-content {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  flex-shrink: 0;
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.message.ai .message-avatar {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.message-text {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.message-bubble {
  background: white;
  padding: 1rem;
  border-radius: 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  word-wrap: break-word;
  white-space: pre-wrap;
  line-height: 1.5;
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
}

.message-time {
  font-size: 0.75rem;
  color: #999;
  padding: 0 0.5rem;
}

.message.user .message-time {
  text-align: right;
}

/* 步骤容器样式 */
.steps-container {
  width: 100%;
}

.step-item {
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #f8f9fa;
}

.step-item:last-child {
  margin-bottom: 0;
}

.step-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.step-number {
  font-weight: bold;
  color: #3f51b5;
  font-size: 0.9rem;
}

.step-status {
  padding: 0.25rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: bold;
}

.step-status.thinking {
  background: #e3f2fd;
  color: #1976d2;
}

.step-status.action {
  background: #fff3e0;
  color: #f57c00;
}

.step-status.observation {
  background: #f3e5f5;
  color: #7b1fa2;
}

.step-status.completed {
  background: #e8f5e8;
  color: #2e7d32;
}

.step-status.terminated {
  background: #ffebee;
  color: #c62828;
}

.step-status.tool {
  background: #e0f2f1;
  color: #00695c;
}

.step-status.processing {
  background: #f1f8e9;
  color: #558b2f;
}

.step-content {
  color: #333;
  line-height: 1.4;
}

.final-result {
  margin-top: 1rem;
  padding: 1rem;
  border: 2px solid #4caf50;
  border-radius: 8px;
  background: linear-gradient(135deg, #e8f5e8, #f1f8e9);
}

.result-header {
  font-weight: bold;
  color: #2e7d32;
  margin-bottom: 0.5rem;
  font-size: 1.1rem;
}

.result-content {
  color: #333;
  line-height: 1.5;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ccc;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.chat-input-container {
  background: white;
  padding: 1rem;
  border-top: 1px solid #e0e0e0;
  margin-top: auto;
}

.chat-input-wrapper {
  display: flex;
  gap: 1rem;
  align-items: flex-end;
  max-width: 800px;
  margin: 0 auto;
}

.chat-input {
  flex: 1;
  border: 2px solid #e0e0e0;
  border-radius: 20px;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  resize: none;
  outline: none;
  transition: border-color 0.3s ease;
  min-height: 20px;
  max-height: 120px;
}

.chat-input:focus {
  border-color: #3f51b5;
}

.chat-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.send-button {
  background: linear-gradient(135deg, #3f51b5, #5c6bc0);
  color: white;
  border: none;
  border-radius: 20px;
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  cursor: pointer;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(63, 81, 181, 0.3);
}

.send-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 响应式样式 */
@media (max-width: 768px) {
  .header {
    padding: 12px 16px;
  }

  .title {
    font-size: 18px;
  }

  .chat-area {
    padding: 12px;
    min-height: calc(100vh - 48px - 160px);
    margin-bottom: 12px;
  }

  .message-content {
    max-width: 85%;
  }

  .chat-input-container {
    padding: 1rem;
  }

  .chat-input-wrapper {
    flex-direction: column;
    gap: 0.5rem;
  }

  .send-button {
    align-self: flex-end;
  }
}

@media (max-width: 480px) {
  .header {
    padding: 10px 12px;
  }

  .back-button {
    font-size: 14px;
  }

  .title {
    font-size: 16px;
  }

  .chat-area {
    padding: 8px;
    min-height: calc(100vh - 42px - 150px);
    margin-bottom: 8px;
  }
}
</style>