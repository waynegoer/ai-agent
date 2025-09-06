# Wayne Agent Frontend

AI聊天应用前端项目，包含AI恋爱大师和AI超级智能体两个聊天应用。

## 功能特性

- 🏠 **主页**: 用于切换不同的AI应用
- 💕 **AI恋爱大师**: 专门处理恋爱相关问题的聊天应用
- 🤖 **AI超级智能体**: 通用AI助手，处理各种任务和问题
- 🔄 **实时对话**: 通过SSE实现实时流式对话
- 📱 **响应式设计**: 支持桌面和移动设备
- 🌐 **日语界面**: 完全日语化的用户界面

## 技术栈

- Vue 3
- Vue Router 4
- Pinia (状态管理)
- Axios (HTTP请求)
- Vite (构建工具)

## 安装和运行

1. 安装依赖：

```bash
npm install
```

2. 启动开发服务器：

```bash
npm run dev
```

3. 构建生产版本：

```bash
npm run build
```

## 后端接口

项目需要连接到运行在 `http://localhost:8123` 的后端服务。

### 接口端点

- `GET /api/ai/love_app/chat/sse_emitter` - AI恋爱大师SSE接口
- `GET /api/ai/manus/chat` - AI超级智能体SSE接口

## 项目结构

```
src/
├── api/           # API服务模块
├── components/    # Vue组件
├── stores/        # Pinia状态管理
├── main.js        # 应用入口
└── App.vue        # 根组件
```

## 使用说明

1. 访问主页选择要使用的AI应用
2. 在聊天界面输入消息并发送
3. AI会通过SSE实时返回回复
4. 每个应用都有独立的聊天历史记录
5. 可以随时清空聊天记录或返回主页


