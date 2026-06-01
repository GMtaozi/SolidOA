<template>
  <div class="approval-center">
    <!-- 顶部导航 -->
    <div class="top-nav">
      <button class="back-btn" @click="router.push('/')">
        <ArrowLeft />
      </button>
      <h2 class="nav-title">审批中心</h2>
      <button class="refresh-btn" @click="loadData">
        <RefreshRight />
      </button>
    </div>

    <!-- 状态选项卡 -->
    <div class="status-tabs">
      <div
        v-for="tab in statusTabs"
        :key="tab.value"
        class="tab-item"
        :class="{ active: currentStatus === tab.value }"
        @click="setStatus(tab.value)"
      >
        {{ tab.label }}
        <span class="tab-count">{{ tab.count }}</span>
      </div>
    </div>

    <!-- 类型筛选 -->
    <div class="type-filter">
      <div
        class="type-chip"
        :class="{ active: currentType === 'ALL' }"
        @click="currentType = 'ALL'"
      >
        全部
      </div>
      <div
        v-for="type in typeList"
        :key="type.value"
        class="type-chip"
        :class="{ active: currentType === type.value }"
        @click="currentType = type.value"
      >
        <component :is="type.icon" class="type-icon" />
        {{ type.label }}
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="filteredList.length === 0" class="empty-state">
      <Check class="empty-icon" />
      <p>暂无审批数据</p>
      <button class="empty-btn" @click="router.push('/')">返回首页</button>
    </div>

    <!-- 审批列表 -->
    <div v-else class="approval-list">
      <div
        v-for="item in filteredList"
        :key="item.id"
        class="approval-card"
        @click="handleCardClick(item)"
      >
        <div class="card-header">
          <component :is="typeIcons[item.businessType] || Check" class="biz-icon" />
          <span class="biz-type">{{ typeNames[item.businessType] }}</span>
          <span class="card-status" :class="getStatusClass(item.status)">
            {{ statusNames[item.status] }}
          </span>
        </div>
        <div class="card-body">
          <div class="info-row">
            <span class="info-label">申请人</span>
            <span class="info-value">{{ item.userName || '未知' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">申请时间</span>
            <span class="info-value">{{ formatDate(item.createTime) }}</span>
          </div>
          <div v-if="item.reason" class="info-row">
            <span class="info-label">申请理由</span>
            <span class="info-value reason">{{ item.reason }}</span>
          </div>
        </div>
        <!-- 审批操作按钮 - 仅当前待办节点审批人有权限 -->
        <div v-if="item.status === 'PENDING' && item.canApprove === true" class="card-actions">
          <button class="quick-action approve" @click.stop="quickApprove(item)">
            <Check />通过
          </button>
          <button class="quick-action reject" @click.stop="quickReject(item)">
            <Close />拒绝
          </button>
          <button class="quick-action" @click.stop="showMoreActions(item)">
            <MoreFilled />
          </button>
        </div>
      </div>
    </div>

    <!-- 详情弹窗 -->
    <div class="dialog-overlay" v-if="detailVisible" @click.self="detailVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">审批详情</h3>
          <button class="dialog-close" @click="detailVisible = false">×</button>
        </div>
        <div class="dialog-body" v-if="currentDetail">
          <div class="detail-section">
            <h4 class="section-title">申请信息</h4>
            <div class="detail-row">
              <span class="label">申请类型</span>
              <span class="value">{{ typeNames[currentDetail.businessType] }}</span>
            </div>
            <div class="detail-row">
              <span class="label">申请人</span>
              <span class="value">{{ currentDetail.userName }}</span>
            </div>
            <div class="detail-row">
              <span class="label">申请时间</span>
              <span class="value">{{ formatDate(currentDetail.createTime) }}</span>
            </div>
            <div class="detail-row">
              <span class="label">申请理由</span>
              <span class="value">{{ currentDetail.reason || '无' }}</span>
            </div>
          </div>
          <div class="detail-section">
            <h4 class="section-title">审批流程</h4>
            <div class="flow-nodes">
              <div
                v-for="(node, idx) in currentDetail.nodes"
                :key="idx"
                class="flow-node"
                :class="{ completed: idx < currentDetail.completedNodes, current: idx === currentDetail.currentNode }"
              >
                <div class="node-dot"></div>
                <div class="node-info">
                  <span class="node-name">{{ node.name }}</span>
                  <span class="node-status">{{ node.status }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="action-btn action-btn-reject" @click="handleReject">
            <Close />拒绝
          </button>
          <button class="action-btn action-btn-approved" @click="handleApprove">
            <Check />通过
          </button>
        </div>
      </div>
    </div>

    <!-- 审批意见弹窗 -->
    <div class="dialog-overlay" v-if="commentDialogVisible" @click.self="commentDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">{{ commentDialogTitle }}</h3>
          <button class="dialog-close" @click="commentDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <textarea
            class="form-textarea"
            v-model="commentForm.comment"
            placeholder="请输入审批意见（选填）"
            rows="4"
          ></textarea>
        </div>
        <div class="dialog-footer">
          <button class="so-btn" @click="commentDialogVisible = false">取消</button>
          <button class="so-btn primary" @click="handleSubmitComment">提交</button>
        </div>
      </div>
    </div>

    <!-- 转交弹窗 -->
    <div class="dialog-overlay" v-if="transferDialogVisible" @click.self="transferDialogVisible = false">
      <div class="dialog">
        <div class="dialog-header">
          <h3 class="dialog-title">转交审批</h3>
          <button class="dialog-close" @click="transferDialogVisible = false">×</button>
        </div>
        <div class="dialog-body">
          <div class="form-item">
            <label class="form-label">转交给</label>
            <input
              type="text"
              class="form-input"
              v-model="transferForm.transferToName"
              placeholder="请输入转交给的姓名"
            />
          </div>
          <div class="form-item">
            <label class="form-label">转交原因</label>
            <textarea
              class="form-textarea"
              v-model="transferForm.reason"
              placeholder="请输入转交原因"
              rows="3"
            ></textarea>
          </div>
        </div>
        <div class="dialog-footer">
          <button class="so-btn" @click="transferDialogVisible = false">取消</button>
          <button class="so-btn primary" @click="submitTransfer">确认转交</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Check, Close, Clock, User, MoreFilled, Document,
  RefreshRight, Plus, Bell, Printer, Money, Coin, Goods, List
} from '@element-plus/icons-vue'
import { workflowApi } from '@/api/workflow'
import { hrApi } from '@/api/hr'

const router = useRouter()

// 状态
const currentStatus = ref('pending')
const currentType = ref('ALL')
const detailVisible = ref(false)
const commentDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const currentDetail = ref(null)
const commentDialogTitle = ref('审批')
const commentType = ref('approve')
const approvalList = ref([])
const loading = ref(false)

// 表单数据
const commentForm = ref({ comment: '' })
const transferForm = ref({ transferToName: '', reason: '' })

// 状态选项卡
const statusTabs = ref([
  { label: '全部', value: 'all', count: 0 },
  { label: '待审批', value: 'pending', count: 0 },
  { label: '已通过', value: 'approved', count: 0 },
  { label: '已拒绝', value: 'rejected', count: 0 }
])

// 类型列表
const typeList = [
  { value: 'LEAVE', label: '请假', icon: 'Calendar' },
  { value: 'EXPENSE', label: '报销', icon: 'Money' },
  { value: 'STAMP', label: '用印', icon: 'Goods' },
  { value: 'PURCHASE', label: '采购', icon: 'Coin' },
  { value: 'OVERTIME', label: '加班', icon: 'Clock' }
]

// 类型映射
const typeIcons = {
  LEAVE: 'Calendar',
  EXPENSE: 'Money',
  STAMP: 'Goods',
  PURCHASE: 'Coin',
  OVERTIME: 'Clock'
}

const typeNames = {
  LEAVE: '请假申请',
  EXPENSE: '费用报销',
  STAMP: '用印申请',
  PURCHASE: '采购申请',
  OVERTIME: '加班申请'
}

const statusNames = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  COMPLETED: '已完成',
  CANCELLED: '已撤回'
}

// 筛选后的列表
const filteredList = computed(() => {
  let result = approvalList.value

  if (currentStatus.value !== 'all') {
    result = result.filter(item => item.status === currentStatus.value)
  }

  if (currentType.value !== 'ALL') {
    result = result.filter(item => item.businessType === currentType.value)
  }

  return result
})

const setStatus = (status) => {
  currentStatus.value = status
  loadData()
}

const getStatusClass = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    COMPLETED: 'success',
    CANCELLED: 'info'
  }
  return map[status] || ''
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

const handleCardClick = (item) => {
  currentDetail.value = item
  detailVisible.value = true
}

const handleApprove = () => {
  commentDialogTitle.value = '审批 - 通过'
  commentType.value = 'approve'
  commentForm.value.comment = ''
  commentDialogVisible.value = true
}

const handleReject = () => {
  commentDialogTitle.value = '审批 - 拒绝'
  commentType.value = 'reject'
  commentForm.value.comment = ''
  commentDialogVisible.value = true
}

const handleSubmitComment = async () => {
  try {
    const result = commentType.value === 'approve' ? 'APPROVE' : 'REJECT'
    const approvalData = { result, comment: commentForm.value.comment }

    // 根据 businessType 动态调用对应的审批方法
    switch (currentDetail.value.businessType) {
      case 'LEAVE':
        await workflowApi.approveLeave(currentDetail.value.id, approvalData)
        break
      case 'STAMP':
        await workflowApi.approveStamp(currentDetail.value.id, approvalData)
        break
      case 'PURCHASE':
        await workflowApi.approvePurchase(currentDetail.value.id, approvalData)
        break
      case 'EXPENSE':
        await hrApi.approveExpense(currentDetail.value.id, approvalData)
        break
      case 'OVERTIME':
        await hrApi.approveOvertime(currentDetail.value.id, approvalData)
        break
      case 'REPAIR_CARD':
        await hrApi.approveRepairCard(currentDetail.value.id, approvalData)
        break
      case 'GO_OUT':
        await hrApi.approveGoOut(currentDetail.value.id, approvalData)
        break
      case 'BUSINESS_TRIP':
        await hrApi.approveBusinessTrip(currentDetail.value.id, approvalData)
        break
      default:
        ElMessage.error('不支持的审批类型: ' + currentDetail.value.businessType)
        return
    }

    ElMessage.success(result === 'APPROVE' ? '审批通过' : '已拒绝')
    commentDialogVisible.value = false
    detailVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('审批失败: ' + (error.message || '未知错误'))
  }
}

// 辅助函数：根据业务类型获取审批API
const getApproveApi = (item) => {
  switch (item.businessType) {
    case 'LEAVE':
      return { api: workflowApi.approveLeave, module: 'workflow' }
    case 'STAMP':
      return { api: workflowApi.approveStamp, module: 'workflow' }
    case 'PURCHASE':
      return { api: workflowApi.approvePurchase, module: 'workflow' }
    case 'EXPENSE':
      return { api: hrApi.approveExpense, module: 'hr' }
    case 'OVERTIME':
      return { api: hrApi.approveOvertime, module: 'hr' }
    case 'REPAIR_CARD':
      return { api: hrApi.approveRepairCard, module: 'hr' }
    case 'GO_OUT':
      return { api: hrApi.approveGoOut, module: 'hr' }
    case 'BUSINESS_TRIP':
      return { api: hrApi.approveBusinessTrip, module: 'hr' }
    default:
      return null
  }
}

const quickApprove = async (item) => {
  const approveInfo = getApproveApi(item)
  if (!approveInfo) {
    ElMessage.error('不支持的审批类型')
    return
  }
  try {
    // 添加确认弹窗，防止误操作
    await ElMessageBox.confirm('确定要通过该申请吗？', '确认审批', { type: 'success' })
    await approveInfo.api(item.id, { result: 'APPROVE', comment: '' })
    ElMessage.success('已通过')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const quickReject = async (item) => {
  const approveInfo = getApproveApi(item)
  if (!approveInfo) {
    ElMessage.error('不支持的审批类型')
    return
  }
  try {
    await ElMessageBox.confirm('确定要拒绝该申请吗？', '提示', { type: 'warning' })
    await approveInfo.api(item.id, { result: 'REJECT', comment: '' })
    ElMessage.success('已拒绝')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const showMoreActions = async (item) => {
  try {
    const action = await ElMessageBox({
      title: '更多操作',
      message: '请选择操作',
      confirmButtonText: '转交',
      cancelButtonText: '取消',
      distinguishCancelAndClose: true,
      showCancelButton: true
    }).then(() => 'transfer').catch(() => {})

    if (action === 'transfer') {
      transferDialogVisible.value = true
      currentDetail.value = item
    }
  } catch (error) {
    // 用户取消
  }
}

// 辅助函数：根据业务类型获取流程信息API
const getFlowApi = (item) => {
  const businessType = item.businessType
  switch (businessType) {
    case 'LEAVE':
      return { api: workflowApi.getLeaveFlow, module: 'workflow' }
    case 'STAMP':
      return { api: workflowApi.getStampFlow, module: 'workflow' }
    case 'PURCHASE':
      return { api: workflowApi.getPurchaseFlow, module: 'workflow' }
    case 'EXPENSE':
      return { api: null, fallbackUrl: `/v1/hr/finance/expense/${item.id}/flow` }
    case 'OVERTIME':
      return { api: null, fallbackUrl: `/v1/hr/attendance/overtime/${item.id}/flow` }
    default:
      return null
  }
}

// 获取流程节点信息
const loadFlowInfo = async (item) => {
  try {
    const flowApi = getFlowApi(item)
    let flowRes = null

    if (flowApi?.api) {
      flowRes = await flowApi.api(item.id)
    } else if (flowApi?.fallbackUrl) {
      // 对于HR模块的流程，使用request直接调用
      const { default: request } = await import('@/utils/request')
      flowRes = await request.get(flowApi.fallbackUrl)
    }

    if (flowRes?.data) {
      item.nodes = flowRes.data.nodes || []
      item.currentNode = flowRes.data.currentNode
      item.totalNodes = flowRes.data.totalNodes || 0
      item.completedNodes = flowRes.data.completedNodes || 0
    }
  } catch (e) {
    // 获取流程信息失败，静默处理
  }
}

const submitTransfer = async () => {
  // 转交校验：必须选择目标用户，不能手动输入姓名
  if (!transferForm.value.transferToName) {
    ElMessage.warning('请选择要转交的目标用户')
    return
  }
  if (transferForm.value.transferToName.length < 2) {
    ElMessage.warning('转交目标用户无效')
    return
  }
  try {
    await workflowApi.transfer(currentDetail.value.id, {
      transferToName: transferForm.value.transferToName,
      reason: transferForm.value.reason
    })
    ElMessage.success('已转交')
    transferDialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('转交失败')
  }
}

const handlePrint = () => {
  window.print()
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 同时获取所有状态的数据
    const [pendingRes, approvedRes, rejectedRes] = await Promise.all([
      workflowApi.getMyTasks().catch(() => ({ data: { data: { records: [], total: 0 } } })),
      workflowApi.getMyProcessed().catch(() => ({ data: { data: [] } }))
    ])

    // 更新选项卡数量
    const pendingCount = pendingRes?.data?.data?.total || pendingRes?.data?.data?.records?.length || 0
    const approvedCount = approvedRes?.data?.total || approvedRes?.data?.data?.length || 0
    const rejectedCount = rejectedRes?.data?.total || rejectedRes?.data?.data?.length || 0

    statusTabs.value[0].count = pendingCount + approvedCount + rejectedCount // 全部
    statusTabs.value[1].count = pendingCount // 待审批
    statusTabs.value[2].count = approvedCount // 已通过
    statusTabs.value[3].count = rejectedCount // 已拒绝

    // 根据状态和类型筛选
    let list = []
    if (currentStatus.value === 'pending') {
      list = pendingRes?.data?.data?.records || []
    } else if (currentStatus.value === 'approved' || currentStatus.value === 'rejected') {
      list = approvedRes?.data?.data || approvedRes?.data || []
    }

    // 处理分页响应格式
    let dataList = []
    if (Array.isArray(list)) {
      dataList = list
    } else if (list?.records) {
      dataList = list.records
    } else if (list?.data && Array.isArray(list.data)) {
      dataList = list.data
    }

    // 过滤业务类型
    if (currentType.value !== 'ALL') {
      dataList = dataList.filter(item => item.businessType === currentType.value)
    }

    // 补充流程节点信息（并行请求，避免 n+1 性能问题）
    await Promise.all(dataList.map(item => loadFlowInfo(item)))

    approvalList.value = dataList
  } catch (error) {
    console.error('加载审批数据失败:', error)
    ElMessage.error('加载审批数据失败')
  } finally {
    loading.value = false
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
$primary-light: #93C5FD;
$success: #34D399;
$success-deep: #2E8B57;
$warning: #FBBF24;
$warning-deep: #D97706;
$danger: #FCA5A5;
$danger-deep: #DC2626;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border: #F0EDE9;
$white: #ffffff;
$money: #D97706;

.approval-center {
  min-height: 100vh;
  background: $bg-primary;
  padding-bottom: 100px;
}

// 顶部导航
.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: $bg-card;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.back-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $bg-primary;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  color: $text-primary;
  transition: all 0.2s;
  &:hover {
    background: darken($bg-primary, 5%);
  }
}

.nav-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  margin: 0;
}

.refresh-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  color: $text-secondary;
  border-radius: 12px;
  transition: all 0.2s;
  &:hover {
    background: $bg-primary;
    color: $primary;
  }
}

// 状态选项卡
.status-tabs {
  display: flex;
  gap: 8px;
  padding: 16px 20px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  &::-webkit-scrollbar {
    display: none;
  }
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: $bg-card;
  border: 1px solid $border;
  border-radius: 20px;
  font-size: 14px;
  color: $text-secondary;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  &.active {
    background: $primary;
    border-color: $primary;
    color: $white;
    .tab-count {
      background: rgba(255, 255, 255, 0.2);
      color: $white;
    }
  }
  &:hover:not(.active) {
    border-color: $primary;
    color: $primary;
  }
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: $bg-primary;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

// 类型筛选
.type-filter {
  display: flex;
  gap: 8px;
  padding: 0 20px 16px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  &::-webkit-scrollbar {
    display: none;
  }
}

.type-chip {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: $bg-card;
  border: 1px solid $border;
  border-radius: 16px;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  &.active {
    background: rgba($primary, 0.1);
    border-color: $primary;
    color: $primary;
  }
  &:hover:not(.active) {
    background: $bg-primary;
  }
}

.type-icon {
  width: 14px;
  height: 14px;
}

// 加载状态
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  .loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid $border;
    border-top-color: $primary;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  p {
    margin-top: 16px;
    color: $text-secondary;
    font-size: 14px;
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  .empty-icon {
    width: 64px;
    height: 64px;
    color: $text-secondary;
    opacity: 0.5;
  }
  p {
    margin-top: 16px;
    color: $text-secondary;
    font-size: 14px;
  }
}

.empty-btn {
  margin-top: 24px;
  padding: 10px 24px;
  background: $primary;
  border: none;
  border-radius: 12px;
  color: $white;
  font-size: 14px;
  cursor: pointer;
  &:hover {
    background: darken($primary, 5%);
  }
}

// 审批列表
.approval-list {
  padding: 0 20px;
}

.approval-card {
  background: $bg-card;
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.biz-icon {
  width: 20px;
  height: 20px;
  color: $primary;
}

.biz-type {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
}

.card-status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  &.warning {
    background: rgba($warning, 0.15);
    color: $warning-deep;
  }
  &.success {
    background: rgba($success, 0.15);
    color: $success-deep;
  }
  &.danger {
    background: rgba($danger, 0.15);
    color: $danger-deep;
  }
  &.info {
    background: $bg-primary;
    color: $text-secondary;
  }
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
}

.info-label {
  color: $text-secondary;
  min-width: 70px;
}

.info-value {
  color: $text-primary;
  &.reason {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.card-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid $border;
}

.quick-action {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 8px;
  font-size: 13px;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: $bg-card;
    border-color: $primary;
    color: $primary;
  }
  &.approve {
    background: rgba($success, 0.1);
    border-color: $success;
    color: $success-deep;
    &:hover {
      background: rgba($success, 0.15);
    }
  }
  &.reject {
    background: rgba($danger, 0.1);
    border-color: $danger;
    color: $danger-deep;
    &:hover {
      background: rgba($danger, 0.15);
    }
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
  max-width: 480px;
  max-height: 90vh;
  background: $bg-card;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.15);
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
  font-weight: 600;
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
    background: $bg-primary;
  }
}

.dialog-body {
  padding: 20px 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid $border;
}

// 表单
.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: $text-secondary;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 10px 16px;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 8px;
  font-size: 14px;
  color: $text-primary;
  transition: all 0.2s;
  &:focus {
    outline: none;
    border-color: $primary;
    box-shadow: 0 0 0 2px rgba($primary, 0.1);
  }
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

// 详情
.detail-section {
  margin-bottom: 20px;
  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: $text-secondary;
}

.detail-row {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px solid $border;
  &:last-child {
    border-bottom: none;
  }
  .label {
    color: $text-secondary;
    min-width: 80px;
  }
  .value {
    color: $text-primary;
  }
}

.flow-nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-node {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: $bg-primary;
  border-radius: 8px;
  &.completed {
    .node-dot {
      background: $success;
    }
    .node-status {
      color: $success;
    }
  }
  &.current {
    background: rgba($primary, 0.1);
    .node-dot {
      background: $primary;
      animation: pulse 2s infinite;
    }
    .node-status {
      color: $primary;
    }
  }
}

.node-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $text-secondary;
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.node-name {
  font-size: 14px;
  color: $text-primary;
}

.node-status {
  font-size: 12px;
  color: $text-secondary;
}

@keyframes pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba($primary, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba($primary, 0);
  }
}

// 操作按钮
.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 16px;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  &-reject {
    background: rgba($danger, 0.1);
    color: $danger-deep;
    &:hover {
      background: rgba($danger, 0.15);
    }
  }
  &-approved {
    background: $success;
    color: $white;
    &:hover {
      background: darken($success, 5%);
    }
  }
}

.so-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  background: $bg-primary;
  border: 1px solid $border;
  border-radius: 12px;
  font-size: 14px;
  color: $text-secondary;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    background: darken($bg-primary, 3%);
  }
  &.primary {
    background: $primary;
    border-color: $primary;
    color: $white;
    &:hover {
      background: darken($primary, 5%);
    }
  }
}
</style>
