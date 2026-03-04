import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import 'element-plus/theme-chalk/el-message.css'
import './styles/design-system.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
