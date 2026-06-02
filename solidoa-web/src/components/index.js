/**
 * SolidOA 通用组件库 - 统一导出
 * 用法: app.use(globalComponents) 或 app.component('OaButton', OaButton)
 */
import OaButton from './OaButton/index.vue'
import OaCard from './OaCard/index.vue'
import OaDialog from './OaDialog/index.vue'
import OaFormDialog from './OaFormDialog/index.vue'
import OaStatusBadge from './OaStatusBadge/index.vue'
import OaTable from './OaTable/index.vue'
import OaApprovalCard from './OaApprovalCard/index.vue'
import OaApprovalFlow from './OaApprovalFlow/index.vue'
import OaEmpty from './OaEmpty/index.vue'
import OaPagination from './OaPagination/index.vue'
import OaSearchForm from './OaSearchForm/index.vue'
import OaPageHeader from './OaPageHeader/index.vue'
import OaIcon from './OaIcon/index.vue'

const components = [
  OaButton, OaCard, OaDialog, OaFormDialog, OaStatusBadge, OaTable,
  OaApprovalCard, OaApprovalFlow, OaEmpty, OaPagination,
  OaSearchForm, OaPageHeader, OaIcon
]

export {
  OaButton,
  OaCard,
  OaDialog,
  OaFormDialog,
  OaStatusBadge,
  OaTable,
  OaApprovalCard,
  OaApprovalFlow,
  OaEmpty,
  OaPagination,
  OaSearchForm,
  OaPageHeader,
  OaIcon
}

/** Vue 插件安装方式（自动注册所有 OaXxx 为全局组件） */
export default {
  install(app) {
    for (const comp of components) {
      app.component(comp.name || comp.__name, comp)
    }
  }
}
