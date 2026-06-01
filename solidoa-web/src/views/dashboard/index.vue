<template>
  <div class="so-dashboard">
    <!-- Header -->
    <header class="dashboard-header">
      <div class="header-left">
        <div class="logo">
          <span class="logo-icon"><el-icon><Document /></el-icon></span>
          <span class="logo-text">SolidOA</span>
        </div>
        <div class="header-time">{{ currentTime }}</div>
      </div>
      <div class="header-right">
        <div class="user-info">
          <div class="avatar">{{ userInitials }}</div>
          <div class="user-details">
            <span class="username">{{ username }}</span>
            <span class="role">{{ role }}</span>
          </div>
        </div>
        <div class="notification-bell">
          <span class="bell-icon"><el-icon><Bell /></el-icon></span>
          <span class="notification-dot"></span>
        </div>
      </div>
    </header>

    <!-- Stats Cards -->
    <section class="stats-grid">
      <div v-for="(stat, index) in stats" :key="index" class="stat-card" :class="stat.type" :style="{ '--delay': index * 0.1 + 's' }">
        <div class="stat-header">
          <span class="stat-label">{{ stat.label }}</span>
          <span class="stat-icon"><el-icon><component :is="stat.iconComponent" /></el-icon></span>
        </div>
        <div class="stat-body">
          <div class="stat-primary">{{ stat.primary }}</div>
          <div class="stat-secondary">{{ stat.secondary }}</div>
        </div>
        <div class="stat-footer">
          <span class="stat-trend" :class="stat.trend">
            <el-icon v-if="stat.trend === 'up'"><Top /></el-icon>
            <el-icon v-else><Bottom /></el-icon>
            {{ stat.trendValue }}
          </span>
          <span class="stat-period">较上周</span>
        </div>
      </div>
    </section>

    <!-- Main Content Grid -->
    <div class="main-grid">
      <!-- Recent Approvals -->
      <section class="approval-section">
        <div class="section-header">
          <h2 class="section-title">
            <span class="title-icon"><el-icon><Document /></el-icon></span>
            待审批流程
          </h2>
          <div class="section-actions">
            <button class="btn-ghost" @click="router.push('/approval')">查看全部</button>
          </div>
        </div>
        <div class="approval-list">
          <div v-for="(item, index) in approvals" :key="index" class="approval-item" :style="{ '--delay': index * 0.08 + 's' }">
            <div class="approval-icon" :class="item.type"><el-icon><component :is="item.iconComponent" /></el-icon></div>
            <div class="approval-info">
              <div class="approval-title">{{ item.title }}</div>
              <div class="approval-meta">
                <span class="applicant">{{ item.applicant }}</span>
                <span class="separator">·</span>
                <span class="time">{{ item.time }}</span>
              </div>
            </div>
            <div class="approval-amount" :class="item.type">
              ¥{{ item.amount.toLocaleString() }}
            </div>
            <div class="approval-actions">
              <button class="btn-approve" @click="handleApprove(item)">通过</button>
              <button class="btn-reject" @click="handleReject(item)">拒绝</button>
            </div>
          </div>
        </div>
      </section>

      <!-- Quick Actions -->
      <section class="quick-actions">
        <div class="section-header">
          <h2 class="section-title">
            <span class="title-icon"><el-icon><Plus /></el-icon></span>
            快捷操作
          </h2>
        </div>
        <div class="action-grid">
          <button v-for="(action, index) in quickActions" :key="index" class="action-btn" @click="navigateTo(action.path)" :style="{ '--delay': index * 0.05 + 's' }">
            <span class="action-icon"><el-icon><component :is="action.iconComponent" /></el-icon></span>
            <span class="action-label">{{ action.label }}</span>
          </button>
        </div>
      </section>

      <!-- Activity Timeline -->
      <section class="activity-section">
        <div class="section-header">
          <h2 class="section-title">
            <span class="title-icon"><el-icon><Bell /></el-icon></span>
            系统动态
          </h2>
        </div>
        <div class="timeline">
          <div v-for="(event, index) in activities" :key="index" class="timeline-item" :style="{ '--delay': index * 0.06 + 's' }">
            <div class="timeline-dot" :class="event.type"></div>
            <div class="timeline-content">
              <div class="event-title">{{ event.title }}</div>
              <div class="event-time">{{ event.time }}</div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- Footer -->
    <footer class="dashboard-footer">
      <div class="system-status">
        <span class="status-indicator online"></span>
        <span>系统正常</span>
      </div>
      <div class="footer-info">
        SolidOA v1.0.0 · 未来办公系统
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTime } from '@/composables/useTime'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Coin, Check, Close, Plus, Document, Calendar, TrendCharts, More, Bell, Stamp, Top, Bottom, ShoppingCart, Goods, Memo } from '@element-plus/icons-vue'
import { workflowApi } from '@/api'

const router = useRouter()
const { currentTime } = useTime()
const userStore = useUserStore()

const username = computed(() => userStore.userInfo?.username || '')
const role = computed(() => userStore.userInfo?.roleName || '')
const userInitials = computed(() => {
  const name = username.value
  return name ? name.slice(0, 2).toUpperCase() : ''
})

const stats = ref([
  { iconComponent: Clock, label: '待审批', primary: '12', secondary: '条流程待处理', type: 'pending', trend: 'up', trendValue: '3' },
  { iconComponent: Coin, label: '进行中', primary: '28', secondary: '条流程进行中', type: 'active', trend: 'up', trendValue: '5' },
  { iconComponent: Check, label: '已完成', primary: '156', secondary: '条流程已办结', type: 'completed', trend: 'up', trendValue: '12' },
  { iconComponent: Close, label: '已驳回', primary: '3', secondary: '条流程已驳回', type: 'rejected', trend: 'down', trendValue: '2' }
])

const approvals = ref([
  { iconComponent: Document, title: '差旅费用报销 - 张三', applicant: '张三维', time: '2小时前', amount: 12580, type: 'expense' },
  { iconComponent: Stamp, title: '合同用印申请', applicant: '李思思', time: '4小时前', amount: 0, type: 'seal' },
  { iconComponent: ShoppingCart, title: '办公设备采购', applicant: '王五', time: '5小时前', amount: 45000, type: 'procurement' },
  { iconComponent: Document, title: '招待费用报销', applicant: '赵六', time: '1天前', amount: 3800, type: 'expense' },
  { iconComponent: Stamp, title: '项目用印申请', applicant: '孙七', time: '1天前', amount: 0, type: 'seal' }
])

const quickActions = [
  { iconComponent: Plus, label: '新建报销', path: '/expense' },
  { iconComponent: Stamp, label: '用印申请', path: '/stamp' },
  { iconComponent: ShoppingCart, label: '采购申请', path: '/purchase' },
  { iconComponent: Calendar, label: '会议预约', path: '/schedule' },
  { iconComponent: Goods, label: '物品领用', path: '/home' },
  { iconComponent: Memo, label: '新建请假', path: '/leave' }
]

const activities = [
  { title: '孙七 提交了 项目用印申请', time: '1天前', type: 'submit' },
  { title: '赵六 的差旅报销已通过', time: '1天前', type: 'approve' },
  { title: '系统自动驳回 王五 的采购申请', time: '2天前', type: 'reject' },
  { title: '李思思 创建了 部门会议', time: '2天前', type: 'create' },
  { title: '孙七 完成了 报销流程', time: '3天前', type: 'complete' }
]

const handleApprove = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要通过「${item.title}」吗？`, '审批确认', { type: 'success' })
    // 根据类型调用对应的审批API
    if (item.id) {
      if (item.type === 'seal') {
        await workflowApi.approveStamp(item.id, { action: 'approve' })
      } else if (item.type === 'procurement') {
        await workflowApi.approvePurchase(item.id, { action: 'approve' })
      } else {
        // 默认按请假处理
        await workflowApi.approveLeave(item.id, { action: 'approve' })
      }
      // 从列表中移除已审批项
      const index = approvals.value.findIndex(a => a.id === item.id)
      if (index > -1) approvals.value.splice(index, 1)
      ElMessage.success('已通过')
    } else {
      ElMessage.warning('缺少审批ID，请检查数据')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('审批失败:', error)
      ElMessage.error('审批失败，请重试')
    }
  }
}

const handleReject = async (item) => {
  try {
    await ElMessageBox.confirm(`确定要拒绝「${item.title}」吗？`, '审批确认', { type: 'warning' })
    // 根据类型调用对应的审批API
    if (item.id) {
      if (item.type === 'seal') {
        await workflowApi.approveStamp(item.id, { action: 'reject' })
      } else if (item.type === 'procurement') {
        await workflowApi.approvePurchase(item.id, { action: 'reject' })
      } else {
        // 默认按请假处理
        await workflowApi.approveLeave(item.id, { action: 'reject' })
      }
      // 从列表中移除已拒绝项
      const index = approvals.value.findIndex(a => a.id === item.id)
      if (index > -1) approvals.value.splice(index, 1)
      ElMessage.success('已拒绝')
    } else {
      ElMessage.warning('缺少审批ID，请检查数据')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('拒绝失败:', error)
      ElMessage.error('拒绝失败，请重试')
    }
  }
}

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped lang="scss">
/* ========================================
   SolidOA Soft & Comfortable Dashboard
   ======================================== */

$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$color-primary: #60A5FA;
$color-primary-light: #93C5FD;
$color-expense: #34D399;
$color-seal: #FBBF24;
$color-procurement: #FCA5A5;
$color-success: #34D399;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$divider: #F0EDE9;
$color-procurement-deep: #DC2626;
$white: #ffffff;

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.so-dashboard {
  min-height: 100vh;
  background: $bg-primary;
  color: $text-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  position: relative;
  overflow-x: hidden;
}

/* Header */
.dashboard-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: $bg-card;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 2rem;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.logo-icon {
  font-size: 1.75rem;
  color: $color-primary;
}

.logo-text {
  font-size: 1.25rem;
  font-weight: 600;
  color: $text-primary;
}

.header-time {
  font-size: 0.875rem;
  color: $text-secondary;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, $color-primary, $color-primary-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.875rem;
  color: $white;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-weight: 500;
  font-size: 0.875rem;
}

.role {
  font-size: 0.75rem;
  color: $text-secondary;
}

.notification-bell {
  position: relative;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: $bg-primary;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.notification-bell:hover {
  box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.bell-icon {
  font-size: 1.125rem;
  color: $text-secondary;
}

.notification-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: $color-procurement;
  border-radius: 50%;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1.5rem;
  padding: 2rem;
  position: relative;
  z-index: 1;
}

.stat-card {
  position: relative;
  padding: 1.5rem;
  background: $bg-card;
  border-radius: 16px;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
  border-top: 3px solid transparent;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
}

.stat-card.pending { border-top-color: $color-primary; }
.stat-card.active { border-top-color: $color-seal; }
.stat-card.completed { border-top-color: $color-expense; }
.stat-card.rejected { border-top-color: $color-procurement; }

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
}

.stat-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: $text-secondary;
}

.stat-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
}

.stat-card.pending .stat-icon {
  background: rgba($color-primary, 0.1);
  color: $color-primary;
}

.stat-card.active .stat-icon {
  background: rgba($color-seal, 0.1);
  color: $color-seal;
}

.stat-card.completed .stat-icon {
  background: rgba($color-expense, 0.1);
  color: $color-expense;
}

.stat-card.rejected .stat-icon {
  background: rgba($color-procurement, 0.1);
  color: $color-procurement;
}

.stat-body {
  margin-bottom: 1rem;
}

.stat-primary {
  font-size: 1.75rem;
  font-weight: 600;
  line-height: 1;
  margin-bottom: 0.375rem;
  color: $text-primary;
}

.stat-secondary {
  font-size: 0.75rem;
  color: $text-secondary;
}

.stat-footer {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding-top: 0.75rem;
  border-top: 1px solid $divider;
}

.stat-trend {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  font-weight: 600;
}

.stat-trend.up { color: $color-expense; }
.stat-trend.down { color: $color-procurement; }

.stat-period {
  font-size: 0.75rem;
  color: $text-secondary;
}

/* Main Grid */
.main-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 1.5rem;
  padding: 0 2rem 2rem;
  position: relative;
  z-index: 1;
}

/* Approval Section */
.approval-section {
  background: $bg-card;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 500;
  color: $text-primary;
}

.title-icon {
  color: $color-primary;
}

.btn-ghost {
  padding: 0.5rem 1rem;
  background: transparent;
  border: none;
  border-radius: 12px;
  color: $text-secondary;
  font-size: 0.875rem;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-ghost:hover {
  background: $bg-primary;
  color: $color-primary;
}

.approval-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.approval-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: $bg-primary;
  border-radius: 12px;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.approval-item:hover {
  background: #fcfaf7;
  box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
}

.approval-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.125rem;
  flex-shrink: 0;
}

.approval-icon.expense {
  background: rgba(52, 211, 153, 0.15);
  color: $color-expense;
}

.approval-icon.seal {
  background: rgba(251, 191, 36, 0.15);
  color: $color-seal;
}

.approval-icon.procurement {
  background: rgba(252, 165, 165, 0.15);
  color: $color-procurement;
}

.approval-info {
  flex: 1;
  min-width: 0;
}

.approval-title {
  font-weight: 500;
  margin-bottom: 0.25rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: $text-primary;
}

.approval-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: $text-secondary;
}

.separator {
  opacity: 0.5;
}

.approval-amount {
  font-weight: 600;
  padding: 0.25rem 0.75rem;
  border-radius: 16px;
  font-size: 0.875rem;
}

.approval-amount.expense {
  background: rgba($color-expense, 0.15);
  color: $color-expense;
}

.approval-amount.seal {
  background: rgba($color-seal, 0.15);
  color: $color-seal;
}

.approval-amount.procurement {
  background: rgba($color-procurement, 0.15);
  color: $color-procurement-deep;
}

.approval-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-approve,
.btn-reject {
  padding: 0.5rem 1rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.btn-approve {
  background: rgba($color-expense, 0.15);
  border: none;
  color: $color-expense;
}

.btn-approve:hover {
  background: $color-expense;
  color: $white;
  transform: translateY(-2px);
  box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
}

.btn-reject {
  background: transparent;
  border: 1px solid $divider;
  color: $text-secondary;
}

.btn-reject:hover {
  border-color: $color-procurement;
  color: $color-procurement-deep;
  background: rgba($color-procurement, 0.15);
}

/* Quick Actions */
.quick-actions {
  background: $bg-card;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0.75rem;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.5rem 1rem;
  background: $bg-primary;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
}

.action-icon {
  font-size: 1.5rem;
  color: $color-primary;
}

.action-label {
  font-size: 0.75rem;
  color: $text-secondary;
}

/* Activity Section */
.activity-section {
  grid-column: 1 / -1;
  background: $bg-card;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
}

.timeline {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 1rem;
}

.timeline-item {
  display: flex;
  gap: 0.75rem;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 0.5rem;
  flex-shrink: 0;
}

.timeline-dot.submit { background: $color-seal; }
.timeline-dot.approve { background: $color-expense; }
.timeline-dot.reject { background: $color-procurement; }
.timeline-dot.create { background: $color-primary; }
.timeline-dot.complete { background: $color-expense; }

.timeline-content {
  flex: 1;
}

.event-title {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
  color: $text-primary;
}

.event-time {
  font-size: 0.75rem;
  color: $text-secondary;
}

/* Footer */
.dashboard-footer {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: $bg-card;
  border-top: 1px solid $divider;
  margin-top: 1rem;
}

.system-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.75rem;
  color: $text-secondary;
}

.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-indicator.online {
  background: $color-expense;
}

.footer-info {
  font-size: 0.75rem;
  color: $text-secondary;
}

// Reduced motion
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>