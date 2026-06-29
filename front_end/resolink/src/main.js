import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
// 导入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 导入Element Plus中文语言包
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import './style.css'
import './elementui.custom.css'
import App from './App.vue'
import { useUserInfoStore } from './stores/userInfo'

const app = createApp(App)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
// 使用Element Plus并配置中文
app.use(ElementPlus, {
  locale: zhCn
})

// 初始化用户信息store，清除可能存在的旧token数据
const userInfoStore = useUserInfoStore()
userInfoStore.initialize()

app.mount('#app')