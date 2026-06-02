<template>
  <div class="cc-wrapper">
    <div class="cc-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">抄送我的</h2>
        <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
      </div>

      <!-- 数据表格 -->
      <div class="table-wrapper">
        <OaTable
          :data="tableData"
          :columns="columns"
          :total="total"
          :page="query.page"
          :size="query.size"
          @update:page="p => { query.page = p; loadData() }"
          @update:size="s => { query.size = s; query.page = 1; loadData() }"
        >
          <template #businessType="{ row }">
            <span class="type-badge" :class="getTypeClass(row.businessType)">
              {{ row.businessTypeName }}
            </span>
          </template>
          <template #isRead="{ row }">
            <OaStatusBadge
              :type="row.isRead ? 'success' : 'warning'"
              :text="row.isRead ? '已读' : '未读'"
            />
          </template>
          <template #actions="{ row }">
            <OaButton variant="ghost" size="small" @click="handleView(row)">查看</OaButton>
            <OaButton
              v-if="!row.isRead"
              variant="primary"
              size="small"
              @click="handleMarkRead(row)"
            >
              标记已读
            </OaButton>
          </template>
        </OaTable>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <button
          class="page-btn"
          :disabled="page <= 1"
          @click="page--; loadData()"
        >上一页</button>
        <span class="page-info">{{ page }} / {{ totalPages }}</span>
        <button
          class="page-btn"
          :disabled="page >= totalPages"
          @click="page++; loadData()"
        >下一页</button>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h3 class="dialog-title">{{ currentRow?.businessTypeName }} - 详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="info-grid">
            <div class="info-item">
              <label>业务编号</label>
              <span>{{ currentRow?.businessId }}</span>
            </div>
            <div class="info-item">
              <label>抄送时间</label>
              <span>{{ formatTime(currentRow?.createTime) }}</span>
            </div>
            <div class="info-item">
              <label>阅读状态</label>
              <span>{{ currentRow?.isRead ? '已读' : '未读' }}</span>
            </div>
          </div>
          <div class="info-section" v-if="flowData">
            <h4 class="section-title">审批流程</h4>
            <OaApprovalFlow
              v-if="flowData.nodes && flowData.nodes.length > 0"
              :nodes="flowData.nodes"
              :current-node-order="currentNodeOrder"
            />
            <el-empty v-else description="暂无审批节点" :image-size="60" />
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="detailVisible = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { ElMessage } from 'element-plus'
import { Check, Close, Clock } from '@element-plus/icons-vue'

const tableData = ref([])
const unreadCount = ref(0)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const detailVisible = ref(false)
const currentRow = ref(null)
const flowData = ref(null)
const currentNodeOrder = computed(() => {
  const data = flowData.value
  if (!data?.nodes) return -1
  const pending = data.nodes.find(n => n.status === 'PENDING')
  return pending ? pending.order : -1
})

// OaTable 分页 v-model 适配（page/size 双向绑定）
const query = reactive({
  get page() { return page.value },
  set page(v) { page.value = v },
  get size() { return pageSize.value },
  set size(v) { pageSize.value = v }
})

// 表格列定义
const columns = [
  { prop: 'businessType', label: '类型', width: 110 },
  { prop: 'businessId', label: '业务编号', minWidth: 160 },
  { prop: 'createTime', label: '抄送时间', width: 180, formatter: (val) => formatTime(val) },
  { prop: 'isRead', label: '状态', width: 100 }
]

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const getTypeClass = (type) => {
  const map = { LEAVE: 'leave', EXPENSE: 'expense', STAMP: 'stamp', PURCHASE: 'purchase' }
  return map[type] || ''
}

const getTypeName = (type) => {
  const map = { LEAVE: '请假', EXPENSE: '报销', STAMP: '用印', PURCHASE: '采购' }
  return map[type] || type
}

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const loadData = async () => {
  try {
    const res = await workflowApi.getMyCcList({ page: page.value, size: pageSize.value })
    tableData.value = res.data?.data?.records || res.data?.data || []
    total.value = res.data?.data?.total || 0

    // 获取未读数量
    const unreadRes = await workflowApi.getMyCcUnreadCount()
    unreadCount.value = unreadRes.data?.data || 0
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const handleView = async (row) => {
  currentRow.value = row
  detailVisible.value = true
  flowData.value = null

  // 加载审批流程 - 安全的动态API调用
  try {
    const typeToApiMap = {
      LEAVE: 'getLeaveFlow',
      EXPENSE: 'getExpenseFlow',
      STAMP: 'getStampFlow',
      PURCHASE: 'getPurchaseFlow'
    }
    const flowApi = typeToApiMap[row.businessType]
    if (flowApi && typeof workflowApi[flowApi] === 'function') {
      const res = await workflowApi[flowApi](row.businessId)
      flowData.value = res.data
    }
  } catch (error) {
    console.error('加载流程信息失败', error)
    // 保持flowData为null，不显示流程信息
  }
}

const handleMarkRead = async (row) => {
  try {
    await workflowApi.markCcAsRead(row.id)
    row.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    ElMessage.success('已标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$danger: #FCA5A5;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border: #F0EDE9;

.cc-wrapper {
  min-height: 100vh;
  background: $bg-primary;
}

.cc-container {
  padding: 30px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;
  margin: 0;
}

.unread-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 12px;
  font-weight: 500;
  color: #ffffff;
  background: $danger;
  border-radius: 10px;
}

.table-wrapper {
  background: $bg-card;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
}

.data-table {
  width: 100%;
  border-collapse: collapse;

  thead {
    background: $bg-primary;

    th {
      padding: 14px 16px;
      font-size: 13px;
      font-weight: 500;
      color: $text-secondary;
      text-align: left;
      border-bottom: 1px solid $border;
    }
  }

  tbody {
    tr {
      transition: background 0.15s ease;

      &:hover {
        background: rgba($primary, 0.03);
      }

      &:not(:last-child) td {
        border-bottom: 1px solid $border;
      }
    }

    td {
      padding: 16px;
      font-size: 14px;
      color: $text-primary;
    }

    .mono {
      font-family: 'SF Mono', 'Monaco', monospace;
      font-size: 13px;
    }
  }
}

.type-badge {
  display: inline-block;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;

  &.leave { background: rgba($primary, 0.2); color: darken($primary, 8%); }
  &.expense { background: rgba($success, 0.2); color: darken($success, 12%); }
  &.stamp { background: rgba($warning, 0.2); color: darken($warning, 20%); }
  &.purchase { background: rgba($danger, 0.2); color: darken($danger, 8%); }
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: $text-secondary;
  margin-right: 6px;

  &.unread {
    background: $primary;
  }
}

.action-cell {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 14px;
  font-size: 13px;
  color: $text-secondary;
  background: transparent;
  border: 1px solid $border;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    color: $primary;
    border-color: $primary;
    background: rgba($primary, 0.05);
  }
}

.empty-cell {
  text-align: center;
  padding: 60px 16px;

  .empty-text {
    color: $text-secondary;
    font-size: 14px;
  }
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.page-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: $text-secondary;
  background: $bg-card;
  border: 1px solid $border;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover:not(:disabled) {
    color: $primary;
    border-color: $primary;
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.page-info {
  font-size: 14px;
  color: $text-secondary;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  backdrop-filter: blur(2px);
}

.dialog {
  width: 100%;
  max-width: 480px;
  background: $bg-card;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.15);

  &.dialog-lg {
    max-width: 600px;
  }
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid $border;
}

.dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;
}

.dialog-close {
  width: 32px;
  height: 32px;
  font-size: 20px;
  color: $text-secondary;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    color: $text-primary;
    background: $bg-primary;
  }
}

.dialog-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 24px;
  border-top: 1px solid $border;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.info-item {
  label {
    display: block;
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: 4px;
  }

  span {
    font-size: 14px;
    color: $text-primary;
  }
}

.info-section {
  margin-top: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 12px;
  padding-left: 12px;
  
}

.flow-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.flow-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: $bg-primary;
  border-radius: 12px;

  .flow-icon {
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    flex-shrink: 0;
  }

  .flow-info {
    flex: 1;
  }

  .flow-name {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }

  .flow-mode {
    font-size: 12px;
    color: $text-secondary;
  }

  .flow-time {
    font-size: 12px;
    color: $text-secondary;
  }

  .flow-comment {
    font-size: 13px;
    color: $text-secondary;
    margin-top: 4px;
  }

  &.approved {
    .flow-icon {
      background: $success;
      color: #ffffff;
    }
  }

  &.pending {
    .flow-icon {
      background: $border;
      color: $text-secondary;
    }
  }

  &.rejected {
    .flow-icon {
      background: $danger;
      color: #ffffff;
    }
  }
}

.cyber-btn {
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: $text-secondary;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    color: $text-primary;
    background: $border;
  }

  &.primary {
    color: #ffffff;
    background: $primary;
    border-color: $primary;
    box-shadow: 0 4px 12px rgba($primary, 0.25);

    &:hover {
      background: darken($primary, 8%);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .cc-container { padding: 16px; }
  .page-header { flex-direction: column; gap: 16px; align-items: flex-start; }
  .page-title { font-size: 16px; }
  .btn-primary { width: 100%; justify-content: center; padding: 12px 20px; }
  .tabs { flex-direction: column; }
  .table-wrapper { overflow-x: auto; }
  .data-table { min-width: 500px; }
  .action-cell { flex-direction: column; gap: 6px; }
  .action-btn { width: 100%; padding: 10px 14px; }
  .dialog { max-width: 100%; margin: 16px; border-radius: 12px; }
  .dialog-body { padding: 16px; }
  .dialog-footer { flex-direction: column; gap: 8px; padding: 16px; }
  .cyber-btn { width: 100%; text-align: center; }
}

@media (max-width: 480px) {
  .cc-container { padding: 12px; }
  .page-title { font-size: 15px; }
}
</style>