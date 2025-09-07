// API配置文件
// 根据环境变量或默认值设置API基础URL

const getApiBaseUrl = () => {
    // 优先使用环境变量
    if (import.meta.env.VITE_API_BASE_URL) {
        return import.meta.env.VITE_API_BASE_URL
    }

    // 根据当前域名判断环境
    const hostname = window.location.hostname

    // 开发环境
    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:8123/api'
    }

    // 生产环境 - 使用您的腾讯云托管后端
    return 'https://springboot-xrsq-185054-5-1377738459.sh.run.tcloudbase.com/api'
}

export const API_BASE_URL = getApiBaseUrl()

// 导出配置信息
export const API_CONFIG = {
    baseURL: API_BASE_URL,
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
}

// 开发时打印配置信息
if (import.meta.env.DEV) {
    console.log('API配置:', {
        baseURL: API_BASE_URL,
        environment: import.meta.env.MODE,
        hostname: window.location.hostname
    })
}
