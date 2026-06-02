<template>
  <div class="goout-wrapper">
    <div class="goout-container">
      <div class="page-header">
        <h2 class="page-title">外出申请</h2>
        <button class="btn-primary" @click="handleAdd" aria-label="新建外出申请">
          <span class="btn-icon" aria-hidden="true">+</span>
          <span>新建外出</span>
        </button>
      </div>

      <div class="tabs">
        <button class="tab-item" :class="{ active: activeTab === 'my' }" @click="activeTab = 'my'; loadData()">
          <span class="tab-indicator"></span>我提交的
        </button>
        <button class="tab-item" :class="{ active: activeTab === 'pending' }" @click="activeTab = 'pending'; loadData()">
          <span class="tab-indicator"></span>待我审批
        </button>
      </div>

      <div class="table-wrapper" role="region" aria-label="外出申请列表">
        <OaTable
          :data="tableData"
          :columns="columns"
          :total="total"
          :page="query.page"
          :size="query.size"
          @update:page="p => { query.page = p; loadData() }"
          @update:size="s => { query.size = s; query.page = 1; loadData() }"
        >
          <template #status="{ row }">
            <OaStatusBadge
              :type="getBadgeType(row.status)"
              :text="getStatusText(row.status)"
            />
          </template>
          <template #actions="{ row }">
            <OaButton variant="ghost" size="small" @click="handleView(row)">查看</OaButton>
            <OaButton
              v-if="row.status === 'PENDING' && activeTab === 'my'"
              variant="danger"
              size="small"
              @click="handleCancel(row)"
            >
              撤回
            </OaButton>
            <OaButton
              v-if="activeTab === 'pending'"
              variant="primary"
              size="small"
              @click="handleApprove(row)"
            >
              审批
            </OaButton>
          </template>
        </OaTable>
      </div>

      <nav class="pagination" v-if="total > 0" role="navigation" aria-label="分页导航">
        <span class="page-info" aria-live="polite">共 {{ total }} 条</span>
        <button class="page-btn" :disabled="pageNum <= 1" @click="pageNum--; loadData()" aria-label="上一页">上一页</button>
        <span class="page-num" aria-current="page">{{ pageNum }}</span>
        <button class="page-btn" :disabled="pageNum * pageSize >= total" @click="pageNum++; loadData()" aria-label="下一页">下一页</button>
      </nav>
    </div>

    <!-- 新建外出弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" role="dialog" aria-modal="true" aria-labelledby="goout-dialog-title" @click.self="dialogVisible = false" @keydown.esc="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title" id="goout-dialog-title">新建外出申请</h3>
          <button class="dialog-close" @click="dialogVisible = false" aria-label="关闭弹窗">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label" for="goout-date">外出日期</label>
            <input id="goout-date" type="date" class="form-input" v-model="form.outDate" required aria-required="true" />
          </div>
          <div class="form-row">
            <div class="form-item">
              <label class="form-label" for="goout-start">开始时间</label>
              <input id="goout-start" type="datetime-local" class="form-input" v-model="form.startTime" required aria-required="true" />
            </div>
            <div class="form-item">
              <label class="form-label" for="goout-end">结束时间</label>
              <input id="goout-end" type="datetime-local" class="form-input" v-model="form.endTime" required aria-required="true" />
            </div>
          </div>
          <div class="form-item">
            <label class="form-label" for="goout-destination">外出地点</label>
            <input id="goout-destination" type="text" class="form-input" v-model="form.destination" placeholder="请输入外出地点" required aria-required="true" />
          </div>
          <div class="form-item">
            <label class="form-label" for="goout-reason">外出事由</label>
            <textarea id="goout-reason" class="form-textarea" v-model="form.reason" placeholder="请输入外出事由" rows="3" required aria-required="true"></textarea>
          </div>
          <div class="form-item">
            <label class="form-label">附件上传</label>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="10"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :file-list="form.attachments"
              accept=".jpg,.jpeg,.png,.pdf,.doc,.docx"
              multiple
            >
              <el-button type="primary" plain size="small">
                <el-icon><Plus /></el-icon> 选择文件
              </el-button>
              <template #tip>
                <div class="upload-tip">支持 jpg、png、pdf、doc、docx 格式，最多10个文件</div>
              </template>
            </el-upload>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" @click="handleSubmit" :disabled="isSubmitting || uploading">
            {{ (isSubmitting || uploading) ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" role="dialog" aria-modal="true" aria-labelledby="goout-detail-title" @click.self="detailVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title" id="goout-detail-title">外出详情</h3>
          <button class="dialog-close" @click="detailVisible = false" aria-label="关闭详情">×</button>
        </div>
        <div class="dialog-body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">外出单号</span>
              <span class="detail-value mono">{{ currentDetail.outNo || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">外出日期</span>
              <span class="detail-value mono">{{ currentDetail.outDate || '-' }}</span>
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
              <span class="detail-value mono">{{ formatTime(currentDetail.startTime) || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">结束时间</span>
              <span class="detail-value mono">{{ formatTime(currentDetail.endTime) || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">外出地点</span>
              <span class="detail-value">{{ currentDetail.destination || '-' }}</span>
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
              <span class="detail-label">外出事由</span>
              <span class="detail-value reason">{{ currentDetail.reason || '-' }}</span>
            </div>
            <div class="detail-item" v-if="currentDetail.approverName">
              <span class="detail-label">审批人</span>
              <span class="detail-value">{{ currentDetail.approverName }}</span>
            </div>
            <div class="detail-item" v-if="currentDetail.approvedTime">
              <span class="detail-label">审批时间</span>
              <span class="detail-value mono">{{ formatTime(currentDetail.approvedTime) }}</span>
            </div>
            <div class="detail-item full-width" v-if="currentDetail.approverComment">
              <span class="detail-label">审批意见</span>
              <span class="detail-value reason">{{ currentDetail.approverComment }}</span>
            </div>
          </div>

          <!-- 审批流程图（V2.0 接入 State Machine） -->
          <OaApprovalCard
            v-if="currentDetail && currentDetail.id"
            title="审批流程"
            business-type="GO_OUT"
            :business-id="currentDetail.id"
            class="detail-flow-card"
          />
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
import { hrApi } from '@/api/hr'
import { workflowApi } from '@/api/workflow'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFiles } from '@/utils/upload'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('my')
const tableData = ref([])
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const uploading = ref(false)
const currentDetail = ref({})

// 表格列定义
const columns = [
  { prop: 'outNo', label: '外出单号', width: 160 },
  { prop: 'outDate', label: '外出日期', width: 120 },
  { prop: 'destination', label: '外出地点', minWidth: 180 },
  { prop: 'startTime', label: '开始时间', width: 160, formatter: (val) => formatTime(val) },
  { prop: 'endTime', label: '结束时间', width: 160, formatter: (val) => formatTime(val) },
  { prop: 'status', label: '状态', width: 110 }
]

// 状态 -> OaStatusBadge type
const getBadgeType = (status) => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info'
}[status] || 'default')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const uploadRef = ref(null)

const form = reactive({
  outDate: '',
  startTime: '',
  endTime: '',
  destination: '',
  reason: '',
  attachments: []
})

let submitLock = false
const withDebounce = async (fn, delay = 500) => {
  if (submitLock) { ElMessage.info('操作过于频繁，请稍后'); return }
  submitLock = true
  try {
    return await fn()
  } finally {
    setTimeout(() => { submitLock = false }, delay)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

const getStatusClass = (status) => ({
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
  CANCELLED: 'cancelled'
}[status] || '')

const getStatusText = (status) => ({
  PENDING: '审批中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  CANCELLED: '已撤回'
}[status] || status)

// HTML转义函数，防止XSS攻击
const escapeHtml = (str) => {
  if (!str) return '-'
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my'
      ? await hrApi.getGoOutList({ pageNum: query.page, pageSize: query.size })
      : await workflowApi.getMyTasks()
    const data = res.data?.data
    tableData.value = data?.records || res.data?.data || []
    total.value = data?.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const handleAdd = () => {
  const today = new Date().toISOString().split('T')[0]
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  Object.assign(form, {
    outDate: today,
    startTime: now.toISOString().slice(0, 16),
    endTime: '',
    destination: '',
    reason: '',
    attachments: []
  })
  dialogVisible.value = true
}

const handleFileChange = (file, fileList) => {
  form.attachments = fileList
}

const handleFileRemove = (file, fileList) => {
  form.attachments = fileList
}

const handleView = async (row) => {
  try {
    const res = await hrApi.getGoOutById(row.id)
    currentDetail.value = res.data || row
  } catch {
    currentDetail.value = row
  }
  detailVisible.value = true
}

const handleApprove = (row) => {
  currentDetail.value = row
  ElMessageBox.confirm(
    `<div style="text-align:left">
      <p><strong>外出日期：</strong>${escapeHtml(row.outDate)}</p>
      <p><strong>外出地点：</strong>${escapeHtml(row.destination)}</p>
      <p><strong>开始时间：</strong>${escapeHtml(formatTime(row.startTime))}</p>
      <p><strong>结束时间：</strong>${escapeHtml(formatTime(row.endTime))}</p>
      <p><strong>外出事由：</strong>${escapeHtml(row.reason)}</p>
    </div>`,
    '确认通过外出申请？',
    { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    await withDebounce(async () => {
      await hrApi.approveGoOut(row.id, { result: 'APPROVE' })
      ElMessage.success('审批成功')
      loadData()
    })
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await withDebounce(async () => {
    await hrApi.approveGoOut(currentDetail.value.id, { result: 'APPROVE' })
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
    await hrApi.approveGoOut(currentDetail.value.id, { result: 'REJECT', comment })
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
  await ElMessageBox.confirm('确定要撤回该外出申请吗？', '提示', { type: 'warning' })
  await withDebounce(async () => {
    await hrApi.cancelGoOut(row.id)
    ElMessage.success('撤回成功')
    loadData()
  })
}

const handleSubmit = async () => {
  if (!form.outDate) { ElMessage.warning('请选择外出日期'); return }
  if (!form.startTime) { ElMessage.warning('请选择开始时间'); return }
  if (!form.endTime) { ElMessage.warning('请选择结束时间'); return }
  if (!form.destination.trim()) { ElMessage.warning('请输入外出地点'); return }
  if (!form.reason.trim()) { ElMessage.warning('请输入外出事由'); return }

  if (new Date(form.endTime) <= new Date(form.startTime)) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  await withDebounce(async () => {
    isSubmitting.value = true
    uploading.value = true
    try {
      const fileList = form.attachments.filter(f => f.raw).map(f => f.raw)
      let attachmentUrls = []
      if (fileList.length > 0) {
        attachmentUrls = await uploadFiles(fileList)
      }

      const data = {
        ...form,
        attachments: attachmentUrls.join(',')
      }

      await hrApi.createGoOut(data)
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('提交失败')
      console.error('提交失败', error)
    } finally {
      isSubmitting.value = false
      uploading.value = false
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2; $bg-card: #ffffff; $primary: #60A5FA; $success: #34D399; $warning: #FBBF24; $danger: #FCA5A5; $text-primary: #3B3B3B; $text-secondary: #9CA3AF; $border: #F0EDE9;

.goout-wrapper { min-height: 100vh; background: $bg-primary; }
.goout-container { padding: 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.page-title { font-size: 18px; font-weight: 500; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } .btn-icon { font-size: 16px; } }
.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }
.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .empty-cell { text-align: center; padding: 60px 16px; .empty-text { color: $text-secondary; font-size: 14px; } } } }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: darken($warning, 20%); .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: darken($success, 12%); .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: darken($danger, 8%); .status-dot { background: $danger; } } &.cancelled { background: rgba($text-secondary, 0.15); color: $text-secondary; .status-dot { background: $text-secondary; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: $primary; background: rgba($primary, 0.05); } &.cancel:hover { color: darken($warning, 20%); border-color: $warning; } &.approve:hover { color: darken($success, 12%); border-color: $success; } }
.pagination { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 20px; padding: 16px 0; }
.page-info { font-size: 13px; color: $text-secondary; }
.page-btn { padding: 8px 16px; font-size: 13px; color: $text-secondary; background: $bg-card; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover:not(:disabled) { color: $primary; border-color: $primary; } &:disabled { opacity: 0.5; cursor: not-allowed; } }
.page-num { min-width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; font-size: 14px; font-weight: 500; color: $primary; background: rgba($primary, 0.08); border-radius: 8px; }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.dialog { width: 100%; max-width: 560px; background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 500; color: $text-primary; }
.dialog-close { width: 32px; height: 32px; font-size: 20px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; max-height: 70vh; overflow-y: auto; }
.form-item { margin-bottom: 20px; }
.form-label { display: block; margin-bottom: 8px; font-size: 14px; font-weight: 500; color: $text-primary; }
.form-row { display: flex; gap: 16px; .form-item { flex: 1; } }
.form-input { width: 100%; padding: 10px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.form-textarea { width: 100%; padding: 10px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; resize: vertical; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; font-family: inherit; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; }
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: darken($danger, 8%); } } }
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; &.full-width { grid-column: 1 / -1; } }
.detail-label { font-size: 12px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } &.reason { line-height: 1.6; color: $text-secondary; } }
.upload-tip { margin-top: 8px; font-size: 12px; color: $text-secondary; }

// 响应式设计
@media (max-width: 768px) {
  .goout-container { padding: 16px; }
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
  .pagination { justify-content: center; }
}

@media (max-width: 480px) {
  .goout-container { padding: 12px; }
  .detail-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 15px; }
  .status-badge { padding: 3px 8px; font-size: 11px; }
}
</style>
