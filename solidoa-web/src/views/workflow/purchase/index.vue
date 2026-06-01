<template>
  <div class="purchase-wrapper">
    <div class="purchase-container">
      <div class="page-header">
        <h2 class="page-title">采购申请</h2>
        <button class="btn-primary" @click="handleAdd">
          <span class="btn-icon">+</span>
          <span>新增采购</span>
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
              <th>采购单号</th>
              <th>申请日期</th>
              <th>采购类型</th>
              <th>申请事由</th>
              <th>期望交付日期</th>
              <th>总金额</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in tableData" :key="row.id">
              <td class="mono">{{ row.purchaseNo }}</td>
              <td class="mono">{{ row.applyDate }}</td>
              <td>
                <span class="type-badge" :class="getTypeClass(row.purchaseType)">
                  {{ typeMap[row.purchaseType] || row.purchaseType }}
                </span>
              </td>
              <td class="reason-cell">{{ row.reason }}</td>
              <td class="mono">{{ row.deliveryDate }}</td>
              <td class="money">¥{{ formatMoney(row.totalAmount) }}</td>
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
              <td colspan="8" class="empty-cell"><span class="empty-text">暂无数据</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 采购申请表单弹窗 -->
    <div class="dialog-overlay" v-if="dialogVisible" @click.self="dialogVisible = false">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h3 class="dialog-title">新增采购</h3>
          <button class="dialog-close" @click="dialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-row">
            <div class="form-item">
              <label class="form-label">采购类型</label>
              <select class="form-input" v-model="form.purchaseType">
                <option value="">请选择</option>
                <option value="OFFICE">办公用品</option>
                <option value="EXPERIMENT">实验耗材</option>
                <option value="OTHER">其他</option>
              </select>
            </div>
            <div class="form-item">
              <label class="form-label">期望交付日期</label>
              <input type="date" class="form-input" v-model="form.deliveryDate" />
            </div>
          </div>
          <div class="form-item">
            <label class="form-label">申请事由</label>
            <textarea class="form-textarea" v-model="form.reason" placeholder="请输入申请事由" rows="2"></textarea>
          </div>

          <!-- 采购明细表格 -->
          <div class="items-section">
            <div class="section-title">采购明细</div>
            <div class="items-table">
              <div class="table-header">
                <span class="col-name">商品名称</span>
                <span class="col-qty">数量</span>
                <span class="col-unit">单位</span>
                <span class="col-price">预估单价</span>
                <span class="col-action"></span>
              </div>
              <div v-for="(item, index) in form.items" :key="index" class="table-row">
                <input type="text" class="col-name form-input" v-model="item.name" placeholder="商品名称" />
                <input type="number" class="col-qty form-input" v-model.number="item.quantity" placeholder="数量" min="1" />
                <input type="text" class="col-unit form-input" v-model="item.unit" placeholder="单位" />
                <input type="number" class="col-price form-input" v-model.number="item.price" placeholder="0.00" step="0.01" />
                <button class="col-action remove-btn" @click="removeItem(index)" :disabled="form.items.length <= 1">×</button>
              </div>
              <button class="add-item-btn" @click="addItem">
                <span>+</span> 添加商品
              </button>
            </div>
            <div class="total-row">
              <span>预估总金额：</span>
              <span class="total-amount">¥{{ formatMoney(totalAmount) }}</span>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="cyber-btn" @click="dialogVisible = false">取消</button>
          <button class="cyber-btn primary" :disabled="isSubmitting" @click="handleSubmit">提交</button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog dialog-lg">
        <div class="dialog-header">
          <h3 class="dialog-title">采购详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">采购单号</span>
              <span class="detail-value mono">{{ currentDetail.purchaseNo || '-' }}</span>
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
              <span class="detail-label">采购类型</span>
              <span class="detail-value">
                <span class="type-badge" :class="getTypeClass(currentDetail.purchaseType)">
                  {{ typeMap[currentDetail.purchaseType] || '-' }}
                </span>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">期望交付日期</span>
              <span class="detail-value">{{ currentDetail.deliveryDate || '-' }}</span>
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
            <div class="detail-item">
              <span class="detail-label">总金额</span>
              <span class="detail-value money">¥{{ formatMoney(currentDetail.totalAmount) }}</span>
            </div>
            <div class="detail-item full-width">
              <span class="detail-label">申请事由</span>
              <span class="detail-value reason">{{ currentDetail.reason || '-' }}</span>
            </div>
          </div>

          <!-- 采购明细 -->
          <div class="items-detail" v-if="currentDetail.items && currentDetail.items.length">
            <div class="section-title">采购明细</div>
            <table class="detail-table">
              <thead>
                <tr>
                  <th>商品名称</th>
                  <th>数量</th>
                  <th>单位</th>
                  <th>预估单价</th>
                  <th>小计</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in currentDetail.items" :key="index">
                  <td>{{ item.name }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ item.unit }}</td>
                  <td>¥{{ formatMoney(item.price) }}</td>
                  <td>¥{{ formatMoney(item.quantity * item.price) }}</td>
                </tr>
              </tbody>
            </table>
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
import { reactive, ref, computed, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('my')
const tableData = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const isSubmitting = ref(false)
const currentDetail = ref({})

const typeMap = { OFFICE: '办公用品', EXPERIMENT: '实验耗材', OTHER: '其他' }

// 采购金额阈值配置：单笔采购超过此金额需要额外审批
const AMOUNT_THRESHOLD = 100000 // 10万元

const defaultItem = () => ({ name: '', quantity: 1, unit: '', price: 0 })

const form = reactive({
  purchaseType: '',
  deliveryDate: '',
  reason: '',
  items: [defaultItem()]
})

// 计算总金额
const totalAmount = computed(() => {
  return form.items.reduce((sum, item) => {
    return sum + (item.quantity || 0) * (item.price || 0)
  }, 0)
})

let submitLock = false
const withDebounce = async (fn, delay = 500) => {
  if (submitLock) { ElMessage.info('操作过于频繁，请稍后'); return true }
  submitLock = true
  try { return await fn() } finally { setTimeout(() => { submitLock = false }, delay) }
}

const getTypeClass = (type) => ({ OFFICE: 'office', EXPERIMENT: 'experiment', OTHER: 'other' }[type] || '')
const getStatusClass = (status) => ({ PENDING: 'pending', APPROVED: 'approved', REJECTED: 'rejected' }[status] || '')
const getStatusText = (status) => ({ PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }[status] || status)

const formatMoney = (val) => {
  if (!val && val !== 0) return '0.00'
  return parseFloat(val).toFixed(2)
}

const addItem = () => {
  form.items.push(defaultItem())
}

const removeItem = (index) => {
  if (form.items.length > 1) {
    form.items.splice(index, 1)
  }
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my' ? await workflowApi.getPurchaseList({}) : await workflowApi.getMyTasks()
    tableData.value = res.data?.data?.records || res.data?.data || []
  } catch (error) { console.error('加载数据失败', error) }
}

const handleAdd = () => {
  const afterWeek = new Date()
  afterWeek.setDate(afterWeek.getDate() + 7)
  Object.assign(form, {
    purchaseType: '',
    deliveryDate: afterWeek.toISOString().split('T')[0],
    reason: '',
    items: [defaultItem()]
  })
  dialogVisible.value = true
}

const handleView = (row) => {
  currentDetail.value = row
  detailVisible.value = true
}

const handleApprove = (row) => {
  currentDetail.value = row
  ElMessageBox.confirm(
    `采购类型：${typeMap[row.purchaseType] || row.purchaseType}\n申请事由：${row.reason}\n期望交付日期：${row.deliveryDate}\n总金额：¥${formatMoney(row.totalAmount)}`,
    '确认通过采购申请？',
    { confirmButtonText: '通过', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    await withDebounce(async () => {
      await workflowApi.approvePurchase(row.id, { approveResult: 'APPROVE', comment: '同意' })
      ElMessage.success('审批成功')
      loadData()
    })
  }).catch(() => {})
}

const handleApproveConfirm = async () => {
  await withDebounce(async () => {
    await workflowApi.approvePurchase(currentDetail.value.id, { approveResult: 'APPROVE', comment: '同意' })
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
    await workflowApi.approvePurchase(currentDetail.value.id, { approveResult: 'REJECT', comment })
    ElMessage.success('已拒绝')
    detailVisible.value = false
    loadData()
  })
}

const handleCancel = async (row) => {
  if (row.status !== 'PENDING') { ElMessage.warning('只能撤回待审批状态的申请'); return }
  await ElMessageBox.confirm('确定要撤回该采购申请吗？', '提示', { type: 'warning' })
  await withDebounce(async () => {
    await workflowApi.cancelPurchase(row.id)
    ElMessage.success('撤回成功')
    loadData()
  })
}

const handleSubmit = async () => {
  if (!form.purchaseType) { ElMessage.warning('请选择采购类型'); return }
  if (!form.reason.trim()) { ElMessage.warning('请输入申请事由'); return }
  if (!form.deliveryDate) { ElMessage.warning('请选择期望交付日期'); return }

  // 验证商品明细
  const validItems = form.items.filter(item => item.name.trim())
  if (validItems.length === 0) { ElMessage.warning('请至少添加一个商品'); return }

  // 采购金额阈值校验：超过阈值需要确认
  const amount = totalAmount.value
  if (amount > AMOUNT_THRESHOLD) {
    try {
      await ElMessageBox.confirm(
        `采购总金额 ¥${amount.toLocaleString()} 超过阈值 ¥${AMOUNT_THRESHOLD.toLocaleString()}，是否确认提交？`,
        '金额预警',
        { type: 'warning' }
      )
    } catch {
      return
    }
  }

  await withDebounce(async () => {
    try {
      await workflowApi.createPurchase({ ...form, totalAmount: amount })
      ElMessage.success('提交成功')
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('提交失败', error)
      ElMessage.error('提交失败')
    }
  })
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2; $bg-card: #ffffff; $primary: #60A5FA; $success: #34D399; $warning: #FBBF24; $danger: #FCA5A5; $text-primary: #3B3B3B; $text-secondary: #9CA3AF; $border: #F0EDE9;

.purchase-wrapper { min-height: 100vh; background: $bg-primary; }
.purchase-container { padding: 30px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px; }
.page-title { font-size: 18px; font-weight: 500; color: $text-primary; margin: 0; }
.btn-primary { display: inline-flex; align-items: center; gap: 8px; padding: 10px 20px; font-size: 14px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } .btn-icon { font-size: 16px; } }
.tabs { display: flex; gap: 8px; margin-bottom: 24px; background: $bg-card; padding: 6px; border-radius: 12px; }
.tab-item { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px 20px; font-size: 14px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); .tab-indicator { width: 8px; height: 8px; border-radius: 50%; background: transparent; } &:hover { color: $text-primary; } &.active { color: $primary; background: rgba($primary, 0.08); .tab-indicator { background: $primary; } } }
.table-wrapper { background: $bg-card; border-radius: 16px; overflow: hidden; }
.data-table { width: 100%; border-collapse: collapse; thead { background: $bg-primary; th { padding: 14px 16px; font-size: 13px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; } } tbody { tr { transition: background 0.15s ease; &:hover { background: rgba($primary, 0.03); } &:not(:last-child) td { border-bottom: 1px solid $border; } } td { padding: 16px; font-size: 14px; color: $text-primary; } .mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } .reason-cell { max-width: 150px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: $text-secondary; } .money { color: $primary; font-weight: 600; } .empty-cell { text-align: center; padding: 60px 16px; .empty-text { color: $text-secondary; font-size: 14px; } } } }
.type-badge { display: inline-block; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; &.office { background: rgba($primary, 0.2); color: darken($primary, 8%); } &.experiment { background: rgba($warning, 0.2); color: darken($warning, 20%); } &.other { background: rgba(167, 139, 250, 0.2); color: #8b6fe6; } }
.status-badge { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; font-size: 12px; font-weight: 500; border-radius: 20px; .status-dot { width: 6px; height: 6px; border-radius: 50%; } &.pending { background: rgba($warning, 0.15); color: darken($warning, 20%); .status-dot { background: $warning; } } &.approved { background: rgba($success, 0.15); color: darken($success, 12%); .status-dot { background: $success; } } &.rejected { background: rgba($danger, 0.2); color: darken($danger, 8%); .status-dot { background: $danger; } } }
.action-cell { display: flex; gap: 8px; }
.action-btn { padding: 6px 14px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: $primary; background: rgba($primary, 0.05); } &.cancel:hover { color: darken($warning, 20%); border-color: $warning; } &.approve:hover { color: darken($success, 12%); border-color: $success; } }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.dialog { width: 100%; max-width: 700px; background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0,0,0,0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 500; color: $text-primary; }
.dialog-close { width: 32px; height: 32px; font-size: 20px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 24px; max-height: 70vh; overflow-y: auto; }
.form-item { margin-bottom: 16px; }
.form-label { display: block; margin-bottom: 6px; font-size: 13px; font-weight: 500; color: $text-primary; }
.form-row { display: flex; gap: 16px; .form-item { flex: 1; } }
.form-input { width: 100%; padding: 10px 14px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.form-textarea { width: 100%; padding: 10px 14px; font-size: 14px; color: $text-primary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; outline: none; resize: vertical; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-sizing: border-box; font-family: inherit; &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba($primary, 0.2); } }
.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid $border; }
.cyber-btn { padding: 10px 20px; font-size: 14px; font-weight: 500; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $border; } &.primary { color: #ffffff; background: $primary; border-color: $primary; box-shadow: 0 4px 12px rgba($primary, 0.25); &:hover { background: darken($primary, 8%); } &:disabled { opacity: 0.6; cursor: not-allowed; } } &.danger { color: #ffffff; background: $danger; border-color: $danger; &:hover { background: darken($danger, 8%); } } }

.items-section { margin-top: 20px; padding: 16px; background: $bg-primary; border-radius: 12px; }
.section-title { font-size: 14px; font-weight: 600; color: $text-primary; margin-bottom: 12px; }
.items-table { margin-bottom: 12px; }
.table-header { display: flex; gap: 8px; padding: 8px 0; font-size: 12px; font-weight: 500; color: $text-secondary; border-bottom: 1px solid $border; }
.table-row { display: flex; gap: 8px; padding: 8px 0; align-items: center; }
.col-name { flex: 3; }
.col-qty { flex: 1; }
.col-unit { flex: 1; }
.col-price { flex: 1.5; }
.col-action { flex: 0 0 32px; }
.remove-btn { width: 28px; height: 28px; font-size: 16px; color: $danger; background: rgba($danger, 0.1); border: none; border-radius: 6px; cursor: pointer; &:hover { background: rgba($danger, 0.2); } &:disabled { opacity: 0.3; cursor: not-allowed; } }
.add-item-btn { display: flex; align-items: center; justify-content: center; gap: 6px; width: 100%; padding: 10px; font-size: 13px; color: $primary; background: rgba($primary, 0.08); border: 1px dashed $primary; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { background: rgba($primary, 0.15); } span { font-size: 16px; } }
.total-row { display: flex; justify-content: flex-end; align-items: center; gap: 12px; padding-top: 12px; border-top: 1px solid $border; font-size: 14px; color: $text-primary; }
.total-amount { font-size: 18px; font-weight: 600; color: $primary; }

.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; margin-bottom: 20px; }
.detail-item { display: flex; flex-direction: column; gap: 4px; &.full-width { grid-column: 1 / -1; } }
.detail-label { font-size: 12px; color: $text-secondary; }
.detail-value { font-size: 14px; color: $text-primary; &.mono { font-family: 'SF Mono', 'Monaco', monospace; font-size: 13px; } &.money { color: $primary; font-weight: 600; } &.reason { line-height: 1.6; color: $text-secondary; } }
.items-detail { margin-top: 16px; padding: 16px; background: $bg-primary; border-radius: 12px; .section-title { margin-bottom: 12px; } }
.detail-table { width: 100%; border-collapse: collapse; th { padding: 10px 12px; font-size: 12px; font-weight: 500; color: $text-secondary; text-align: left; border-bottom: 1px solid $border; background: $bg-card; } td { padding: 10px 12px; font-size: 13px; color: $text-primary; border-bottom: 1px solid $border; } }

// 响应式设计
@media (max-width: 768px) {
  .purchase-container { padding: 16px; }
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
  .table-row { flex-wrap: wrap; }
  .col-price { flex: 1 1 100%; }
  .dialog-footer { flex-direction: column; gap: 8px; padding: 16px; }
  .cyber-btn { width: 100%; text-align: center; }
}

@media (max-width: 480px) {
  .purchase-container { padding: 12px; }
  .detail-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 15px; }
}
</style>