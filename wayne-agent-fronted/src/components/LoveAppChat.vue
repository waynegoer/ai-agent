<template>
  <div class="chat-container">
    <div class="chat-header">
      <button class="back-button" @click="goHome">
        ← ホームに戻る
      </button>
      <h1 class="chat-title">💕 AI 恋愛マスター</h1>
      <button class="clear-button" @click="clearChat">
        チャットをクリア
      </button>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div
          v-for="(message, index) in chatStore.loveAppMessages"
          :key="index"
          :class="['message', message.type]"
      >
        <div class="message-content">
          <div class="message-avatar">
            <span v-if="message.type === 'user'">👤</span>
            <span v-else>💕</span>
          </div>
          <div class="message-text">
            <div class="message-bubble">
              {{ message.content }}
            </div>
            <div class="message-time">
              {{ formatTime(message.timestamp) }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="isLoading" class="message ai">
        <div class="message-content">
          <div class="message-avatar">💕</div>
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
            placeholder="恋愛に関する質問や悩みを入力してください..."
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
</template>

<script>
import {ref, onMounted, nextTick, onUnmounted} from 'vue'
import {useRouter} from 'vue-router'
import {useChatStore} from '../stores/chatStore'
import {generateChatId, connectToLoveAppSSE, closeSSEConnection} from '../api/chatService'

export default {
  name: 'LoveAppChat',
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
      if (!chatStore.loveAppChatId) {
        const chatId = generateChatId()
        chatStore.initLoveAppChat(chatId)
      }
    }

    // 发送消息
    const sendMessage = async () => {
      if (!inputMessage.value.trim() || isLoading.value) return

      const message = inputMessage.value.trim()
      inputMessage.value = ''
      hasReceivedData = false

      // 添加用户消息
      chatStore.addLoveAppMessage({
        type: 'user',
        content: message,
        timestamp: new Date()
      })

      // 添加AI消息占位符
      chatStore.addLoveAppMessage({
        type: 'ai',
        content: '',
        timestamp: new Date()
      })

      isLoading.value = true
      scrollToBottom()

      try {
        // 建立SSE连接
        currentSSEConnection = connectToLoveAppSSE(
            message,
            chatStore.loveAppChatId,
            (data) => {
              // 实时更新AI回复
              hasReceivedData = true
              chatStore.updateLoveAppLastAIMessage(prev => prev + data)
              scrollToBottom()
            },
            (error) => {
              console.error('SSE Error:', error)
              // 只有在真正出错时才显示错误信息
              if (error.message && !error.message.includes('completed normally')) {
                chatStore.updateLoveAppLastAIMessage('申し訳ございません。エラーが発生しました。')
              }
              isLoading.value = false
            },
            () => {
              // 连接完成
              isLoading.value = false
              // 如果没有收到任何数据，显示提示信息
              if (!hasReceivedData) {
                chatStore.updateLoveAppLastAIMessage('申し訳ございません。サーバーから応答がありませんでした。')
              }
              scrollToBottom()
            }
        )
      } catch (error) {
        console.error('Connection Error:', error)
        chatStore.updateLoveAppLastAIMessage('申し訳ございません。接続エラーが発生しました。')
        isLoading.value = false
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
      chatStore.clearLoveAppChat()
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
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.chat-header {
  background: linear-gradient(135deg, #ff6b9d, #c44569);
  color: white;
  padding: 1rem 2rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.back-button, .clear-button {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.3s ease;
}

.back-button:hover, .clear-button:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
}

.chat-title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
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
  background: linear-gradient(135deg, #ff6b9d, #c44569);
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
  padding: 1rem 2rem;
  border-top: 1px solid #e0e0e0;
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
  border-color: #ff6b9d;
}

.chat-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.send-button {
  background: linear-gradient(135deg, #ff6b9d, #c44569);
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
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

.send-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

@media (max-width: 768px) {
  .chat-header {
    padding: 1rem;
  }

  .chat-title {
    font-size: 1.2rem;
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
</style>