import axios from 'axios'
import {API_BASE_URL} from '../config/api.js'

// 生成随机聊天室ID
export function generateChatId() {
    return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

// AI恋爱大师SSE连接
export function connectToLoveAppSSE(message, chatId, onMessage, onError, onComplete) {
    const url = `${API_BASE_URL}/ai/love_app/chat/sse_emitter?message=${encodeURIComponent(message)}&chatId=${encodeURIComponent(chatId)}`

    console.log('Connecting to LoveApp SSE:', url)

    const eventSource = new EventSource(url)
    let isCompleted = false
    let hasReceivedData = false
    let connectionStartTime = Date.now()

    eventSource.onopen = (event) => {
        console.log('LoveApp SSE connection opened successfully')
        connectionStartTime = Date.now()
    }

    eventSource.onmessage = (event) => {
        try {
            const data = event.data
            console.log('LoveApp SSE message received:', data)
            hasReceivedData = true

            // 直接处理所有数据，后端不发送特殊结束信号
            if (data && data.trim() !== '') {
                onMessage(data)
            }
        } catch (error) {
            console.error('LoveApp SSE message parsing error:', error)
            onError(error)
        }
    }

    eventSource.onerror = (error) => {
        const connectionDuration = Date.now() - connectionStartTime
        console.log('LoveApp SSE error event triggered:', {
            readyState: eventSource.readyState,
            hasReceivedData,
            isCompleted,
            connectionDuration
        })

        // 根据后端代码分析，正常完成时readyState会是CLOSED
        // 如果连接持续时间超过1秒且收到过数据，认为是正常完成
        if (eventSource.readyState === EventSource.CLOSED &&
            connectionDuration > 1000 &&
            hasReceivedData &&
            !isCompleted) {
            console.log('LoveApp SSE connection completed normally (backend called complete())')
            isCompleted = true
            onComplete() // 调用完成回调，不调用错误回调
            return
        }

        // 如果连接时间很短且没有收到数据，可能是连接失败
        if (connectionDuration < 1000 && !hasReceivedData) {
            console.error('LoveApp SSE connection failed quickly')
            onError(new Error('Failed to connect to server'))
            return
        }

        // 其他情况，等待一下再判断
        setTimeout(() => {
            if (eventSource.readyState === EventSource.CLOSED && !isCompleted) {
                console.log('LoveApp SSE connection closed after delay')
                isCompleted = true
                onComplete()
            } else if (!isCompleted) {
                console.error('LoveApp SSE connection error')
                onError(error)
            }
        }, 500)

        eventSource.close()
    }

    // 设置超时处理
    const timeout = setTimeout(() => {
        if (!isCompleted) {
            console.log('LoveApp SSE connection timeout')
            eventSource.close()
            onError(new Error('Connection timeout'))
        }
    }, 200000) // 3分20秒超时

    // 清理超时
    const originalClose = eventSource.close
    eventSource.close = function () {
        clearTimeout(timeout)
        originalClose.call(this)
    }

    return eventSource
}

// AI超级智能体SSE连接
export function connectToManusSSE(message, onMessage, onError, onComplete) {
    const url = `${API_BASE_URL}/ai/manus/chat?message=${encodeURIComponent(message)}`

    console.log('Connecting to Manus SSE:', url)

    const eventSource = new EventSource(url)
    let isCompleted = false
    let hasReceivedData = false
    let connectionStartTime = Date.now()

    eventSource.onopen = (event) => {
        console.log('Manus SSE connection opened successfully')
        connectionStartTime = Date.now()
    }

    eventSource.onmessage = (event) => {
        try {
            const data = event.data
            console.log('Manus SSE message received:', data)
            hasReceivedData = true

            // 直接处理所有数据，后端不发送特殊结束信号
            if (data && data.trim() !== '') {
                onMessage(data)
            }
        } catch (error) {
            console.error('Manus SSE message parsing error:', error)
            onError(error)
        }
    }

    eventSource.onerror = (error) => {
        const connectionDuration = Date.now() - connectionStartTime
        console.log('Manus SSE error event triggered:', {
            readyState: eventSource.readyState,
            hasReceivedData,
            isCompleted,
            connectionDuration
        })

        // 根据后端代码分析，正常完成时readyState会是CLOSED
        // 如果连接持续时间超过1秒且收到过数据，认为是正常完成
        if (eventSource.readyState === EventSource.CLOSED &&
            connectionDuration > 1000 &&
            hasReceivedData &&
            !isCompleted) {
            console.log('Manus SSE connection completed normally (backend called complete())')
            isCompleted = true
            onComplete() // 调用完成回调，不调用错误回调
            return
        }

        // 如果连接时间很短且没有收到数据，可能是连接失败
        if (connectionDuration < 1000 && !hasReceivedData) {
            console.error('Manus SSE connection failed quickly')
            onError(new Error('Failed to connect to server'))
            return
        }

        // 其他情况，等待一下再判断
        setTimeout(() => {
            if (eventSource.readyState === EventSource.CLOSED && !isCompleted) {
                console.log('Manus SSE connection closed after delay')
                isCompleted = true
                onComplete()
            } else if (!isCompleted) {
                console.error('Manus SSE connection error')
                onError(error)
            }
        }, 500)

        eventSource.close()
    }

    // 设置超时处理
    const timeout = setTimeout(() => {
        if (!isCompleted) {
            console.log('Manus SSE connection timeout')
            eventSource.close()
            onError(new Error('Connection timeout'))
        }
    }, 320000) // 5分20秒超时

    // 清理超时
    const originalClose = eventSource.close
    eventSource.close = function () {
        clearTimeout(timeout)
        originalClose.call(this)
    }

    return eventSource
}

// 关闭SSE连接
export function closeSSEConnection(eventSource) {
    if (eventSource) {
        eventSource.close()
    }
}