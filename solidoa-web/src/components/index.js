/**
 * SolidOA 通用组件库 - 统一导出
 * 用法: app.use(globalComponents) 或 app.component('OaButton', OaButton)
 */
import OaButton from './OaButton/index.vue'
import OaCard from './OaCard/index.vue'
import OaDialog from './OaDialog/index.vue'
import OaStatusBadge from './OaStatusBadge/index.vue'
import OaTable from './OaTable/index.vue'
import OaApprovalCard from './OaApprovalCard/index.vue'
import OaApprovalFlow from './OaApprovalFlow/index.vue'

const components = [OaButton, OaCard, OaDialog, OaStatusBadge, OaTable, OaApprovalCard, OaApprovalFlow]

export {
  OaButton,
  OaCard,
  OaDialog,
  OaStatusBadge,
  OaTable,
  OaApprovalCard,
  OaApprovalFlow
}

/** Vue 插件安装方式（自动注册所有 OaXxx 为全局组件） */
export default {
  install(app) {
    for (const comp of components) {
      app.component(comp.name || comp.__name, comp)
    }
  }
}
