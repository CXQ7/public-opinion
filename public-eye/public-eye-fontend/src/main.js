import './main.css'

import App from './App.vue'
import ElementPlus from 'element-plus'
import { createApp } from 'vue'
import 'element-plus/dist/index.css'
import router from './router'
import VueCookies from "vue-cookies";
// import '@fortawesome/fontawesome-free/css/all.css';

// icon 图标引入
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)
app.config.globalProperties.$cookies = VueCookies
// icon 图标挂载
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(router)
// 挂载到app实例上
app.use(ElementPlus)

app.mount('#app')


// Vue.config.productionTip = false
