import {defineStore} from 'pinia'

export const useChatStore = defineStore('chat', {
    state: () => ({
        loveAppMessages: [],
        loveAppChatId: null,
        manusAppMessages: []
    }),

    actions: {
        initLoveAppChat(chatId) {
            this.loveAppChatId = chatId
            this.loveAppMessages = []
        },

        addLoveAppMessage(message) {
            this.loveAppMessages.push(message)
        },

        updateLoveAppLastAIMessage(content) {
            const lastMessage = this.loveAppMessages[this.loveAppMessages.length - 1]
            if (lastMessage && lastMessage.type === 'ai') {
                if (typeof content === 'function') {
                    lastMessage.content = content(lastMessage.content || '')
                } else {
                    lastMessage.content = content
                }
            }
        },

        initManusAppChat() {
            this.manusAppMessages = []
        },

        addManusAppMessage(message) {
            this.manusAppMessages.push(message)
        },

        updateManusAppLastAIMessage(content) {
            const lastMessage = this.manusAppMessages[this.manusAppMessages.length - 1]
            if (lastMessage && lastMessage.type === 'ai') {
                if (typeof content === 'function') {
                    lastMessage.content = content(lastMessage.content || '')
                } else {
                    lastMessage.content = content
                }
            }
        },

        clearLoveAppChat() {
            this.loveAppMessages = []
            this.loveAppChatId = null
        },

        clearManusAppChat() {
            this.manusAppMessages = []
        }
    }
})
