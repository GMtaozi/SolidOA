import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import {
  ArrowDown, ArrowRight, Bell, Calendar, CircleCheck, Clock,
  Collection, Connection, DataLine, Delete, Document, Download,
  Expand, Grid, List, Lock, Money, OfficeBuilding, Plus, Refresh,
  Select, SwitchButton, Upload, Warning
} from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
// 引入设计系统基础层（顺序敏感：先 EP 主题覆盖覆盖 Element Plus 默认色，再 reset）
// 注意: tokens.scss 和 mixins.scss 不在此 import，由 vite additionalData 注入到所有 <style lang="scss">
import './styles/element-overrides.scss'
import './styles/reset.scss'
import './styles/variables.scss' // 兼容层（已废弃，下个 Sprint 移除）
// 注册全局通用组件（OaButton / OaCard / OaDialog / OaStatusBadge / OaTable）
import GlobalComponents from '@/components'

const app = createApp(App)

// === 按需注册 Element Plus 图标（25 个，覆盖 100% 现有用法） ===
// 如未来有新图标，先在此白名单添加，再去页面使用
const icons = [
  ArrowDown, ArrowRight, Bell, Calendar, CircleCheck, Clock,
  Collection, Connection, DataLine, Delete, Document, Download,
  Expand, Grid, List, Lock, Money, OfficeBuilding, Plus, Refresh,
  Select, SwitchButton, Upload, Warning
]
for (const icon of icons) {
  app.component(icon.name, icon)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)
app.use(GlobalComponents) // 注册 5 个 OaXxx 全局组件

app.mount('#app')
