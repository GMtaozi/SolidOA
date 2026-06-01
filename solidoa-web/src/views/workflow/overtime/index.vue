<template>
  <div class="overtime-wrapper">
    <div class="overtime-container">
      <div class="page-header">
        <h2 class="page-title">加班申请</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新增加班</span>
        </button>
      </div>

      <div class="tabs">
        <button class="tab-item" :class="{ active: activeTab === 'my' }" @click="activeTab = 'my'">
          <span class="tab-indicator"></span>我提交的
        </button>
        <button class="tab-item" :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'">
          <span class="tab-indicator"></span>待我审批
        </button>
      </div>

      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>加班单号</th>
              <th>申请日期</th>
              <th>开始时间</th>
              <th>结束时间</th>
              <th>时长(小时)</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tableData" :key="row.id">
              <td class="mono">{{ row.overtimeNo }}</td>
              <td class="mono">{{ row.applyDate }}</td>
              <td class="mono">{{ row.startTime }}</td>
              <td class="mono">{{ row.endTime }}</td>
              <td class="mono highlight">{{ row.hours }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(row.status)">
                  <span class="status-dot"></span>
                  {{ getStatusText(row.status) }}
                </span>
              </td>
              <td class="action-cell">
                <button class="action-btn view" @click="handleView(row)">查看</button>
                <button v-if="row.status === 'PENDING' && activeTab === 'my'" class="action-btn cancel" @click="handleCancel(row)">撤回</button>
                <button v-if="activeTab === 'pending'" class="action-btn approve" @click="handleApprove(row)">审批</button>
              </td>
            </tr>
            <tr v-if="tableData.length === 0">
              <td colspan="7" class="empty-cell"><span class="empty-text">暂无数据</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 加班表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">新增加班</h3>
          <button class="dialog-close" @click="dialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">开始时间</label>
              <input type="datetime-local" class="form-input" v-model="form.startTime" @change="calcHours" />
            </div>
            <div class="form-item">
              <label class="form-label">结束时间</label>
              <input type="datetime-local" class="form-input" v-model="form.endTime" @change="calcHours" />
            </div>
          </div>
          <div class="form-item">
            <label class="form-label">时长（小时）</label>
            <input type="number" class="form-input readonly" :value="form.hours" readonly step="0.1" min="0.1" />
          </div>
          <div class="form-item">
            <label class="form-label">加班原因</label>
            <textarea class="form-textarea" v-model="form.reason" placeholder="请输入加班原因" rows="4"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" @click="handleSubmit" :disabled="isSubmitting">{{ isSubmitting ? '提交中...' : '提交' }}</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">加班详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">加班单号</span>
              <span class="detail-value mono">{{ currentDetail.overtimeNo || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请日期</span>
              <span class="detail-value">{{ currentDetail.applyDate || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请人</span>
              <span class="detail-value">{{ currentDetail.userName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">申请部门</span>
              <span class="detail-value">{{ currentDetail.deptName || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">开始时间</span>
              <span class="detail-value mono">{{ currentDetail.startTime || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">结束时间</span>
              <span class="detail-value mono">{{ currentDetail.endTime || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">时长</span>
              <span class="detail-value highlight">{{ currentDetail.hours || 0 }} 小时</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态</span>
              <span class="detail-value">
                <span class="status-badge" :class="getStatusClass(currentDetail.status)">
                  <span class="status-dot"></span>
                  {{ getStatusText(currentDetail.status) }}
                </span>
              </span>
            </div>
            <div class="detail-item full-width">
              <span class="detail-label">加班原因</span>
              <span class="detail-value reason">{{ currentDetail.reason || '-' }}</span>
            </div>
          </div>
        </div>
        <div class="dialog-footer" v-if="activeTab === 'pending' && currentDetail.status === 'PENDING'">
          <button class="cyber-btn" @click="detailVisible = false">关闭</button>
          <button class="cyber-btn danger" @click="handleReject">拒绝</button>
          <button class="cyber-btn primary" @click="handleApproveConfirm">通过</button>
        </div>
        <div class="dialog-footer" v-else>
          <button class="cyber-btn primary" @click="detailVisible = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { hrApi } from '@/api/hr'
import { ElMessage, ElMessageBox } from 'element-plus'

const escapeHtml = (str) => {
  if (!str) return ''
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const activeTab = ref('my')
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const currentDetail = ref({})

const form = reactive({
  startTime: '',
  endTime: '',
  hours: 0,
  reason: ''
})

let submitLock = false
const withDebounce = async (fn, delay = 500) => {
  if (submitLock) { ElMessage.info('操作过于频繁，请稍后'); return true }
  submitLock = true
  try {
    return await fn()
  } finally {
    setTimeout(() => { submitLock = false }, delay)
  }
}

const getStatusClass = (status) => ({ PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' }[status] || '')
const getStatusText = (status) => ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status)

const calcHours = () => {
  if (form.startTime && form.endTime) {
    const start = new Date(form.startTime)
    const end = new Date(form.endTime)
    const diff = (end - start) / (1000 * 60 * 60)
    form.hours = Math.max(0, Math.round(diff * 10) / 10) // 四舍五入到0.1
  }
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my' ? await hrApi.getOvertimeList({}) : await workflowApi.getMyTasks()
    tableData.value = res.data?.data?.records || res.data?.data || []
  } catch (error) { console.error('加载数据失败', error) }
}

const handleAdd = () => {
  Object.assign(form, { startTime: '', endTime: '', hours: 0, reason: '' })
  dialogVisible.value = true
}

const handleView = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const handleApprove = (row) => {
  currentDetail.value = row
  ElMessageBox.confirm(
    `<div style="text-align:left">
      <p><strong>加班时间：</strong>${escapeHtml(row.startTime)} - ${escapeHtml(row.endTime)}</p>
      <p><strong>加班时长：</strong>${escapeHtml(row.hours)} 小时</p>
      <p><strong>加班原因：</strong>${escapeHtml(row.reason) || '-'}</p>
    </div>`,
    '确认通过加班申请？',
    { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    await withDebounce(async () => {
      await hrApi.approveOvertime(row.id, { approveResult: 'APPROVE', comment: '同意' })
      ElMessage.success('审批成功')
      loadData()
    })
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await withDebounce(async () => {
    await hrApi.approveOvertime(currentDetail.value.id, { approveResult: 'APPROVE', comment: '同意' })
    ElMessage.success('审批成功')
    detailVisible.value = false
    loadData()
  })
}

const handleReject = async () => {
  const { value: comment } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
    confirmButtonText: '确认', cancelButtonText: '取消',
    inputPattern: /\S+/, inputErrorMessage: '请输入拒绝原因'
  })
  await withDebounce(async () => {
    await hrApi.approveOvertime(currentDetail.value.id, { approveResult: 'REJECT', comment })
    ElMessage.success('已拒绝')
    detailVisible.value = false
    loadData()
  })
}

const handleCancel = async (row) => {
  if (row.status !== 'PENDING') {
    ElMessage.warning('只能撤回待审批状态的申请')
    return
  }
  await ElMessageBox.confirm('确定要撤回该加班申请吗？', '提示', { type: 'warning' })
  await withDebounce(async () => {
    await hrApi.cancelOvertime(row.id)
    ElMessage.success('撤回成功')
    loadData()
  })
}

const handleSubmit = async () => {
  if (!form.startTime) { ElMessage.warning('请选择开始时间'); return }
  if (!form.endTime) { ElMessage.warning('请选择结束时间'); return }
  if (form.hours <= 0) { ElMessage.warning('时长必须大于0'); return }
  if (!form.reason.trim()) { ElMessage.warning('请输入加班原因'); return }

  await withDebounce(async () => {
    try {
      await hrApi.createOvertime({ ...form })
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('提交失败')
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2; $bg-card: #ffffff; $primary: #60A5FA; $success: #34D399; $warning: #FBBF24; $danger: #FCA5A5; $text-primary: #3B3B3B; $text-secondary: #9CA3AF; $border: #F0EDE9;

.overtime-wrapper { min-height: 100vh; background: $bg-primary; }
.overtime-container { padding: 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.page-title { font-size: 18px; font-weight: 500; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } .btn-icon { font-size: 16px; } }
.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }
.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .highlight { color: $primary; font-weight: 600; } .empty-cell { text-align: center; padding: 60px 16px; .empty-text { color: $text-secondary; font-size: 14px; } } } }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: darken($warning, 20%); .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: darken($success, 12%); .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: darken($danger, 8%); .status-dot { background: $danger; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: $primary; background: rgba($primary, 0.05); } &.cancel:hover { color: darken($warning, 20%); border-color: $warning; } &.approve:hover { color: darken($success, 12%); border-color: $success; } }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.dialog { width: 100%; max-width: 480px; background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 500; color: $text-primary; }
.dialog-close { width: 32px; height: 32px; font-size: 20px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; max-height: 60vh; overflow-y: auto; }
.form-item { margin-bottom: 20px; }
.form-label { display: block; margin-bottom: 8px; font-size: 14px; font-weight: 500; color: $text-primary; }
.form-row { display: flex; gap: 16px; .form-item { flex: 1; } }
.form-input { width: 100%; padding: 10px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } &.readonly { background: darken($bg-primary, 3%); color: $primary; font-weight: 600; } }
.form-textarea { width: 100%; padding: 10px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; resize: vertical; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; font-family: inherit; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; }
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: darken($danger, 8%); } } }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; &.full-width { grid-column: 1 / -1; } }
.detail-label { font-size: 12px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } &.highlight { color: $primary; font-weight: 600; } &.reason { line-height: 1.6; color: $text-secondary; } }

// 响应式设计
@media (max-width: 768px) {
  .overtime-container { padding: 16px; }
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
  .form-row { flex-direction: column; }
  .dialog-footer { flex-direction: column; gap: 8px; padding: 16px; }
  .cyber-btn { width: 100%; text-align: center; }
}

@media (max-width: 480px) {
  .overtime-container { padding: 12px; }
  .detail-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 15px; }
}
</style>