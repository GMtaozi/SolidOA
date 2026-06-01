<template>
  <div class="repair-wrapper">
    <div class="repair-container">
      <div class="page-header">
        <h2 class="page-title">补卡申请</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新增补卡</span>
        </button>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-cards">
        <div class="stat-card">
          <div class="stat-icon blue">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26L12 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.remainingCount || 0 }}</span>
            <span class="stat-label">本月剩余次数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon yellow">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/><polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </div>
          <div class="stat-content">
            <span class="stat-value used">{{ stats.usedCount || 0 }}</span>
            <span class="stat-label">本月已申请</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon green">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><polyline points="22,4 12,14.01 9,11.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
          </div>
          <div class="stat-content">
            <span class="stat-value">{{ stats.approvedCount || 0 }}</span>
            <span class="stat-label">已通过</span>
          </div>
        </div>
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
              <th>补卡单号</th>
              <th>申请日期</th>
              <th>补卡类型</th>
              <th>打卡类型</th>
              <th>补卡时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tableData" :key="row.id">
              <td class="mono">{{ row.repairNo }}</td>
              <td class="mono">{{ row.applyDate }}</td>
              <td>
                <span class="type-badge" :class="getTypeClass(row.repairType)">
                  {{ typeMap[row.repairType] || row.repairType }}
                </span>
              </td>
              <td>
                <span class="clock-badge" :class="row.clockType === 'CLOCK_IN' ? 'in' : 'out'">
                  {{ row.clockType === 'CLOCK_IN' ? '上班' : '下班' }}
                </span>
              </td>
              <td class="mono highlight">{{ row.repairTime }}</td>
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
              <td colspan="7" class="empty-cell">
                <div class="empty-state">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" ry="2" stroke="currentColor" stroke-width="2"/><line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2" stroke-linecap="round"/><line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/></svg>
                  <span>暂无补卡记录</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 补卡申请表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">新增补卡</h3>
          <button class="dialog-close" @click="dialogVisible = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <!-- 打卡类型选择 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/><polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
              </span>
              <span class="section-title">打卡信息</span>
            </div>
            <div class="clock-type-selector">
              <button class="clock-type-btn" :class="{ active: form.clockType === 'CLOCK_IN', 'clock-in': true }" @click="form.clockType = 'CLOCK_IN'">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><polyline points="17,11 12,6 7,11" stroke="currentColor" stroke-width="2"/><line x1="12" y1="6" x2="12" y2="18" stroke="currentColor" stroke-width="2"/></svg>
                <span>上班补卡</span>
              </button>
              <button class="clock-type-btn" :class="{ active: form.clockType === 'CLOCK_OUT', 'clock-out': true }" @click="form.clockType = 'CLOCK_OUT'">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><polyline points="7,13 12,18 17,13" stroke="currentColor" stroke-width="2"/><line x1="12" y1="6" x2="12" y2="18" stroke="currentColor" stroke-width="2"/></svg>
                <span>下班补卡</span>
              </button>
            </div>
          </div>

          <!-- 补卡时间 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><rect x="3" y="4" width="18" height="18" rx="2" stroke="currentColor" stroke-width="2"/><line x1="16" y1="2" x2="16" y2="6" stroke="currentColor" stroke-width="2"/><line x1="8" y1="2" x2="8" y2="6" stroke="currentColor" stroke-width="2"/><line x1="3" y1="10" x2="21" y2="10" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">补卡时间</span>
            </div>
            <div class="time-input-group">
              <input type="date" class="form-input date-input" v-model="form.repairDate" @change="calcDuration" />
              <select class="form-input time-select" v-model="form.repairHour">
                <option v-for="h in 24" :key="h-1" :value="String(h-1).padStart(2,'0')">{{ String(h-1).padStart(2,'0') }}:00</option>
              </select>
              <span class="time-colon">:</span>
              <select class="form-input time-select" v-model="form.repairMinute">
                <option value="00">00</option>
                <option value="30">30</option>
              </select>
            </div>
          </div>

          <!-- 关联审批单 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" stroke="currentColor" stroke-width="2"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">关联审批（选填）</span>
            </div>
            <div class="relation-selector">
              <select class="form-input" v-model="form.relatedType">
                <option value="">不关联</option>
                <option value="LEAVE">请假</option>
                <option value="BUSINESS_TRIP">出差</option>
                <option value="OVERTIME">加班</option>
              </select>
              <input v-if="form.relatedType" type="text" class="form-input" v-model="form.relatedNo" placeholder="请输入关联单号" />
            </div>
          </div>

          <!-- 补卡类型 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" stroke="currentColor" stroke-width="2"/><polyline points="14,2 14,8 20,8" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">补卡原因</span>
            </div>
            <div class="reason-type-selector">
              <button v-for="(label, value) in typeMap" :key="value" class="reason-type-btn" :class="{ active: form.repairType === value, [value.toLowerCase()]: true }" @click="form.repairType = value">
                {{ label }}
              </button>
            </div>
          </div>

          <!-- 补卡说明 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><line x1="17" y1="10" x2="3" y2="10" stroke="currentColor" stroke-width="2"/><line x1="21" y1="6" x2="3" y2="6" stroke="currentColor" stroke-width="2"/><line x1="21" y1="14" x2="3" y2="14" stroke="currentColor" stroke-width="2"/><line x1="17" y1="18" x2="3" y2="18" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">补卡说明</span>
            </div>
            <textarea class="form-textarea" v-model="form.reason" placeholder="请详细描述补卡原因，如：因公司网络故障导致无法正常打卡..." rows="3"></textarea>
            <div class="word-count">{{ form.reason.length }}/200</div>
          </div>

          <!-- 附件上传 -->
          <div class="form-section">
            <div class="section-header">
              <span class="section-icon">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M21.44 11.05l-9.19 9.19a6 6 0 0 1-8.49-8.49l9.19-9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1-2.83-2.83l8.49-8.48" stroke="currentColor" stroke-width="2"/></svg>
              </span>
              <span class="section-title">附件上传</span>
              <span class="section-hint">支持图片、PDF</span>
            </div>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="10"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :file-list="form.files"
              accept=".jpg,.jpeg,.png,.pdf"
              multiple
            >
              <el-button type="primary" plain size="small">
                <el-icon><Plus /></el-icon> 选择文件
              </el-button>
              <template #tip>
                <div class="upload-tip">支持 jpg、png、pdf 格式，最多10个文件</div>
              </template>
            </el-upload>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" :disabled="isSubmitting || uploading" @click="handleSubmit">
            <span v-if="isSubmitting || uploading" class="loading-spinner"></span>
            {{ (isSubmitting || uploading) ? '提交中...' : '提交申请' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">补卡详情</h3>
          <button class="dialog-close" @click="detailVisible = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" stroke-width="2"/><line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" stroke-width="2"/></svg>
          </button>
        </div>
        <div class="dialog-body">
          <div class="detail-card">
            <div class="detail-header">
              <span class="detail-no">{{ currentDetail.repairNo || '-' }}</span>
              <span class="status-badge" :class="getStatusClass(currentDetail.status)">
                <span class="status-dot"></span>
                {{ getStatusText(currentDetail.status) }}
              </span>
            </div>
          </div>

          <div class="detail-section">
            <div class="detail-row">
              <span class="detail-label">申请人</span>
              <span class="detail-value">{{ currentDetail.userName || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">申请部门</span>
              <span class="detail-value">{{ currentDetail.deptName || '-' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">申请日期</span>
              <span class="detail-value mono">{{ currentDetail.applyDate || '-' }}</span>
            </div>
          </div>

          <div class="detail-section">
            <div class="detail-row">
              <span class="detail-label">打卡类型</span>
              <span class="detail-value">
                <span class="clock-badge" :class="currentDetail.clockType === 'CLOCK_IN' ? 'in' : 'out'">
                  {{ currentDetail.clockType === 'CLOCK_IN' ? '上班打卡' : '下班打卡' }}
                </span>
              </span>
            </div>
            <div class="detail-row">
              <span class="detail-label">补卡类型</span>
              <span class="detail-value">
                <span class="type-badge" :class="getTypeClass(currentDetail.repairType)">
                  {{ typeMap[currentDetail.repairType] || '-' }}
                </span>
              </span>
            </div>
            <div class="detail-row highlight">
              <span class="detail-label">补卡时间</span>
              <span class="detail-value mono">{{ currentDetail.repairTime || '-' }}</span>
            </div>
          </div>

          <div class="detail-section" v-if="currentDetail.relatedNo">
            <div class="detail-row">
              <span class="detail-label">关联审批</span>
              <span class="detail-value">{{ currentDetail.relatedNo || '-' }}</span>
            </div>
          </div>

          <div class="detail-section" v-if="currentDetail.reason">
            <div class="detail-label">补卡说明</div>
            <div class="detail-reason">{{ currentDetail.reason }}</div>
          </div>

          <div class="detail-section" v-if="currentDetail.attachments">
            <div class="detail-label">附件</div>
            <div class="detail-files">
              <span class="file-badge">{{ currentDetail.attachments }}</span>
            </div>
          </div>

          <!-- 审批流程 -->
          <div class="approval-flow" v-if="currentDetail.approvalRecords && currentDetail.approvalRecords.length">
            <div class="flow-title">审批记录</div>
            <div v-for="(record, index) in currentDetail.approvalRecords" :key="index" class="flow-item">
              <div class="flow-dot" :class="record.approveType === 'APPROVE' ? 'approved' : 'rejected'"></div>
              <div class="flow-content">
                <div class="flow-header">
                  <span class="flow-user">{{ record.approverName || '系统' }}</span>
                  <span class="flow-time">{{ record.createTime }}</span>
                </div>
                <div class="flow-action">
                  <span class="flow-badge" :class="record.approveType === 'APPROVE' ? 'approved' : 'rejected'">
                    {{ record.approveType === 'APPROVE' ? '同意' : record.approveType === 'REJECT' ? '拒绝' : record.approveType }}
                  </span>
                  <span class="flow-comment" v-if="record.comment">{{ record.comment }}</span>
                </div>
              </div>
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
import { hrApi } from '@/api/hr'
import { workflowApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFiles } from '@/utils/upload'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('my')
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const stats = ref({ remainingCount: 5, usedCount: 0, approvedCount: 0 })
const currentDetail = ref({})
const uploading = ref(false)
const uploadRef = ref(null)

const typeMap = {
  MISSED: '缺卡',
  LATE: '迟到',
  EARLY_LEAVE: '早退',
  FORGET: '忘记打卡'
}

const form = reactive({
  clockType: 'CLOCK_IN',
  repairType: '',
  repairDate: '',
  repairHour: '09',
  repairMinute: '00',
  relatedType: '',
  relatedNo: '',
  reason: '',
  files: []
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

const getTypeClass = (type) => ({ MISSED: 'missed', LATE: 'late', EARLY_LEAVE: 'early', FORGET: 'forget' }[type] || '')
const getStatusClass = (status) => ({ PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' }[status] || '')
const getStatusText = (status) => ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status)

// HTML转义函数，防止XSS攻击
const escapeHtml = (str) => {
  if (!str) return '-'
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const calcDuration = () => {}

const handleFileChange = (file, fileList) => {
  form.files = fileList
}

const handleFileRemove = (file, fileList) => {
  form.files = fileList
}

const removeFile = (index) => {
  form.files.splice(index, 1)
}

const loadStatistics = async () => {
  try {
    const res = await hrApi.getRepairCardStatistics({})
    if (res.data) { stats.value = res.data }
  } catch (error) { console.error('加载统计失败', error) }
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my' ? await hrApi.getRepairCardList({}) : await workflowApi.getMyTasks()
    tableData.value = res.data?.data?.records || res.data?.data || []
  } catch (error) { console.error('加载数据失败', error) }
}

const handleAdd = () => {
  if ((stats.value.remainingCount || 0) <= 0) { ElMessage.warning('本月补卡次数已用完'); return }
  const now = new Date()
  const dateStr = now.toISOString().slice(0, 10)
  Object.assign(form, {
    clockType: 'CLOCK_IN',
    repairType: '',
    repairDate: dateStr,
    repairHour: '09',
    repairMinute: '00',
    relatedType: '',
    relatedNo: '',
    reason: '',
    files: []
  })
  dialogVisible.value = true
}

const handleView = async (row) => {
  try {
    const res = await hrApi.getRepairCardById(row.id)
    currentDetail.value = res.data || row
    detailVisible.value = true
  } catch (error) {
    currentDetail.value = row
    detailVisible.value = true
  }
}

const handleApprove = (row) => {
  currentDetail.value = row
  ElMessageBox.confirm(
    `<div style="text-align:left">
      <p><strong>打卡类型：</strong>${row.clockType === 'CLOCK_IN' ? '上班打卡' : '下班打卡'}</p>
      <p><strong>补卡类型：</strong>${escapeHtml(typeMap[row.repairType] || row.repairType)}</p>
      <p><strong>补卡时间：</strong>${escapeHtml(row.repairTime)}</p>
      <p><strong>补卡理由：</strong>${escapeHtml(row.reason)}</p>
    </div>`,
    '确认通过补卡申请？',
    { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning', dangerouslyUseHTMLString: true }
  ).then(async () => {
    await withDebounce(async () => {
      await hrApi.approveRepairCard(row.id, { approveResult: 'APPROVE', comment: '同意' })
      ElMessage.success('审批成功')
      loadData()
      loadStatistics()
    })
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await withDebounce(async () => {
    await hrApi.approveRepairCard(currentDetail.value.id, { approveResult: 'APPROVE', comment: '同意' })
    ElMessage.success('审批成功')
    detailVisible.value = false
    loadData()
    loadStatistics()
  })
}

const handleReject = async () => {
  const { value: comment } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
    confirmButtonText: '确认', cancelButtonText: '取消',
    inputPattern: /\S+/, inputErrorMessage: '请输入拒绝原因'
  })
  await withDebounce(async () => {
    await hrApi.approveRepairCard(currentDetail.value.id, { approveResult: 'REJECT', comment })
    ElMessage.success('已拒绝')
    detailVisible.value = false
    loadData()
    loadStatistics()
  })
}

const handleCancel = async (row) => {
  if (row.status !== 'PENDING') {
    ElMessage.warning('只能撤回待审批状态的申请')
    return
  }
  await ElMessageBox.confirm('确定要撤回该补卡申请吗？', '提示', { type: 'warning' })
  await withDebounce(async () => {
    await hrApi.cancelRepairCard(row.id)
    ElMessage.success('撤回成功')
    loadData()
    loadStatistics()
  })
}

const handleSubmit = async () => {
  if (!form.repairType) { ElMessage.warning('请选择补卡原因'); return }
  if (!form.repairDate) { ElMessage.warning('请选择补卡日期'); return }
  if (!form.reason.trim()) { ElMessage.warning('请输入补卡说明'); return }

  const repairTime = `${form.repairDate} ${form.repairHour}:${form.repairMinute}:00`

  await withDebounce(async () => {
    isSubmitting.value = true
    uploading.value = true
    try {
      // 上传文件
      const fileList = form.files.filter(f => f.raw).map(f => f.raw)
      let attachmentUrls = []
      if (fileList.length > 0) {
        attachmentUrls = await uploadFiles(fileList)
      }

      const payload = {
        clockType: form.clockType,
        repairType: form.repairType,
        repairTime,
        relatedType: form.relatedType || null,
        relatedNo: form.relatedNo || null,
        reason: form.reason,
        attachments: attachmentUrls.join(',')
      }
      await hrApi.createRepairCard(payload)
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadData()
      loadStatistics()
    } catch (error) {
      console.error('提交失败', error)
      ElMessage.error('提交失败：' + (error.message || '系统异常'))
    } finally {
      isSubmitting.value = false
      uploading.value = false
    }
  })
}

onMounted(() => { loadStatistics(); loadData() })
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

.repair-wrapper { min-height: 100vh; background: $bg-primary; }
.repair-container { padding: 30px; max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.page-title { font-size: 20px; font-weight: 600; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: #5a95f7; transform: translateY(-1px); } .btn-icon { font-size: 16px; } }

/* 统计卡片 */
.stats-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { display: flex; align-items: center; gap: 16px; padding: 20px; background: $bg-card; border-radius: 16px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.08); } }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; &.blue { background: rgba($primary, 0.12); color: $primary; } &.yellow { background: rgba($warning, 0.12); color: $warning; } &.green { background: rgba($success, 0.12); color: $success; } }
.stat-content { display: flex; flex-direction: column; gap: 2px; }
.stat-value { font-size: 28px; font-weight: 700; color: $text-primary; line-height: 1; &.used { color: $warning; } }
.stat-label { font-size: 13px; color: $text-secondary; }

.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }

.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .highlight { color: $primary; font-weight: 600; } .empty-cell { text-align: center; padding: 80px 16px; } } }
.empty-state { display: flex; flex-direction: column; align-items: center; gap: 12px; color: $text-secondary; svg { opacity: 0.4; } }

.type-badge { display: inline-block; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; &.missed { background: rgba($primary, 0.2); color: #5a95f7; } &.late { background: rgba($warning, 0.2); color: #d49b1f; } &.early { background: rgba($danger, 0.2); color: #e57373; } &.forget { background: rgba(167, 139, 250, 0.2); color: #8b6fe6; } }
.clock-badge { display: inline-block; padding: 4px 10px; font-size: 12px; font-weight: 500; border-radius: 6px; &.in { background: rgba($success, 0.15); color: #2eb385; } &.out { background: rgba($primary, 0.15); color: $primary; } }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: #d49b1f; .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: #2eb385; .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: #e57373; .status-dot { background: $danger; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: rgba($primary, 0.5); background: rgba($primary, 0.05); } &.cancel:hover { color: #d49b1f; border-color: rgba($warning, 0.5); } &.approve:hover { color: #2eb385; border-color: rgba($success, 0.5); } }

/* 弹窗 */
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(4px); }
.dialog { width: 100%; max-width: 520px; max-height: 90vh; background: $bg-card; border-radius: 20px; overflow: hidden; box-shadow: 0 24px 48px -12px rgba(0,0,0,0.2); display: flex; flex-direction: column; }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; flex-shrink: 0; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 600; color: $text-primary; }
.dialog-close { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; color: $text-secondary; background: transparent; border: none; border-radius: 10px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; overflow-y: auto; flex: 1; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; flex-shrink: 0; }

/* 表单分区 */
.form-section { margin-bottom: 24px; &:last-child { margin-bottom: 0; } }
.section-header { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.section-icon { color: $primary; }
.section-title { font-size: 14px; font-weight: 600; color: $text-primary; }
.section-hint { font-size: 12px; color: $text-secondary; margin-left: auto; }

/* 打卡类型选择 */
.clock-type-selector { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.clock-type-btn { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 20px; background: $bg-primary; border: 2px solid transparent; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); color: $text-secondary; &.clock-in { &.active { border-color: $success; background: rgba($success, 0.08); color: $success; } } &.clock-out { &.active { border-color: $primary; background: rgba($primary, 0.08); color: $primary; } } }

/* 时间选择 */
.time-input-group { display: flex; align-items: center; gap: 8px; }
.date-input { flex: 1; }
.time-select { width: 90px; }
.time-colon { font-size: 18px; font-weight: 600; color: $text-secondary; }

/* 关联选择 */
.relation-selector { display: flex; flex-direction: column; gap: 8px; }

/* 原因类型 */
.reason-type-selector { display: flex; flex-wrap: wrap; gap: 8px; }
.reason-type-btn { padding: 8px 16px; font-size: 13px; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 20px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: rgba($primary, 0.3); } &.active { color: #ffffff; border-color: transparent; &.missed { background: $primary; } &.late { background: $warning; } &.early { background: $danger; } &.forget { background: #A78BFA; } } }

/* 文本域 */
.form-textarea { width: 100%; padding: 12px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; outline: none; resize: vertical; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); font-family: inherit; &:focus { border-color: $primary; box-shadow: 0 0 0 3px rgba($primary, 0.15); } }
.word-count { text-align: right; font-size: 12px; color: $text-secondary; margin-top: 4px; }

/* 上传区域 */
.upload-area { border: 2px dashed rgba($primary, 0.4); border-radius: 12px; padding: 32px; text-align: center; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { border-color: $primary; background: rgba($primary, 0.04); } }
.upload-content { display: flex; flex-direction: column; align-items: center; gap: 8px; color: $text-secondary; svg { color: $primary; opacity: 0.6; } span { font-size: 14px; } .upload-hint { font-size: 12px; opacity: 0.7; } }
.upload-tip { font-size: 12px; color: $text-secondary; margin-top: 8px; }
.file-list { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.file-item { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: $bg-primary; border-radius: 10px; svg { color: $text-secondary; flex-shrink: 0; } .file-name { flex: 1; font-size: 13px; color: $text-primary; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .file-size { font-size: 12px; color: $text-secondary; } .file-remove { width: 28px; height: 28px; display: flex; align-items: center; justify-content: center; color: $text-secondary; background: transparent; border: none; border-radius: 6px; cursor: pointer; &:hover { color: $danger; background: rgba($danger, 0.1); } } }

/* 按钮 */
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); display: inline-flex; align-items: center; gap: 8px; &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: #5a95f7; } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: #f08080; } } }
.loading-spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; border-radius: 50%; animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* 详情 */
.detail-card { background: linear-gradient(135deg, rgba($primary, 0.08) 0%, rgba($primary, 0.02) 100%); border-radius: 16px; padding: 20px; margin-bottom: 20px; }
.detail-header { display: flex; justify-content: space-between; align-items: center; }
.detail-no { font-size: 18px; font-weight: 600; color: $text-primary; font-family: 'SF Mono', 'Monaco', monospace; }
.detail-section { background: $bg-primary; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
.detail-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; &:not(:last-child) { border-bottom: 1px solid rgba($border, 0.5); } &.highlight .detail-value { color: $primary; font-weight: 600; } }
.detail-label { font-size: 13px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; } }
.detail-reason { margin-top: 8px; font-size: 14px; color: $text-primary; line-height: 1.6; padding: 12px; background: $bg-card; border-radius: 8px; }
.detail-files { margin-top: 8px; }
.file-badge { display: inline-block; padding: 6px 12px; background: $bg-card; border-radius: 8px; font-size: 13px; color: $text-secondary; }

/* 审批流程 */
.approval-flow { margin-top: 20px; }
.flow-title { font-size: 14px; font-weight: 600; color: $text-primary; margin-bottom: 16px;   }
.flow-item { display: flex; gap: 12px; padding-bottom: 16px; position: relative; &:not(:last-child)::before { content: ''; position: absolute; left: 7px; top: 16px; bottom: 0; width: 2px; background: $border; } }
.flow-dot { width: 16px; height: 16px; border-radius: 50%; flex-shrink: 0; margin-top: 4px; &.approved { background: $success; } &.rejected { background: $danger; } }
.flow-content { flex: 1; }
.flow-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.flow-user { font-size: 14px; font-weight: 500; color: $text-primary; }
.flow-time { font-size: 12px; color: $text-secondary; }
.flow-action { display: flex; align-items: center; gap: 8px; }
.flow-badge { padding: 2px 8px; font-size: 12px; font-weight: 500; border-radius: 4px; &.approved { background: rgba($success, 0.15); color: #2eb385; } &.rejected { background: rgba($danger, 0.15); color: #e57373; } }
.flow-comment { font-size: 13px; color: $text-secondary; }

/* 表单输入 */
.form-input { width: 100%; padding: 10px 14px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 10px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 3px rgba($primary, 0.15); } &::placeholder { color: $text-secondary; } }
</style>
