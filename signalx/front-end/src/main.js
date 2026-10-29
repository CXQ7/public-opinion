import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import 'amfe-flexible'
import * as request from './utils/request'
import { bridge } from './utils/bridge'
import tools from './utils'
import Vant from 'vant'
import 'vant/lib/index.css'

const app = createApp(App)

app.config.globalProperties.$request = request
app.config.globalProperties.$bridge = bridge
app.config.globalProperties.$tools = tools

app.use(Vant)
app.use(router)

app.mount('#app')
