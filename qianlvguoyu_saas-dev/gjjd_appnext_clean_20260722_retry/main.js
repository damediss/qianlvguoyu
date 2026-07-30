import App from './App'

// #ifndef VUE3
import Vue from 'vue'
import util from '@/utils/util.js'
import { request } from '@/utils/index.js'
import './uni.promisify.adaptor'
Vue.config.productionTip = false
//全局页面变量
Vue.prototype.$util = util
Vue.prototype.$http = { request }
Vue.prototype.$statusBarHeight = uni.getSystemInfoSync().statusBarHeight
Vue.prototype.$winWidth = uni.getSystemInfoSync().windowWidth
Vue.prototype.$winHeight = uni.getSystemInfoSync().windowHeight

App.mpType = 'app'
const app = new Vue({
  ...App
})
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import util from './utils/util.js'
import { request } from './utils/index.js'

export function createApp() {
  const app = createSSRApp(App)
  
  // 创建 Pinia 实例
  const pinia = createPinia()
  app.use(pinia)
  
  // 全局属性
  app.config.globalProperties.$statusBarHeight = uni.getSystemInfoSync().statusBarHeight
  app.config.globalProperties.$winWidth = uni.getSystemInfoSync().windowWidth
  app.config.globalProperties.$winHeight = uni.getSystemInfoSync().windowHeight
  app.config.globalProperties.$areaHeight = uni.getSystemInfoSync().safeArea.height
  app.config.globalProperties.$util = util
  app.config.globalProperties.$http = { request }
  
  return {
    app,
    Pinia: pinia
  }
}
// #endif