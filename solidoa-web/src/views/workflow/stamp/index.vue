<template>
  <div class="stamp-wrapper">
    <div class="stamp-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">用印申请</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新增用印</span>
        </button>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button
          class="tab-item"
          :class="{ active: activeTab === 'my' }"
          @click="activeTab = 'my'"
        >
          <span class="tab-indicator"></span>
          我提交的
        </button>
        <button
          class="tab-item"
          :class="{ active: activeTab === 'pending' }"
          @click="activeTab = 'pending'"
        >
          <span class="tab-indicator"></span>
          待我审批
        </button>
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
          <template #stampType="{ row }">
            <span class="type-badge" :class="getStampTypeClass(row.stampType)">
              {{ row.stampTypeDesc || row.stampType }}
            </span>
          </template>
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
            <OaButton
              v-if="row.status === 'APPROVED'"
              variant="primary"
              size="small"
              @click="handleRecord(row)"
            >
              登记
            </OaButton>
          </template>
        </OaTable>
      </div>
    </div>

    <!-- 用印申请表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">{{ dialogTitle }}</h3>
          <button class="dialog-close" @click="dialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-card">
            <div class="form-item">
              <label class="form-label">用印部门</label>
              <el-select
                v-model="form.deptId"
                placeholder="请选择用印部门"
                class="form-select"
                clearable
              >
                <el-option
                  v-for="dept in departments"
                  :key="dept.id"
                  :label="dept.name"
                  :value="dept.id"
                />
              </el-select>
            </div>

            <div class="form-item">
              <label class="form-label">用印日期</label>
              <el-date-picker
                v-model="form.stampDate"
                type="date"
                placeholder="选择日期"
                class="form-date-picker"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
              />
            </div>

            <div class="form-item">
              <label class="form-label">用印文件名称</label>
              <el-input
                v-model="form.documentName"
                placeholder="请输入需要盖章的文件名称"
              />
            </div>

            <div class="form-row">
              <div class="form-item">
                <label class="form-label">文件份数</label>
                <el-input-number
                  v-model="form.documentCount"
                  :min="1"
                  :max="99"
                  class="form-number"
                />
              </div>
              <div class="form-item">
                <label class="form-label">文件类别</label>
                <el-select
                  v-model="form.documentType"
                  placeholder="请选择"
                  class="form-select"
                  clearable
                >
                  <el-option
                    v-for="type in documentTypes"
                    :key="type.value"
                    :label="type.label"
                    :value="type.value"
                  />
                </el-select>
              </div>
            </div>

            <div class="form-item">
              <label class="form-label">加盖何种印章</label>
              <div class="type-selector">
                <button
                  v-for="(item, index) in stampTypes"
                  :key="item.value"
                  class="type-option"
                  :class="{ active: form.stampType === item.value, ['type-' + index]: true }"
                  @click="form.stampType = item.value"
                >
                  {{ item.label }}
                </button>
              </div>
            </div>

            <div class="form-item">
              <label class="form-label">备注</label>
              <el-input
                v-model="form.remark"
                type="textarea"
                placeholder="请输入备注说明"
                :rows="3"
              />
            </div>

            <div class="form-item">
              <label class="form-label">用印文件（附件）</label>
              <el-upload
                v-model:file-list="form.attachments"
                action="#"
                :auto-upload="false"
                :limit="10"
                multiple
                accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                class="form-upload"
              >
                <el-button type="primary" plain>
                  <el-icon class="el-icon--left"><UploadFilled /></el-icon>
                  选择文件
                </el-button>
                <template #tip>
                  <div class="upload-tip">支持 PDF、Word、图片格式，最多上传 10 个文件</div>
                </template>
              </el-upload>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" :disabled="uploading" @click="handleSubmit">
            {{ uploading ? '提交中...' : '提交' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 审批弹窗 -->
    <div class="dialog-overlay" v-if="approveDialogVisible" @click.self="approveDialogVisible = false">
      <div class="dialog dialog-sm">
        <div class="dialog-header">
          <h3 class="dialog-title">审批用印申请</h3>
          <button class="dialog-close" @click="approveDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label">审批意见</label>
            <textarea class="form-textarea" v-model="approveForm.comment" placeholder="请输入审批意见" rows="3"></textarea>
          </div>
        </div>
        <div class="dialog-footer approve-actions">
          <button class="cyber-btn reject" @click="handleReject">拒绝</button>
          <button class="cyber-btn approve" @click="handleApproveConfirm">同意</button>
        </div>
      </div>
    </div>

    <!-- 用印登记弹窗 -->
    <div class="dialog-overlay" v-if="recordDialogVisible" @click.self="recordDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">物理用印登记</h3>
          <button class="dialog-close" @click="recordDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label">用印时间</label>
            <input type="datetime-local" class="form-input" v-model="recordForm.stampTime" />
          </div>
          <div class="form-item">
            <label class="form-label">领取人</label>
            <input type="text" class="form-input" v-model="recordForm.receivedBy" placeholder="请输入领取人姓名" />
          </div>
          <div class="form-item">
            <label class="form-label">领取人电话</label>
            <input type="tel" class="form-input" v-model="recordForm.receivedMobile" placeholder="请输入领取人电话" />
          </div>
          <div class="form-item">
            <label class="form-label">实际用印份数</label>
            <div class="number-input">
              <button class="num-btn" @click="recordForm.actualCount = Math.max(1, recordForm.actualCount - 1)">-</button>
              <span class="num-value">{{ recordForm.actualCount }}</span>
              <button class="num-btn" @click="recordForm.actualCount++">+</button>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="recordDialogVisible = false">取消</button>
          <button class="cyber-btn primary" @click="handleRecordSubmit">确认登记</button>
        </div>
      </div>
    </div>

    <!-- 用印详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">用印详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body" v-if="currentDetail">
          <div class="detail-item"><span class="label">文件名称</span><span class="value">{{ currentDetail.documentName }}</span></div>
          <div class="detail-item"><span class="label">文件类型</span><span class="value">{{ currentDetail.documentType }}</span></div>
          <div class="detail-item"><span class="label">用印类型</span><span class="value">{{ currentDetail.stampType }}</span></div>
          <div class="detail-item"><span class="label">用印份数</span><span class="value">{{ currentDetail.documentCount }}</span></div>
          <div class="detail-item"><span class="label">申请部门</span><span class="value">{{ currentDetail.deptName }}</span></div>
          <div class="detail-item"><span class="label">申请日期</span><span class="value">{{ currentDetail.stampDate }}</span></div>
          <div class="detail-item"><span class="label">备注</span><span class="value">{{ currentDetail.remark || '无' }}</span></div>
          <div class="detail-item"><span class="label">状态</span><span class="value" :class="'status-' + (currentDetail.status || '').toLowerCase()">{{ currentDetail.status }}</span></div>

          <!-- 审批流程图（V2.0 接入 State Machine） -->
          <OaApprovalCard
            v-if="currentDetail && currentDetail.id"
            title="审批流程"
            business-type="STAMP"
            :business-id="currentDetail.id"
            class="detail-flow-card"
          />
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="detailVisible = false">关闭</button>
          <button class="cyber-btn approve" v-if="currentDetail?.status === 'PENDING'" @click="handleApprove(currentDetail); detailVisible = false">审批</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { systemApi } from '@/api/system'
import { uploadFiles } from '@/utils/upload'
import { ElMessage, ElMessageBox, genFileId } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

const activeTab = ref('my')
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const dialogVisible = ref(false)
const uploading = ref(false)
const approveDialogVisible = ref(false)
const recordDialogVisible = ref(false)

// 表格列定义
const columns = [
  { prop: 'stampNo', label: '用印单号', width: 160 },
  { prop: 'stampType', label: '用印类型', width: 110 },
  { prop: 'documentName', label: '文件名称', minWidth: 180 },
  { prop: 'documentCount', label: '份数', width: 80, formatter: (val) => `${val || 0}份` },
  { prop: 'usage', label: '用途', minWidth: 180 },
  { prop: 'status', label: '状态', width: 110 },
  { prop: 'createTime', label: '申请时间', width: 160, formatter: (val) => formatTime(val) }
]

// 状态 -> OaStatusBadge type
const getBadgeType = (status) => ({
  PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info', COMPLETED: 'primary'
}[status] || 'default')
const detailVisible = ref(false)
const dialogTitle = ref('新增用印')
const currentRow = ref(null)
const currentDetail = ref(null)
const departments = ref([])

const form = reactive({
  deptId: '',
  stampDate: '',
  documentName: '',
  documentCount: 1,
  documentType: '',
  stampType: '',
  remark: '',
  attachments: []
})

const documentTypes = [
  { value: 'CONTRACT', label: '合同/协议' },
  { value: 'CERTIFICATE', label: '证明/证书' },
  { value: 'LETTER', label: '函件/介绍信' },
  { value: 'REPORT', label: '报表/报告' },
  { value: 'OTHER', label: '其他' }
]

const stampTypes = [
  { value: 'PUBLIC', label: '公章' },
  { value: 'CONTRACT', label: '合同章' },
  { value: 'LEGAL', label: '法人章' },
  { value: 'DEPT', label: '部门章' }
]

const loadDepartments = async () => {
  try {
    const res = await systemApi.getDeptTree()
    departments.value = res.data?.data || res.data || []
  } catch (error) {
    console.error('加载部门失败', error)
  }
}

const approveForm = reactive({
  approveType: 'APPROVE',
  comment: ''
})

const recordForm = reactive({
  stampTime: '',
  receivedBy: '',
  receivedMobile: '',
  actualCount: 1
})

const formatTime = (time) => {
  if (!time) return '-'
  const d = new Date(time)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const getStampTypeClass = (type) => {
  const map = { PUBLIC: 'public', CONTRACT: 'contract', LEGAL: 'legal', DEPT: 'dept' }
  return map[type] || 'default'
}

const getStatusClass = (status) => {
  const map = {
    PENDING: 'pending',
    APPROVED: 'approved',
    REJECTED: 'rejected',
    COMPLETED: 'completed',
    CANCELLED: 'cancelled'
  }
  return map[status] || 'pending'
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my'
      ? await workflowApi.getStampList(query)
      : await workflowApi.getMyTasks(query)
    const data = res.data?.data
    tableData.value = data?.records || data || []
    total.value = data?.total || tableData.value.length
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增用印'
  Object.assign(form, {
    deptId: '',
    stampDate: '',
    documentName: '',
    documentCount: 1,
    documentType: '',
    stampType: '',
    remark: '',
    attachments: []
  })
  dialogVisible.value = true
}

const handleView = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定要撤回该用印申请吗？', '提示', { type: 'warning' })
  await workflowApi.cancelStamp(row.id)
  ElMessage.success('撤回成功')
  loadData()
}

const handleApprove = (row) => {
  currentRow.value = row
  approveForm.comment = ''
  approveDialogVisible.value = true
}

const handleApproveConfirm = async () => {
  try {
    await workflowApi.approveStamp(currentRow.value.id, {
      approveType: 'APPROVE',
      comment: approveForm.comment || '同意'
    })
    ElMessage.success('审批通过')
    approveDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('审批失败')
  }
}

const handleReject = async () => {
  try {
    await workflowApi.approveStamp(currentRow.value.id, {
      approveType: 'REJECT',
      comment: approveForm.comment || '拒绝'
    })
    ElMessage.success('已拒绝')
    approveDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

const handleRecord = (row) => {
  currentRow.value = row
  Object.assign(recordForm, { stampTime: '', receivedBy: '', receivedMobile: '', actualCount: row.documentCount || 1 })
  recordDialogVisible.value = true
}

const handleRecordSubmit = async () => {
  try {
    await workflowApi.recordStamp(currentRow.value.id, recordForm)
    ElMessage.success('登记成功')
    recordDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('登记失败')
  }
}

const handleSubmit = async () => {
  // 验证表单
  if (!form.deptId || !form.stampDate || !form.documentName || !form.stampType) {
    ElMessage.warning('请填写完整信息')
    return
  }

  uploading.value = true
  try {
    // 上传附件
    const fileList = form.attachments.filter(f => f.raw).map(f => f.raw)
    let attachmentUrls = []
    if (fileList.length > 0) {
      attachmentUrls = await uploadFiles(fileList)
    }

    // 提交表单（附件URL用逗号分隔）
    const data = {
      ...form,
      attachments: attachmentUrls.join(',')
    }

    await workflowApi.createStamp(data)
    ElMessage.success('提交成功')
    dialogVisible.value = false
    loadData()
    resetForm()
  } catch (error) {
    ElMessage.error('提交失败')
  } finally {
    uploading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    deptId: '',
    stampDate: '',
    documentName: '',
    documentCount: 1,
    documentType: '',
    stampType: '',
    remark: '',
    attachments: []
  })
}

onMounted(() => {
  loadData()
  loadDepartments()
})
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$danger: #FCA5A5;
$purple: #A78BFA;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border: #F0EDE9;

.stamp-wrapper {
  min-height: 100vh;
  background: $bg-primary;
}

.stamp-container {
  padding: 30px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;
  margin: 0;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  color: #ffffff;
  background: $primary;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 4px 12px rgba($primary, 0.25);

  &:hover {
    background: darken($primary, 8%);
    box-shadow: 0 6px 16px rgba($primary, 0.3);
  }

  .btn-icon {
    font-size: 16px;
  }
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  background: $bg-card;
  padding: 6px;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  font-size: 14px;
  color: $text-secondary;
  background: transparent;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  .tab-indicator {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: transparent;
  }

  &:hover {
    color: $text-primary;
  }

  &.active {
    color: $primary;
    background: rgba(96, 165, 250, 0.08);

    .tab-indicator {
      background: $primary;
    }
  }
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
        background: rgba(96, 165, 250, 0.03);
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

    .name-cell {
      max-width: 180px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .reason-cell {
      max-width: 150px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      color: $text-secondary;
    }

    .time-cell {
      color: $text-secondary;
      font-size: 13px;
    }

    .empty-cell {
      text-align: center;
      padding: 60px 16px;

      .empty-text {
        color: $text-secondary;
        font-size: 14px;
      }
    }
  }
}

.type-badge {
  display: inline-block;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;

  &.public {
    background: rgba($primary, 0.25);
    color: darken($primary, 8%);
  }

  &.contract {
    background: rgba(167, 139, 250, 0.25);
    color: #8b6fe6;
  }

  &.legal {
    background: rgba($success, 0.25);
    color: darken($success, 12%);
  }

  &.dept {
    background: rgba($warning, 0.25);
    color: darken($warning, 20%);
  }
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 20px;

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
  }

  &.pending {
    background: rgba($warning, 0.15);
    color: darken($warning, 20%);

    .status-dot {
      background: $warning;
    }
  }

  &.approved {
    background: rgba($success, 0.15);
    color: darken($success, 12%);

    .status-dot {
      background: $success;
    }
  }

  &.rejected {
    background: rgba($danger, 0.2);
    color: darken($danger, 8%);

    .status-dot {
      background: $danger;
    }
  }

  &.completed {
    background: rgba($primary, 0.15);
    color: darken($primary, 8%);

    .status-dot {
      background: $primary;
    }
  }

  &.cancelled {
    background: rgba($text-secondary, 0.15);
    color: $text-secondary;

    .status-dot {
      background: #9CA3AF;
    }
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
    background: rgba(96, 165, 250, 0.05);
  }

  &.cancel:hover {
    color: darken($warning, 20%);
    border-color: $warning;
  }

  &.approve:hover {
    color: darken($success, 12%);
    border-color: $success;
  }

  &.record:hover {
    color: #8b6fe6;
    border-color: $purple;
  }
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

  &.dialog-sm {
    max-width: 400px;
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
}

.form-item {
  margin-bottom: 20px;
}

.form-card {
  background: $bg-primary;
  border-radius: 16px;
  padding: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

.form-row {
  display: flex;
  gap: 16px;

  .form-item {
    flex: 1;
  }
}

.form-select {
  width: 100%;
}

.form-date-picker {
  width: 100%;
}

.form-number {
  width: 100%;
}

.form-upload {
  :deep(.el-upload-list) {
    margin-top: 12px;
  }
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: $text-secondary;
}

.type-selector {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.type-option {
  padding: 10px 18px;
  font-size: 14px;
  color: $text-secondary;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    color: $primary;
    border-color: rgba(96, 165, 250, 0.3);
  }

  &.active {
    color: #ffffff;
    border-color: transparent;

    &.type-0 {
      background: #60A5FA;
    }

    &.type-1 {
      background: #A78BFA;
    }

    &.type-2 {
      background: #34D399;
    }

    &.type-3 {
      background: #FBBF24;
    }
  }
}

.number-input {
  display: flex;
  align-items: center;
  gap: 16px;

  .num-btn {
    width: 36px;
    height: 36px;
    font-size: 18px;
    color: $text-primary;
    background: $bg-primary;
    border: 1px solid $border;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      background: rgba(96, 165, 250, 0.1);
      border-color: $primary;
      color: $primary;
    }
  }

  .num-value {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-size: 20px;
    color: $primary;
    min-width: 50px;
    text-align: center;
  }
}

.form-input {
  width: 100%;
  padding: 10px 16px;
  font-size: 14px;
  color: $text-primary;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 8px;
  outline: none;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;

  &:focus {
    border-color: $primary;
    box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
  }

  &::placeholder {
    color: $text-secondary;
  }

  &::-webkit-calendar-picker-indicator {
    opacity: 0.6;
  }
}

.form-textarea {
  width: 100%;
  padding: 10px 16px;
  font-size: 14px;
  color: $text-primary;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 8px;
  outline: none;
  resize: vertical;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  box-sizing: border-box;
  font-family: inherit;

  &:focus {
    border-color: $primary;
    box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
  }

  &::placeholder {
    color: $text-secondary;
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid $border;

  &.approve-actions {
    justify-content: space-between;
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

  &.approve {
    color: #ffffff;
    background: $success;
    border-color: $success;
    box-shadow: 0 4px 12px rgba($success, 0.25);

    &:hover {
      background: darken($success, 8%);
    }
  }

  &.reject {
    color: #ffffff;
    background: $danger;
    border-color: $danger;
    box-shadow: 0 4px 12px rgba(252, 165, 165, 0.25);

    &:hover {
      background: darken($danger, 8%);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .stamp-container { padding: 16px; }
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
  .stamp-container { padding: 12px; }
  .page-title { font-size: 15px; }
}
</style>
