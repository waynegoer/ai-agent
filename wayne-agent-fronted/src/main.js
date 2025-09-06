import {createApp} from 'vue'
import {createRouter, createWebHistory} from 'vue-router'
import {createPinia} from 'pinia'
import App from './App.vue'
import HomePage from './components/HomePage.vue'
import LoveAppChat from './components/LoveAppChat.vue'
import ManusAppChat from './components/ManusAppChat.vue'
import TestPage from './components/TestPage.vue'

// 路由配置
const routes = [
    {
        path: '/',
        name: 'Home',
        component: HomePage
    },
    {
        path: '/test',
        name: 'Test',
        component: TestPage
    },
    {
        path: '/love-app',
        name: 'LoveApp',
        component: LoveAppChat
    },
    {
        path: '/manus-app',
        name: 'ManusApp',
        component: ManusAppChat
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.mount('#app')
