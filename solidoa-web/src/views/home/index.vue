<template>
  <div class="home-container">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card blue" style="animation-delay: 0.1s">
        <div class="stat-icon"><el-icon><DataLine /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber(stats.totalApprovals) }}</div>
          <div class="stat-label">总审批数</div>
        </div>
      </div>
      <div class="stat-card green" style="animation-delay: 0.15s">
        <div class="stat-icon"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber(stats.completed) }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card yellow" style="animation-delay: 0.2s">
        <div class="stat-icon"><el-icon><Clock /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ formatNumber(stats.pending) }}</div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
      <div class="stat-card red" style="animation-delay: 0.25s">
        <div class="stat-icon"><el-icon><Money /></el-icon></div>
        <div class="stat-content">
          <div class="stat-value">{{ formatMoney(stats.expenseAmount) }}</div>
          <div class="stat-label">报销金额</div>
        </div>
      </div>
    </div>

    <!-- 内容网格 -->
    <div class="content-grid">
      <!-- 快捷操作 -->
      <div class="content-card" style="animation-delay: 0.3s">
        <div class="card-header">
          <div class="card-title">
            <el-icon><Grid /></el-icon>
            快捷操作
          </div>
          <span class="card-line"></span>
        </div>
        <div class="card-body">
          <div class="action-grid">
            <div class="action-btn blue" @click="navigateTo('/expense')">
              <span class="action-icon"><el-icon><Money /></el-icon></span>
              <span class="action-label">提交报销</span>
            </div>
            <div class="action-btn green" @click="navigateTo('/leave')">
              <span class="action-icon"><el-icon><Calendar /></el-icon></span>
              <span class="action-label">请假申请</span>
            </div>
            <div class="action-btn yellow" @click="navigateTo('/check')">
              <span class="action-icon"><el-icon><Clock /></el-icon></span>
              <span class="action-label">考勤打卡</span>
            </div>
            <div class="action-btn purple" @click="navigateTo('/approval')">
              <span class="action-icon"><el-icon><Document /></el-icon></span>
              <span class="action-label">审批管理</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 最新消息 -->
      <div class="content-card" style="animation-delay: 0.35s">
        <div class="card-header">
          <div class="card-title">
            <el-icon><Bell /></el-icon>
            最新消息
          </div>
          <span class="card-line"></span>
        </div>
        <div class="card-body">
          <div v-if="messageLoading" class="loading-state">
            <span class="el-icon el-icon-loading"></span>
            <span>加载中...</span>
          </div>
          <div v-else-if="messages.length === 0" class="empty-state">
            <span class="el-icon el-icon-document"></span>
            <span>暂无消息</span>
          </div>
          <div v-else class="messages-list">
            <div v-for="(msg, index) in messages" :key="index" class="message-item">
              <span class="message-icon" :class="msg.type || 'info'">
                <el-icon><component :is="getMessageIcon(msg.type)" /></el-icon>
              </span>
              <div class="message-content">
                <div class="message-title">{{ msg.title }}</div>
                <div class="message-desc">{{ msg.desc }}</div>
              </div>
              <span class="message-time">{{ msg.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部统计 -->
    <div class="footer-stats" style="animation-delay: 0.4s">
      <div class="footer-stat">
        <div class="footer-stat-value">{{ footerStats.attendanceRate || 'N/A' }}</div>
        <div class="footer-stat-label">考勤达标率</div>
      </div>
      <div class="stat-divider"></div>
      <div class="footer-stat">
        <div class="footer-stat-value">{{ footerStats.leaveDays || 0 }}</div>
        <div class="footer-stat-label">本月请假天数</div>
      </div>
      <div class="stat-divider"></div>
      <div class="footer-stat">
        <div class="footer-stat-value">{{ formatMoney(footerStats.yearlyExpense) }}</div>
        <div class="footer-stat-label">年度报销总额</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Grid, Bell, DataLine, CircleCheck, Clock, Money, Calendar, Document, Select, WarningFilled, InfoFilled } from '@element-plus/icons-vue'
import { systemApi } from '@/api/system'

const router = useRouter()

// 统计数据
const stats = reactive({
  totalApprovals: 0,
  completed: 0,
  pending: 0,
  expenseAmount: 0
})

// 底部统计数据
const footerStats = reactive({
  attendanceRate: 'N/A',
  leaveDays: 0,
  yearlyExpense: 0
})

// 消息列表
const messages = ref([])
const messageLoading = ref(false)

// 格式化数字（千分位）
const formatNumber = (num) => {
  if (num === null || num === undefined) return '0'
  return Number(num).toLocaleString()
}

// 格式化金额
const formatMoney = (amount) => {
  if (!amount && amount !== 0) return '¥0'
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  if (isNaN(num)) return '¥0'
  return '¥' + num.toLocaleString()
}

// 获取消息图标
const getMessageIcon = (type) => {
  switch (type) {
    case 'success': return Select
    case 'warning': return WarningFilled
    default: return InfoFilled
  }
}

// 跳转到指定页面
const navigateTo = (path) => {
  router.push(path)
}

// 加载首页数据
const loadDashboardData = async () => {
  try {
    // 并行加载统计数据和底部统计
    const [statsRes, footerRes, messagesRes] = await Promise.allSettled([
      systemApi.getDashboardStats(),
      systemApi.getFooterStats(),
      systemApi.getDashboardMessages()
    ])

    // 处理统计数据
    if (statsRes.status === 'fulfilled' && statsRes.value?.data) {
      const data = statsRes.value.data
      stats.totalApprovals = data.totalApprovals || 0
      stats.completed = data.completed || 0
      stats.pending = data.pending || 0
      stats.expenseAmount = data.expenseAmount || 0
    }

    // 处理底部统计
    if (footerRes.status === 'fulfilled' && footerRes.value?.data) {
      const data = footerRes.value.data
      footerStats.attendanceRate = data.attendanceRate || 'N/A'
      footerStats.leaveDays = data.leaveDays || 0
      footerStats.yearlyExpense = data.yearlyExpense || 0
    }

    // 处理消息列表
    if (messagesRes.status === 'fulfilled' && messagesRes.value?.data) {
      const data = messagesRes.value.data
      messages.value = data.list || []
    }
  } catch (error) {
    console.error('加载首页数据失败:', error)
  }
}

onMounted(() => {
  loadDashboardData()
})
</script>

<style scoped lang="scss">
// 变量 - Soft & Comfortable 主题
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

// 辅助色
$color-expense: #34D399;
$color-seal: #FBBF24;
$color-purchase: #FCA5A5;

// 阴影
$shadow-base: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
$shadow-hover: 0 20px 35px -12px rgba(0, 0, 0, 0.1);

// 圆角
$radius-lg: 16px;
$radius-md: 12px;
$radius-sm: 8px;

.home-container {
  position: relative;
  min-height: calc(100vh - 128px);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  animation: fadeIn 0.5s ease-out;
  line-height: 1.6;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

// 统计卡片网格
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  position: relative;
  padding: 24px;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-base;
  overflow: hidden;
  animation: fadeInUp 0.5s ease-out backwards;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-hover;

    .stat-icon {
      transform: scale(1.05);
    }
  }

  &.blue { --card-color: #3b82f6; }
  &.green { --card-color: #34D399; }
  &.yellow { --card-color: #FBBF24; }
  &.red { --card-color: #FCA5A5; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stat-icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-md;
  background: color-mix(in srgb, var(--card-color) 10%, transparent);
  color: var(--card-color);
  font-size: 24px;
  margin-bottom: 16px;
  transition: transform 0.25s ease;
}

.stat-content {
  position: relative;
  z-index: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  color: var(--card-color);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: $text-secondary;
}

// 内容网格
.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.content-card {
  background: $bg-card;
  border-radius: $radius-lg;
  animation: fadeInUp 0.5s ease-out backwards;
  overflow: hidden;
  box-shadow: $shadow-base;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    box-shadow: $shadow-hover;
  }
}

.card-header {
  padding: 20px 24px;
  border-bottom: 1px solid $border-color;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 500;
  color: $text-primary;

  .el-icon {
    font-size: 18px;
    color: $primary;
  }
}

.card-line {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, $primary, transparent);
  border-radius: 1px;
}

.card-body {
  padding: 24px;
}

// 快捷操作
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-btn {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  background: $bg-primary;
  border-radius: $radius-md;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  animation: fadeInUp 0.4s ease-out backwards;

  &:hover {
    background: rgba($primary, 0.05);
    box-shadow: $shadow-base;

    .action-icon {
      transform: scale(1.05);
    }
  }

  &.blue { --btn-color: #3b82f6; }
  &.green { --btn-color: #34D399; }
  &.yellow { --btn-color: #FBBF24; }
  &.purple { --btn-color: #8b5cf6; }
}

.action-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-md;
  background: color-mix(in srgb, var(--btn-color) 10%, transparent);
  color: var(--btn-color);
  font-size: 22px;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
}

.action-label {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
}

// 消息列表
.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: $bg-primary;
  border-radius: $radius-md;
  animation: fadeInUp 0.4s ease-out backwards;
  transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    box-shadow: $shadow-base;
  }
}

.message-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-sm;
  font-size: 18px;
  flex-shrink: 0;

  &.success {
    background: rgba($success, 0.15);
    color: $success;
  }

  &.info {
    background: rgba($primary, 0.1);
    color: $primary;
  }

  &.warning {
    background: rgba($color-seal, 0.15);
    color: $color-seal;
  }
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-title {
  font-size: 14px;
  font-weight: 500;
  color: $text-primary;
  margin-bottom: 4px;
}

.message-desc {
  font-size: 12px;
  color: $text-secondary;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-time {
  font-size: 12px;
  color: $text-secondary;
  flex-shrink: 0;
}

// 底部统计
.footer-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  padding: 24px;
  background: $bg-card;
  border-radius: $radius-lg;
  box-shadow: $shadow-base;
  animation: fadeInUp 0.5s ease-out backwards;
}

.footer-stat {
  text-align: center;
}

.footer-stat-value {
  font-size: 28px;
  font-weight: 600;
  color: $primary;
  margin-bottom: 4px;
}

.footer-stat-label {
  font-size: 13px;
  color: $text-secondary;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: $border-color;
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