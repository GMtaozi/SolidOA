<template>
  <OaCard :title="title || '审批流程'" class="oa-approval-card">
    <!-- 业务信息头部 -->
    <template v-if="$slots.header" #header>
      <slot name="header" />
    </template>

    <!-- 加载状态 -->
    <div v-if="loading" class="oa-approval-card__loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载流程...</span>
    </div>

    <!-- 状态徽章 -->
    <div v-else class="oa-approval-card__body">
      <div v-if="flow" class="oa-approval-card__header-row">
        <div class="oa-approval-card__info">
          <span v-if="flow.businessNo" class="oa-approval-card__no">{{ flow.businessNo }}</span>
          <OaStatusBadge
            v-if="flow.currentState"
            :type="getStateBadgeType(flow.currentState)"
            :text="getStateText(flow.currentState)"
          />
        </div>
      </div>

      <!-- 节点流程图 -->
      <OaApprovalFlow
        v-if="flow && flow.nodes && flow.nodes.length > 0"
        :nodes="flow.nodes"
        :current-node-order="flow.currentNodeOrder"
      />

      <!-- 抄送人 -->
      <div v-if="flow && flow.ccUsers && flow.ccUsers.length > 0" class="oa-approval-card__cc">
        <div class="oa-approval-card__cc-title">抄送（已读 {{ ccReadCount }} / 共 {{ flow.ccUsers.length }}）</div>
        <div class="oa-approval-card__cc-list">
          <div v-for="cc in flow.ccUsers" :key="cc.id" class="oa-approval-card__cc-item">
            <el-avatar :size="24">{{ cc.approverName?.[0] || '?' }}</el-avatar>
            <span>{{ cc.approverName }}</span>
            <OaStatusBadge
              :type="cc.status === 'READ' ? 'success' : 'default'"
              :text="cc.status === 'READ' ? '已读' : '未读'"
              :dot="false"
            />
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div v-if="actions && actions.length > 0" class="oa-approval-card__actions">
        <OaButton
          v-for="act in actions"
          :key="act.key"
          :variant="act.variant || 'primary'"
          size="small"
          :disabled="act.disabled"
          @click="$emit(act.key, flow)"
        >
          <el-icon v-if="act.icon"><component :is="act.icon" /></el-icon>
          {{ act.label }}
        </OaButton>
      </div>
    </div>
  </OaCard>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Check, Close, Loading } from '@element-plus/icons-vue'
import { workflowApi } from '@/api/workflow'
import OaCard from '../OaCard/index.vue'
import OaStatusBadge from '../OaStatusBadge/index.vue'
import OaApprovalFlow from '../OaApprovalFlow/index.vue'
import OaButton from '../OaButton/index.vue'

defineOptions({ name: 'OaApprovalCard' })

const props = defineProps({
  /** 业务类型 LEAVE/EXPENSE/STAMP/PURCHASE */
  businessType: { type: String, required: true },
  /** 业务单据 ID */
  businessId: { type: [String, Number], required: true },
  /** 卡片标题（可选） */
  title: { type: String, default: '' },
  /** 流程图 API（默认调 workflowApi.getFlowGraph，可覆盖） */
  flowApi: { type: Function, default: null },
  /** 操作按钮配置 */
  actions: { type: Array, default: () => [] }
})

defineEmits(['approve', 'reject', 'withdraw', 'transfer', 'addSign', 'urge', 'record'])

const flow = ref(null)
const loading = ref(false)

const ccReadCount = computed(() => {
  if (!flow.value?.ccUsers) return 0
  return flow.value.ccUsers.filter(c => c.status === 'READ').length
})

async function loadFlow() {
  if (!props.businessId) return
  loading.value = true
  try {
    const api = props.flowApi || (() => workflowApi.getFlowGraph(props.businessType, props.businessId))
    const res = await api()
    flow.value = res.data?.data || res.data || null
  } catch (err) {
    console.error('[OaApprovalCard] 加载流程图失败', err)
    flow.value = null
  } finally {
    loading.value = false
  }
}

function getStateBadgeType(state) {
  return {
    DRAFT: 'default', PENDING: 'warning', APPROVING: 'primary',
    ADD_SIGNING: 'primary', APPROVED: 'success', REJECTED: 'danger',
    WITHDRAWN: 'info', CC: 'default', FINISHED: 'success'
  }[state] || 'default'
}

function getStateText(state) {
  return {
    DRAFT: '草稿', PENDING: '待审批', APPROVING: '审批中',
    ADD_SIGNING: '加签中', APPROVED: '已通过', REJECTED: '已拒绝',
    WITHDRAWN: '已撤回', CC: '抄送中', FINISHED: '已完成'
  }[state] || state
}

onMounted(() => loadFlow())
watch(() => [props.businessType, props.businessId], () => loadFlow())
</script>

<style lang="scss" scoped>
.oa-approval-card {
  &__loading {
    padding: $gap-3xl;
    text-align: center;
    color: $text-3;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $gap-sm;
  }

  &__body {
    padding: $gap-lg 0;
  }

  &__header-row {
    margin-bottom: $gap-lg;
  }

  &__info {
    display: flex;
    align-items: center;
    gap: $gap-md;
  }

  &__no {
    font-family: monospace;
    font-size: $fz-body;
    color: $text-1;
    font-weight: $fw-medium;
  }

  &__cc {
    margin-top: $gap-xl;
    padding-top: $gap-lg;
    border-top: 1px solid $divider;
  }

  &__cc-title {
    font-size: $fz-aux;
    color: $text-2;
    margin-bottom: $gap-md;
  }

  &__cc-list {
    display: flex;
    flex-direction: column;
    gap: $gap-sm;
  }

  &__cc-item {
    display: flex;
    align-items: center;
    gap: $gap-sm;
    font-size: $fz-aux;
    color: $text-1;
  }

  &__actions {
    margin-top: $gap-xl;
    padding-top: $gap-lg;
    border-top: 1px solid $divider;
    display: flex;
    gap: $gap-sm;
    flex-wrap: wrap;
  }
}
</style>
