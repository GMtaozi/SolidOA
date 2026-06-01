<template>
  <div class="salary-wrapper">
    <div class="salary-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">工资发放审批</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新建工资单</span>
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

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <div class="filter-item">
          <label>薪资期间</label>
          <input type="month" v-model="filters.salaryMonth" class="filter-input" />
        </div>
        <button class="btn-filter" @click="loadData">查询</button>
        <button class="btn-reset" @click="resetFilters">重置</button>
      </div>

      <!-- 数据表格 -->
      <div class="table-wrapper">
        <table class="data-table">
          <thead>
            <tr>
              <th>工资单号</th>
              <th>申请日期</th>
              <th>薪资期间</th>
              <th>发薪日期</th>
              <th>发薪类型</th>
              <th>发放人数</th>
              <th>实发合计</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tableData" :key="row.id">
              <td class="mono">{{ row.salaryNo }}</td>
              <td class="mono">{{ row.applyDate }}</td>
              <td>{{ row.salaryMonth }}</td>
              <td class="mono">{{ row.payDate || '-' }}</td>
              <td>{{ row.payTypeName || '-' }}</td>
              <td class="center">{{ row.employeeCount || 0 }} 人</td>
              <td class="money">¥{{ formatMoney(row.totalNetSalary) }}</td>
              <td>
                <span class="status-badge" :class="getStatusClass(row.status)">
                  <span class="status-dot"></span>
                  {{ getStatusText(row.status) }}
                </span>
              </td>
              <td class="action-cell">
                <button class="action-btn view" @click="handleView(row)">查看</button>
                <button
                  v-if="row.status === 'DRAFT' && activeTab === 'my'"
                  class="action-btn submit"
                  @click="handleSubmit(row)"
                >提交</button>
                <button
                  v-if="row.status === 'PENDING' && activeTab === 'my'"
                  class="action-btn cancel"
                  @click="handleCancel(row)"
                >撤回</button>
                <button
                  v-if="activeTab === 'pending' && row.status === 'PENDING'"
                  class="action-btn approve"
                  @click="handleApprove(row)"
                >审批</button>
              </td>
            </tr>
            <tr v-if="tableData.length === 0">
              <td colspan="9" class="empty-cell">
                <span class="empty-text">暂无数据</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <span class="total">共 {{ total }} 条</span>
        <div class="page-btns">
          <button class="page-btn" :disabled="page <= 1" @click="page--; loadData()">上一页</button>
          <span class="page-num">{{ page }} / {{ totalPages }}</span>
          <button class="page-btn" :disabled="page >= totalPages" @click="page++; loadData()">下一页</button>
        </div>
      </div>
    </div>

    <!-- 查看详情弹窗 -->
    <div class="dialog-overlay" v-if="viewDialogVisible" @click.self="viewDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">工资单详情</h3>
          <button class="dialog-close" @click="viewDialogVisible = false">×</button>
        </div>
        <div class="dialog-body" v-if="currentRow">
          <div class="detail-grid">
            <div class="detail-item">
              <label>工资单号</label>
              <span class="mono">{{ currentRow.salaryNo }}</span>
            </div>
            <div class="detail-item">
              <label>申请日期</label>
              <span>{{ currentRow.applyDate }}</span>
            </div>
            <div class="detail-item">
              <label>申请部门</label>
              <span>{{ currentRow.deptName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>申请人</label>
              <span>{{ currentRow.userName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>薪资期间</label>
              <span>{{ currentRow.salaryMonth }}</span>
            </div>
            <div class="detail-item">
              <label>发薪日期</label>
              <span>{{ currentRow.payDate || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>发薪类型</label>
              <span>{{ currentRow.payTypeName || '-' }}</span>
            </div>
            <div class="detail-item">
              <label>发放人数</label>
              <span>{{ currentRow.employeeCount || 0 }} 人</span>
            </div>
          </div>

          <div class="salary-summary">
            <div class="summary-row">
              <span>应发工资合计</span>
              <span class="value">¥{{ formatMoney(currentRow.totalGrossSalary) }}</span>
            </div>
            <div class="summary-row">
              <span>代扣代缴合计</span>
              <span class="value">-¥{{ formatMoney(currentRow.totalDeduction) }}</span>
            </div>
            <div class="summary-row highlight">
              <span>实发工资合计</span>
              <span class="value primary">¥{{ formatMoney(currentRow.totalNetSalary) }}</span>
            </div>
          </div>

          <div class="attachment-section" v-if="currentRow.attachments">
            <label>附件</label>
            <div class="attachment-list">
              <span class="attachment-item">{{ currentRow.attachments }}</span>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="viewDialogVisible = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 新建工资单弹窗 -->
    <div class="dialog-overlay" v-if="formDialogVisible" @click.self="formDialogVisible = false">
      <div class="dialog dialog-md">
        <div class="dialog-header">
          <h3 class="dialog-title">新建工资单</h3>
          <button class="dialog-close" @click="formDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">申请日期</label>
              <input type="date" class="form-input" v-model="form.applyDate" />
            </div>
            <div class="form-item">
              <label class="form-label">发薪日期</label>
              <input type="date" class="form-input" v-model="form.payDate" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label class="form-label">薪资期间</label>
              <input type="month" class="form-input" v-model="form.salaryMonth" />
            </div>
            <div class="form-item">
              <label class="form-label">发薪类型</label>
              <select class="form-input" v-model="form.payType">
                <option value="">请选择</option>
                <option value="MONTHLY">月薪</option>
                <option value="BONUS">奖金</option>
                <option value="ANNUAL">年终奖</option>
                <option value="OTHER">其他</option>
              </select>
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label class="form-label">工资发放人数</label>
              <input type="number" class="form-input" v-model.number="form.employeeCount" placeholder="输入人数" />
            </div>
          </div>

          <div class="form-row">
            <div class="form-item">
              <label class="form-label">应发工资合计</label>
              <input type="number" class="form-input" v-model.number="form.totalGrossSalary" placeholder="0.00" />
            </div>
            <div class="form-item">
              <label class="form-label">代扣代缴合计</label>
              <input type="number" class="form-input" v-model.number="form.totalDeduction" placeholder="0.00" />
            </div>
            <div class="form-item">
              <label class="form-label">实发工资合计</label>
              <input type="number" class="form-input readonly primary" :value="calcNetSalary" readonly />
            </div>
          </div>

          <div class="form-item">
            <label class="form-label">附件</label>
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
          <button class="cyber-btn" @click="formDialogVisible = false">取消</button>
          <button class="cyber-btn primary" :disabled="uploading" @click="handleSave">
            {{ uploading ? '上传中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 审批弹窗 -->
    <div class="dialog-overlay" v-if="approveDialogVisible" @click.self="approveDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">审批工资单</h3>
          <button class="dialog-close" @click="approveDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="approve-info">
            <p>工资单号：<span class="mono">{{ currentRow?.salaryNo }}</span></p>
            <p>薪资期间：<span>{{ currentRow?.salaryMonth }}</span></p>
            <p>发放人数：<span>{{ currentRow?.employeeCount || 0 }} 人</span></p>
            <p>实发合计：<span class="money primary">¥{{ formatMoney(currentRow?.totalNetSalary) }}</span></p>
          </div>
          <div class="form-item">
            <label class="form-label">审批意见</label>
            <textarea class="form-textarea" v-model="approveComment" placeholder="请输入审批意见" rows="3"></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn danger" @click="handleReject">驳回</button>
          <button class="cyber-btn primary" @click="handleApproveConfirm">通过</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { hrApi } from '@/api/hr'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFiles } from '@/utils/upload'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('my')
const tableData = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  salaryMonth: ''
})

const viewDialogVisible = ref(false)
const formDialogVisible = ref(false)
const approveDialogVisible = ref(false)
const currentRow = ref(null)
const approveComment = ref('')

const payTypeMap = {
  MONTHLY: '月薪',
  BONUS: '奖金',
  ANNUAL: '年终奖',
  OTHER: '其他'
}

const uploading = ref(false)
const uploadRef = ref(null)

const form = reactive({
  applyDate: '',
  payDate: '',
  salaryMonth: '',
  payType: '',
  employeeCount: 0,
  totalGrossSalary: 0,
  totalDeduction: 0,
  attachments: []
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const calcNetSalary = computed(() => {
  const gross = form.totalGrossSalary || 0
  const deduct = form.totalDeduction || 0
  const net = gross - deduct
  // 实发工资不能为负数
  return Math.max(0, net).toFixed(2)
})

const formatMoney = (val) => {
  if (!val && val !== 0) return '0.00'
  return parseFloat(val).toFixed(2)
}

const handleFileChange = (file, fileList) => {
  form.attachments = fileList
}

const handleFileRemove = (file, fileList) => {
  form.attachments = fileList
}

const getStatusClass = (status) => {
  const map = { DRAFT: 'draft', PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected', PAID: 'paid', CANCELLED: 'cancelled' }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = { DRAFT: '草稿', PENDING: '待审批', APPROVED: '已通过', REJECTED: '已驳回', PAID: '已发放', CANCELLED: '已撤回' }
  return map[status] || status
}

const loadData = async () => {
  try {
    const params = {
      page: page.value,
      size: pageSize.value
    }
    if (filters.salaryMonth) params.yearMonth = filters.salaryMonth

    if (activeTab.value === 'pending') {
      params.status = 'PENDING'
    }

    const res = await hrApi.getSalaryList(params)
    tableData.value = res.data?.data?.records || res.data?.data || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const resetFilters = () => {
  filters.salaryMonth = ''
  page.value = 1
}

const handleAdd = () => {
  const today = new Date().toISOString().split('T')[0]
  Object.assign(form, {
    applyDate: today,
    payDate: '',
    salaryMonth: today.substring(0, 7),
    payType: '',
    employeeCount: 0,
    totalGrossSalary: 0,
    totalDeduction: 0,
    attachments: []
  })
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  formDialogVisible.value = true
}

const handleView = (row) => {
  currentRow.value = row
  viewDialogVisible.value = true
}

const handleSave = async () => {
  if (!form.applyDate || !form.payDate || !form.salaryMonth || !form.payType) {
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

    await hrApi.createSalary(data)
    ElMessage.success('创建成功')
    formDialogVisible.value = false
    loadData()
    resetForm()
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    uploading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    applyDate: '',
    payDate: '',
    salaryMonth: '',
    payType: '',
    employeeCount: 0,
    totalGrossSalary: 0,
    totalDeduction: 0,
    attachments: []
  })
}

const handleSubmit = async (row) => {
  await ElMessageBox.confirm('确定要提交该工资单进行审批吗？', '提示', { type: 'warning' })
  await hrApi.submitSalary(row.id)
  ElMessage.success('提交成功')
  loadData()
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定要撤回该工资单吗？', '提示', { type: 'warning' })
  await hrApi.cancelSalary(row.id)
  ElMessage.success('撤回成功')
  loadData()
}

const handleApprove = (row) => {
  currentRow.value = row
  approveComment.value = ''
  approveDialogVisible.value = true
}

const handleApproveConfirm = async () => {
  try {
    await hrApi.approveSalary(currentRow.value.id, { comment: approveComment.value })
    ElMessage.success('审批通过')
    approveDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('审批失败', error)
  }
}

const handleReject = async () => {
  try {
    await hrApi.rejectSalary(currentRow.value.id, { comment: approveComment.value })
    ElMessage.success('已驳回')
    approveDialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('驳回失败', error)
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

.salary-wrapper {
  min-height: 100vh;
  background: $bg-primary;
}

.salary-container {
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

  &.active {
    color: $primary;
    background: rgba(96, 165, 250, 0.08);

    .tab-indicator {
      background: $primary;
    }
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: $bg-card;
  border-radius: 12px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;

  label {
    font-size: 14px;
    color: $text-secondary;
  }
}

.filter-input {
  padding: 8px 12px;
  font-size: 14px;
  color: $text-primary;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 8px;
  outline: none;

  &:focus {
    border-color: $primary;
    box-shadow: 0 0 0 2px rgba(96, 165, 250, 0.2);
  }
}

.btn-filter {
  padding: 8px 16px;
  font-size: 14px;
  color: #ffffff;
  background: $primary;
  border: none;
  border-radius: 8px;
  cursor: pointer;

  &:hover {
    background: darken($primary, 8%);
  }
}

.btn-reset {
  padding: 8px 16px;
  font-size: 14px;
  color: $text-secondary;
  background: transparent;
  border: 1px solid $border;
  border-radius: 8px;
  cursor: pointer;

  &:hover {
    color: $text-primary;
    background: $bg-primary;
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

    .center {
      text-align: center;
    }

    .money {
      font-family: 'SF Mono', 'Monaco', monospace;
      color: $primary;
      font-weight: 600;
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

  &.draft {
    background: rgba($text-secondary, 0.15);
    color: $text-secondary;
    .status-dot { background: $text-secondary; }
  }

  &.pending {
    background: rgba($warning, 0.15);
    color: darken($warning, 20%);
    .status-dot { background: $warning; }
  }

  &.approved {
    background: rgba($success, 0.15);
    color: darken($success, 12%);
    .status-dot { background: $success; }
  }

  &.rejected {
    background: rgba($danger, 0.2);
    color: darken($danger, 8%);
    .status-dot { background: $danger; }
  }

  &.paid {
    background: rgba($primary, 0.2);
    color: darken($primary, 8%);
    .status-dot { background: $primary; }
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

  &.submit:hover { color: $success; border-color: $success; }
  &.cancel:hover { color: darken($warning, 20%); border-color: $warning; }
  &.approve:hover { color: $success; border-color: $success; }
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
  padding: 16px;
  background: $bg-card;
  border-radius: 12px;

  .total {
    font-size: 14px;
    color: $text-secondary;
  }

  .page-btns {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .page-btn {
    padding: 8px 16px;
    font-size: 14px;
    color: $text-secondary;
    background: $bg-primary;
    border: 1px solid $border;
    border-radius: 8px;
    cursor: pointer;

    &:hover:not(:disabled) {
      color: $primary;
      border-color: $primary;
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }

  .page-num {
    font-size: 14px;
    color: $text-primary;
  }
}

// 弹窗
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
  max-width: 500px;
  background: $bg-card;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.15);

  &.dialog-md {
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

  &:hover {
    color: $text-primary;
    background: $bg-primary;
  }
}

.dialog-body {
  padding: 24px;
  max-height: 70vh;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid $border;
}

.form-item {
  margin-bottom: 20px;
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

  &.readonly {
    background: darken($bg-primary, 3%);
    color: $primary;
    font-weight: 600;

    &.primary {
      font-size: 16px;
    }
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
}

// 详情弹窗
.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.detail-item {
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

  .mono {
    font-family: 'SF Mono', 'Monaco', monospace;
  }
}

.salary-summary {
  background: $bg-primary;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: $text-primary;

  .value {
    font-family: 'SF Mono', 'Monaco', monospace;
    font-weight: 500;
  }

  &.highlight {
    margin-top: 8px;
    padding-top: 12px;
    border-top: 1px dashed $border;
    font-weight: 600;

    .value {
      color: $primary;
      font-size: 18px;
    }
  }
}

.attachment-section {
  label {
    display: block;
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: 8px;
  }
}

.attachment-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.attachment-item {
  padding: 6px 12px;
  background: $bg-primary;
  border-radius: 6px;
  font-size: 13px;
  color: $text-primary;
}

.approve-info {
  padding: 16px;
  background: $bg-primary;
  border-radius: 12px;
  margin-bottom: 20px;

  p {
    margin: 8px 0;
    font-size: 14px;
    color: $text-secondary;

    span {
      color: $text-primary;
      font-weight: 500;

      &.mono {
        font-family: 'SF Mono', 'Monaco', monospace;
      }

      &.money {
        font-family: 'SF Mono', 'Monaco', monospace;
        color: $primary;
      }
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

  &.danger {
    color: #ffffff;
    background: $danger;
    border-color: $danger;

    &:hover {
      background: darken($danger, 8%);
    }
  }
}

.upload-tip {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 4px;
}

// 响应式设计
@media (max-width: 768px) {
  .salary-container { padding: 16px; }
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
  .dialog-footer { flex-direction: column; gap: 8px; padding: 16px; }
  .cyber-btn { width: 100%; text-align: center; }
}

@media (max-width: 480px) {
  .salary-container { padding: 12px; }
  .page-title { font-size: 15px; }
}
</style>