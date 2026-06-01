<template>
  <div class="leave-wrapper">
    <div class="leave-container">
      <div class="page-header">
        <h2 class="page-title">请假申请</h2>
        <button class="btn-primary" @click="handleAdd" aria-label="新建请假申请">
          <span class="btn-icon" aria-hidden="true">+</span>
          <span>新增请假</span>
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

      <div class="table-wrapper" role="region" aria-label="请假申请列表">
        <table class="data-table" role="table">
          <thead>
            <tr role="row">
              <th role="columnheader">请假单号</th>
              <th role="columnheader">申请日期</th>
              <th role="columnheader">请假类型</th>
              <th role="columnheader">开始时间</th>
              <th role="columnheader">结束时间</th>
              <th role="columnheader">时长</th>
              <th role="columnheader">状态</th>
              <th role="columnheader">操作</th>
            </tr>
          </thead>
          <tbody role="rowgroup">
            <tr v-for="row in tableData" :key="row.id" role="row">
              <td class="mono" role="cell">{{ row.leaveNo }}</td>
              <td class="mono" role="cell">{{ row.applyDate }}</td>
              <td role="cell">
                <span class="type-badge" :class="getTypeClass(row.leaveType)">
                  {{ typeMap[row.leaveType] || row.leaveType }}
                </span>
              </td>
              <td class="mono" role="cell">{{ row.startTimeDesc }}</td>
              <td class="mono" role="cell">{{ row.endTimeDesc }}</td>
              <td class="mono highlight" role="cell">{{ row.duration }}{{ row.unit }}</td>
              <td role="cell">
                <span class="status-badge" :class="getStatusClass(row.status)" role="status" :aria-label="getStatusText(row.status)">
                  <span class="status-dot" aria-hidden="true"></span>
                  {{ getStatusText(row.status) }}
                </span>
              </td>
              <td class="action-cell" role="cell">
                <button class="action-btn view" @click="handleView(row)" :aria-label="`查看 ${row.leaveNo}`">查看</button>
                <button v-if="row.status === 'PENDING' && activeTab === 'my'" class="action-btn cancel" @click="handleCancel(row)" :aria-label="`撤回 ${row.leaveNo}`">撤回</button>
                <button v-if="activeTab === 'pending'" class="action-btn approve" @click="handleApprove(row)" :aria-label="`审批 ${row.leaveNo}`">审批</button>
              </td>
            </tr>
            <tr v-if="tableData.length === 0" role="row">
              <td colspan="8" class="empty-cell" role="cell"><span class="empty-text" role="status">暂无数据</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 请假表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" role="dialog" aria-modal="true" aria-labelledby="leave-dialog-title" @click.self="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title" id="leave-dialog-title">新增请假</h3>
          <button class="dialog-close" @click="dialogVisible = false" aria-label="关闭弹窗">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label">请假类型</label>
            <div class="type-selector">
              <button v-for="(item, key) in typeMap" :key="key" class="type-option" :class="{ active: form.leaveType === key, [key.toLowerCase()]: true }" @click="selectLeaveType(key)">{{ item.name }}</button>
            </div>
          </div>

          <!-- 按小时请假（事假、病假、哺乳假） -->
          <template v-if="getLeaveUnit() === 'HOUR'">
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">开始时间</label>
                <input type="datetime-local" class="form-input" v-model="form.startTime" @change="calcDuration" />
              </div>
              <div class="form-item">
                <label class="form-label">结束时间</label>
                <input type="datetime-local" class="form-input" v-model="form.endTime" @change="calcDuration" />
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">时长（小时）</label>
              <input type="number" class="form-input readonly" :value="form.duration" readonly step="0.1" min="0.1" />
            </div>
          </template>

          <!-- 按半天请假（调休、年假） -->
          <template v-else-if="getLeaveUnit() === 'HALF_DAY'">
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">开始时间</label>
                <div class="date-time-picker">
                  <input type="date" class="form-input" v-model="form.startDate" @change="calcDuration" />
                  <select class="form-select" v-model="form.startHalf" @change="calcDuration">
                    <option value="AM">上午</option>
                    <option value="PM">下午</option>
                  </select>
                </div>
              </div>
              <div class="form-item">
                <label class="form-label">结束时间</label>
                <div class="date-time-picker">
                  <input type="date" class="form-input" v-model="form.endDate" @change="calcDuration" />
                  <select class="form-select" v-model="form.endHalf" @change="calcDuration">
                    <option value="AM">上午</option>
                    <option value="PM">下午</option>
                  </select>
                </div>
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">时长（天）</label>
              <input type="number" class="form-input readonly" :value="form.duration" readonly step="0.5" min="0.5" />
            </div>
          </template>

          <!-- 按天请假（产假、陪产假、婚假、丧假） -->
          <template v-else>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">开始时间</label>
                <input type="date" class="form-input" v-model="form.startDate" @change="calcDuration" />
              </div>
              <div class="form-item">
                <label class="form-label">结束时间</label>
                <input type="date" class="form-input" v-model="form.endDate" @change="calcDuration" />
              </div>
            </div>
            <div class="form-item">
              <label class="form-label">时长（天）</label>
              <input type="number" class="form-input readonly" :value="form.duration" readonly min="1" />
            </div>
          </template>

          <div class="form-item">
            <label class="form-label">请假事由</label>
            <textarea class="form-textarea" v-model="form.reason" placeholder="请输入请假事由" rows="3"></textarea>
          </div>
          <div class="upload-section">
            <label class="form-label">附件上传</label>
            <el-upload
              ref="uploadRef"
              :auto-upload="false"
              :limit="10"
              :on-change="handleFileChange"
              :on-remove="handleFileRemove"
              :file-list="form.images"
              accept=".jpg,.jpeg,.png,.pdf,.doc,.docx"
              multiple
            >
              <el-button type="primary" plain>
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
          <button class="cyber-btn primary" @click="handleSubmit">提交</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">请假详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">请假单号</span>
              <span class="detail-value mono">{{ currentDetail.leaveNo || '-' }}</span>
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
              <span class="detail-label">请假类型</span>
              <span class="detail-value">
                <span class="type-badge" :class="getTypeClass(currentDetail.leaveType)">
                  {{ typeMap[currentDetail.leaveType]?.name || '-' }}
                </span>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">时长</span>
              <span class="detail-value highlight">{{ currentDetail.duration }} {{ currentDetail.unit }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">开始时间</span>
              <span class="detail-value mono">{{ currentDetail.startTimeDesc || '-' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">结束时间</span>
              <span class="detail-value mono">{{ currentDetail.endTimeDesc || '-' }}</span>
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
              <span class="detail-label">请假事由</span>
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
import { reactive, ref, watch, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFiles } from '@/utils/upload'
import { Plus, Delete } from '@element-plus/icons-vue'

const activeTab = ref('my')
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const currentDetail = ref({})
const uploading = ref(false)
const uploadRef = ref(null)

// 请假类型配置：name-显示名称，unit-计量单位(HOUR/HALF_DAY/DAY)
const typeMap = {
  PERSONAL: { name: '事假', unit: 'HOUR' },
  SICK: { name: '病假', unit: 'HOUR' },
  NURSING: { name: '哺乳假', unit: 'HOUR' },
  ADJUSTMENT: { name: '调休', unit: 'HALF_DAY' },
  ANNUAL: { name: '年假', unit: 'HALF_DAY' },
  MATERNITY: { name: '产假', unit: 'DAY' },
  PATERNITY: { name: '陪产假', unit: 'DAY' },
  MARRIAGE: { name: '婚假', unit: 'DAY' },
  FUNERAL: { name: '丧假', unit: 'DAY' }
}

const form = reactive({
  leaveType: '',
  // 按小时
  startTime: '',
  endTime: '',
  duration: 0,
  // 按半天/按天
  startDate: '',
  startHalf: 'AM',
  endDate: '',
  endHalf: 'PM',
  reason: '',
  images: []
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

const getTypeClass = (type) => {
  const classMap = {
    PERSONAL: 'personal', SICK: 'sick', NURSING: 'nursing',
    ADJUSTMENT: 'adjustment', ANNUAL: 'annual',
    MATERNITY: 'maternity', PATERNITY: 'paternity', MARRIAGE: 'marriage', FUNERAL: 'funeral'
  }
  return classMap[type] || ''
}

const getStatusClass = (status) => ({ PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' }[status] || '')
const getStatusText = (status) => ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status)

const getLeaveUnit = () => {
  if (!form.leaveType) return 'HALF_DAY'
  return typeMap[form.leaveType]?.unit || 'HALF_DAY'
}

const selectLeaveType = (type) => {
  form.leaveType = type
  // 重置时间字段
  form.startTime = ''
  form.endTime = ''
  form.startDate = ''
  form.startHalf = 'AM'
  form.endDate = ''
  form.endHalf = 'PM'
  form.duration = 0
}

const calcDuration = () => {
  const unit = getLeaveUnit()

  if (unit === 'HOUR') {
    // 按小时计算
    if (form.startTime && form.endTime) {
      const start = new Date(form.startTime)
      const end = new Date(form.endTime)
      const diff = (end - start) / (1000 * 60 * 60)
      form.duration = Math.max(0.1, Math.round(diff * 10) / 10)
    }
  } else if (unit === 'HALF_DAY') {
    // 按半天计算
    if (form.startDate && form.endDate) {
      const start = new Date(form.startDate)
      const end = new Date(form.endDate)
      const diffDays = Math.floor((end - start) / (1000 * 60 * 60 * 24))
      const startHalf = form.startHalf === 'AM' ? 0 : 0.5
      const endHalf = form.endHalf === 'PM' ? 0 : 0.5
      form.duration = Math.max(0.5, Math.round((diffDays + startHalf + (1 - endHalf)) * 2) / 2)
    }
  } else {
    // 按天计算
    if (form.startDate && form.endDate) {
      const start = new Date(form.startDate)
      const end = new Date(form.endDate)
      const diffDays = Math.floor((end - start) / (1000 * 60 * 60 * 24)) + 1
      form.duration = Math.max(1, diffDays)
    }
  }
}

const handleFileChange = (file, fileList) => {
  form.images = fileList
}

const handleFileRemove = (file, fileList) => {
  form.images = fileList
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my' ? await workflowApi.getLeaveList({}) : await workflowApi.getMyTasks()
    tableData.value = res.data?.data?.records || res.data?.data || []
  } catch (error) { console.error('加载数据失败', error) }
}

const handleAdd = () => {
  const today = new Date().toISOString().split('T')[0]
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  Object.assign(form, {
    leaveType: '',
    startTime: now.toISOString().slice(0, 16),
    endTime: '',
    startDate: today,
    startHalf: 'AM',
    endDate: today,
    endHalf: 'PM',
    duration: 0,
    reason: '',
    images: []
  })
  dialogVisible.value = true
}

const handleView = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const escapeHtml = (str) => {
  if (!str) return '-'
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

const handleApprove = (row) => {
  currentDetail.value = row
  const typeName = typeMap[row.leaveType]?.name || row.leaveType
  const message = [
    `请假类型：${typeName}`,
    `开始时间：${row.startTimeDesc || '-'}`,
    `结束时间：${row.endTimeDesc || '-'}`,
    `请假时长：${row.duration} ${row.unit}`,
    `请假事由：${row.reason || '-'}`
  ].join('\n')
  ElMessageBox.confirm(message, '确认通过请假申请？', {
    confirmButtonText: '通过',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await withDebounce(async () => {
      await workflowApi.approveLeave(row.id, { approveResult: 'APPROVE', comment: '同意' })
      ElMessage.success('审批成功')
      loadData()
    })
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await withDebounce(async () => {
    await workflowApi.approveLeave(currentDetail.value.id, { approveResult: 'APPROVE', comment: '同意' })
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
    await workflowApi.approveLeave(currentDetail.value.id, { approveResult: 'REJECT', comment })
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
  await ElMessageBox.confirm('确定要撤回该请假申请吗？', '提示', { type: 'warning' })
  await withDebounce(async () => {
    await workflowApi.cancelLeave(row.id)
    ElMessage.success('撤回成功')
    loadData()
  })
}

const handleSubmit = () => {
  if (!form.leaveType) { ElMessage.warning('请选择请假类型'); return }

  const unit = getLeaveUnit()
  if (unit === 'HOUR') {
    if (!form.startTime) { ElMessage.warning('请选择开始时间'); return }
    if (!form.endTime) { ElMessage.warning('请选择结束时间'); return }
  } else {
    if (!form.startDate) { ElMessage.warning('请选择开始日期'); return }
    if (!form.endDate) { ElMessage.warning('请选择结束日期'); return }
  }

  if (!form.reason.trim()) { ElMessage.warning('请输入请假事由'); return }

  withDebounce(async () => {
    uploading.value = true
    try {
      const fileList = form.images.filter(f => f.raw).map(f => f.raw)
      let attachmentUrls = []
      if (fileList.length > 0) {
        attachmentUrls = await uploadFiles(fileList)
      }

      const data = {
        ...form,
        attachments: attachmentUrls.join(',')
      }

      await workflowApi.createLeave(data)
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('提交失败')
    } finally {
      uploading.value = false
    }
  })
}

watch(activeTab, () => loadData())

onMounted(() => loadData())
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2; $bg-card: #ffffff; $primary: #60A5FA; $success: #34D399; $warning: #FBBF24; $danger: #FCA5A5; $text-primary: #3B3B3B; $text-secondary: #9CA3AF; $border: #F0EDE9;

.leave-wrapper { min-height: 100vh; background: $bg-primary; }
.leave-container { padding: 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.page-title { font-size: 18px; font-weight: 500; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } .btn-icon { font-size: 16px; } }
.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }
.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .highlight { color: $primary; font-weight: 600; } .empty-cell { text-align: center; padding: 60px 16px; .empty-text { color: $text-secondary; font-size: 14px; } } } }
.type-badge { display: inline-block; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; &.personal { background: rgba($primary, 0.2); color: darken($primary, 8%); } &.sick { background: rgba($danger, 0.2); color: darken($danger, 8%); } &.nursing { background: rgba(167, 139, 250, 0.2); color: #8b6fe6; } &.adjustment { background: rgba($warning, 0.2); color: darken($warning, 20%); } &.annual { background: rgba($success, 0.2); color: darken($success, 12%); } &.maternity { background: rgba(251, 146, 60, 0.2); color: #ea7c3b; } &.paternity { background: rgba(45, 212, 191, 0.2); color: #20b2a5; } &.marriage { background: rgba(236, 72, 153, 0.2); color: #db2777; } &.funeral { background: rgba(107, 114, 128, 0.2); color: #6b7280; } }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: darken($warning, 20%); .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: darken($success, 12%); .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: darken($danger, 8%); .status-dot { background: $danger; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: $primary; background: rgba($primary, 0.05); } &.cancel:hover { color: darken($warning, 20%); border-color: $warning; } &.approve:hover { color: darken($success, 12%); border-color: $success; } }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.dialog { width: 100%; max-width: 560px; background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 500; color: $text-primary; }
.dialog-close { width: 32px; height: 32px; font-size: 20px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; max-height: 70vh; overflow-y: auto; }
.form-item { margin-bottom: 20px; }
.form-label { display: block; margin-bottom: 8px; font-size: 14px; font-weight: 500; color: $text-primary; }
.form-row { display: flex; gap: 16px; .form-item { flex: 1; } }
.type-selector { display: flex; gap: 8px; flex-wrap: wrap; }
.type-option { padding: 8px 16px; font-size: 13px; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 20px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: rgba($primary, 0.3); } &.active { color: #ffffff; border-color: transparent; &.personal { background: $primary; } &.sick { background: $danger; } &.nursing { background: #A78BFA; } &.adjustment { background: $warning; } &.annual { background: $success; } &.maternity { background: #FB923C; } &.paternity { background: #2DD4BF; } &.marriage { background: #EC4899; } &.funeral { background: #6B7280; } } }
.date-time-picker { display: flex; gap: 8px; .form-input { flex: 1; } .form-select { width: 90px; padding: 10px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; cursor: pointer; &:focus { border-color: $primary; } } }
.form-input { width: 100%; padding: 10px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } &.readonly { background: darken($bg-primary, 3%); color: $primary; font-weight: 600; } }
.form-textarea { width: 100%; padding: 10px 16px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; resize: vertical; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; font-family: inherit; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.upload-area { display: flex; flex-direction: column; gap: 10px; }
.upload-btn { display: flex; align-items: center; gap: 8px; padding: 16px; background: $bg-primary; border: 1px dashed $primary; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { background: rgba($primary, 0.05); } .upload-icon { font-size: 20px; color: $primary; } span { font-size: 14px; color: $primary; } }
.image-preview { display: flex; flex-wrap: wrap; gap: 8px; }
.preview-item { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: $bg-primary; border-radius: 6px; .preview-name { font-size: 13px; color: $text-primary; max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; } .preview-remove { font-size: 16px; color: $danger; cursor: pointer; &:hover { color: darken($danger, 8%); } } }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; }
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: darken($danger, 8%); } } }

.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; &.full-width { grid-column: 1 / -1; } }
.detail-label { font-size: 12px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } &.highlight { color: $primary; font-weight: 600; } &.reason { line-height: 1.6; color: $text-secondary; } }

.upload-section { margin-bottom: 16px; }
.upload-tip { margin-top: 8px; font-size: 12px; color: #999; }

// 响应式设计
@media (max-width: 768px) {
  .leave-container { padding: 16px; }
  .page-header { flex-direction: column; gap: 16px; align-items: flex-start; }
  .page-title { font-size: 16px; }
  .btn-primary { width: 100%; justify-content: center; padding: 12px 20px; }
  .tabs { flex-direction: column; }
  .table-wrapper { overflow-x: auto; }
  .data-table { min-width: 600px; }
  .action-cell { flex-direction: column; gap: 6px; }
  .action-btn { width: 100%; padding: 10px 14px; }
  .dialog { max-width: 100%; margin: 16px; border-radius: 12px; }
  .dialog-body { padding: 16px; }
  .form-row { flex-direction: column; }
  .type-selector { gap: 6px; }
  .type-option { padding: 8px 12px; font-size: 12px; }
  .dialog-footer { flex-direction: column; gap: 8px; padding: 16px; }
  .cyber-btn { width: 100%; text-align: center; }
}

@media (max-width: 480px) {
  .leave-container { padding: 12px; }
  .detail-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 15px; }
  .status-badge { padding: 3px 8px; font-size: 11px; }
}
</style>