<template>
  <div class="check-wrapper">
    <div class="check-container">
      <div class="check-card">
        <!-- 状态指示器 -->
        <div class="status-indicator">
          <span class="pulse dingtalk"></span>
          <span class="label">请在钉钉App中打卡</span>
          <span class="sync-badge" v-if="isSyncing">同步中...</span>
        </div>

        <!-- 时间显示 -->
        <div class="time-display">
          <span class="time">{{ currentTime }}</span>
          <span class="seconds">{{ currentSeconds }}</span>
        </div>
        <div class="date">{{ dateWithWeekday }}</div>

        <!-- 打卡按钮 - 已注释，保留备用模式 -->
        <!--
        <div class="action-area">
          <button class="cyber-btn" @click="handleCheck" :class="checkType" :disabled="isLoading">
            <span class="btn-text">{{ checkType === 'SIGN_IN' ? '签到' : '签退' }}</span>
            <span class="btn-glow"></span>
          </button>
        </div>
        -->

        <!-- 备用打卡提示 -->
        <div class="backup-tip">
          <el-icon><Warning /></el-icon>
          <span>本地打卡已停用，请使用钉钉App打卡</span>
        </div>

        <!-- 打卡记录 -->
        <div class="record-info">
          <div class="record-item">
            <span class="record-label">今日签到</span>
            <span class="record-value" :class="{ 'no-data': !signInTime }">
              {{ signInTime || '--:--' }}
            </span>
            <span class="record-source" v-if="signInSource">
              <el-icon><Connection /></el-icon>{{ signInSource }}
            </span>
          </div>
          <div class="record-divider"></div>
          <div class="record-item">
            <span class="record-label">今日签退</span>
            <span class="record-value" :class="{ 'no-data': !signOutTime }">
              {{ signOutTime || '--:--' }}
            </span>
            <span class="record-source" v-if="signOutSource">
              <el-icon><Connection /></el-icon>{{ signOutSource }}
            </span>
          </div>
        </div>

        <!-- 考勤统计 -->
        <div class="stats-section" v-if="stats">
          <h4 class="section-title">本月考勤</h4>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value">{{ stats.normalDays || 0 }}</span>
              <span class="stat-label">正常</span>
            </div>
            <div class="stat-item warning">
              <span class="stat-value">{{ stats.lateDays || 0 }}</span>
              <span class="stat-label">迟到</span>
            </div>
            <div class="stat-item warning">
              <span class="stat-value">{{ stats.earlyLeaveDays || 0 }}</span>
              <span class="stat-label">早退</span>
            </div>
            <div class="stat-item danger">
              <span class="stat-value">{{ stats.absentDays || 0 }}</span>
              <span class="stat-label">缺卡</span>
            </div>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <button class="action-btn" @click="syncFromDingtalk">
            <el-icon><Refresh /></el-icon>
            <span>同步钉钉数据</span>
          </button>
          <button class="action-btn" @click="viewHistory">
            <el-icon><List /></el-icon>
            <span>打卡历史</span>
          </button>
        </div>

        <!-- 备用打卡入口（已禁用） -->
        <!--
        <div class="backup-section">
          <button class="backup-btn" disabled>
            <el-icon><Clock /></el-icon>
            <span>备用打卡（已停用）</span>
          </button>
        </div>
        -->
      </div>

      <!-- 打卡历史弹窗 -->
      <div class="dialog-overlay" v-if="historyVisible" @click.self="historyVisible = false">
        <div class="dialog">
          <div class="dialog-header">
            <h3 class="dialog-title">打卡历史</h3>
            <button class="dialog-close" @click="historyVisible = false">×</button>
          </div>
          <div class="dialog-body">
            <div class="history-list">
              <div v-for="record in clockHistory" :key="record.id" class="history-item">
                <div class="history-date">{{ record.date }}</div>
                <div class="history-detail">
                  <span class="clock-in">上班: {{ record.clockIn || '--' }}</span>
                  <span class="clock-out">下班: {{ record.clockOut || '--' }}</span>
                </div>
                <div class="history-source" v-if="record.source">
                  <el-icon><Connection /></el-icon>
                </div>
              </div>
              <div v-if="clockHistory.length === 0" class="empty-text">暂无打卡记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * 考勤打卡页面
 *
 * 考勤数据来源：钉钉同步（主数据源）
 * 本地打卡功能已禁用，保留代码作为备用模式
 *
 * 如需启用本地打卡功能：
 * 1. 取消注释打卡按钮部分
 * 2. 取消注释 handleCheck 函数中的本地打卡逻辑
 * 3. 取消注释备用打卡入口
 */
import { ref, onMounted } from 'vue'
import { hrApi } from '@/api/hr'
import { ElMessage } from 'element-plus'
import { useTime } from '@/composables/useTime'
import { Connection, Refresh, List, Warning } from '@element-plus/icons-vue'

const { currentTime, dateWithWeekday, currentSeconds } = useTime()
const checkType = ref('SIGN_IN')
const signInTime = ref('')
const signOutTime = ref('')
const signInSource = ref('')
const signOutSource = ref('')
const isLoading = ref(false)
const isSyncing = ref(false)
const stats = ref(null)
const historyVisible = ref(false)
const clockHistory = ref([])

// 获取考勤统计数据
const loadStatistics = async () => {
  try {
    const month = new Date().toISOString().slice(0, 7)
    const res = await hrApi.getDingTalkStatistics(null, month)
    if (res.data) {
      stats.value = res.data
    }
  } catch (error) {
    console.error('加载考勤统计失败', error)
  }
}

// 同步钉钉数据
const syncFromDingtalk = async () => {
  isSyncing.value = true
  try {
    await hrApi.syncClockRecords(null, null, null)
    ElMessage.success('同步成功')
    await loadStatistics()
    await loadTodayRecords()
  } catch (error) {
    console.error('同步失败', error)
  } finally {
    isSyncing.value = false
  }
}

// 加载今日打卡记录
const loadTodayRecords = async () => {
  isLoading.value = true
  try {
    const today = new Date().toISOString().slice(0, 10)
    const res = await hrApi.getClockRecords(null, today, today)
    if (res.data && res.data.length > 0) {
      for (const record of res.data) {
        // 安全处理clockTime切片
        const clockTimeStr = record.clockTime || ''
        if (clockTimeStr.length >= 16) {
          const timePart = clockTimeStr.slice(11, 16)
          if (record.clockType === 'OnDuty') {
            signInTime.value = timePart
            signInSource.value = record.source === 'APP' ? '钉钉' : ''
          } else {
            signOutTime.value = timePart
            signOutSource.value = record.source === 'APP' ? '钉钉' : ''
          }
        }
      }
    }
  } catch (error) {
    console.error('加载打卡记录失败', error)
  } finally {
    isLoading.value = false
  }
}

// 查看历史
const viewHistory = async () => {
  historyVisible.value = true
  try {
    const month = new Date().toISOString().slice(0, 7)
    const res = await hrApi.getClockRecords(null, month + '-01', month + '-31')
    clockHistory.value = res.data?.data || res.data || []
  } catch (error) {
    console.error('加载历史失败', error)
    clockHistory.value = []
  }
}

/**
 * 本地打卡功能（已禁用，保留代码作为备用模式）
 * 如需启用，请取消注释以下代码
 */
// const handleCheck = async () => {
//   /*
//   isLoading.value = true
//   try {
//     await hrApi.check({
//       checkType: checkType.value,
//       location: '办公室'
//     })
//     ElMessage.success(checkType.value === 'SIGN_IN' ? '签到成功' : '签退成功')
//     if (checkType.value === 'SIGN_IN') {
//       signInTime.value = currentTime.value
//       signInSource.value = '本地'
//     } else {
//       signOutTime.value = currentTime.value
//       signOutSource.value = '本地'
//     }
//     checkType.value = checkType.value === 'SIGN_IN' ? 'SIGN_OUT' : 'SIGN_IN'
//     await loadStatistics()
//   } catch (error) {
//     console.error('打卡失败', error)
//   } finally {
//     isLoading.value = false
//   }
//   */
//   ElMessage.info('本地打卡已停用，请使用钉钉App打卡')
// }

onMounted(() => {
  loadTodayRecords()
  loadStatistics()
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

.check-wrapper { min-height: 100vh; background: $bg-primary; }
.check-container { display: flex; justify-content: center; align-items: center; min-height: calc(100vh - 120px); padding: 40px 20px; }
.check-card { width: 100%; max-width: 420px; padding: 48px 40px; background: $bg-card; border-radius: 16px; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05); }

.status-indicator {
  display: flex; align-items: center; justify-content: center; gap: 10px; margin-bottom: 32px;
  .pulse { width: 10px; height: 10px; border-radius: 50%; background: $text-secondary; }
  .pulse.dingtalk { background: $primary; animation: pulse 2s infinite; }
  .label { font-size: 14px; color: $text-secondary; letter-spacing: 1px; }
  .sync-badge { font-size: 12px; color: $primary; padding: 2px 8px; background: rgba($primary, 0.1); border-radius: 10px; }
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba($primary, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba($primary, 0); }
}

.time-display { text-align: center; margin-bottom: 8px; .time { font-family: 'SF Mono', 'Monaco', monospace; font-size: 64px; font-weight: 600; color: $text-primary; letter-spacing: -1px; } .seconds { font-family: 'SF Mono', 'Monaco', monospace; font-size: 22px; color: $primary; margin-left: 6px; } }
.date { text-align: center; font-size: 15px; color: $text-secondary; margin-bottom: 24px; }

// 备用打卡提示
.backup-tip {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  margin-bottom: 32px; padding: 12px 20px;
  background: rgba($primary, 0.08); border-radius: 8px;
  font-size: 13px; color: $primary;
}

// 打卡按钮样式（已注释）
// .action-area { display: flex; justify-content: center; margin-bottom: 40px; }
// .cyber-btn {
//   padding: 16px 56px; font-size: 16px; font-weight: 500; color: #ffffff; background: $primary; border: none; border-radius: 12px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); box-shadow: 0 4px 12px rgba($primary, 0.25); position: relative; overflow: hidden;
//   &:hover { background: #5a95f7; box-shadow: 0 6px 16px rgba($primary, 0.3); }
//   &:active { transform: translateY(1px); }
//   &:disabled { opacity: 0.7; cursor: not-allowed; }
//   &.sign-in { background: $success; box-shadow: 0 4px 12px rgba($success, 0.25); &:hover { background: #2eb385; } }
//   &.sign-out { background: $warning; box-shadow: 0 4px 12px rgba($warning, 0.25); &:hover { background: #e5ab1f; } }
// }

.record-info { display: flex; align-items: center; justify-content: center; gap: 40px; padding: 20px 24px; background: $bg-primary; border-radius: 12px; margin-bottom: 24px; }
.record-item { display: flex; flex-direction: column; align-items: center; gap: 6px; .record-label { font-size: 12px; color: $text-secondary; } .record-value { font-family: 'SF Mono', 'Monaco', monospace; font-size: 18px; color: $text-primary; &.no-data { color: $text-secondary; } } .record-source { font-size: 11px; color: $primary; display: flex; align-items: center; gap: 4px; } }
.record-divider { width: 1px; height: 36px; background: $border; }

.stats-section { margin-bottom: 24px; }
.section-title { font-size: 14px; font-weight: 600; color: $text-primary; margin: 0 0 12px; }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-item { display: flex; flex-direction: column; align-items: center; padding: 12px; background: $bg-primary; border-radius: 8px; .stat-value { font-size: 20px; font-weight: 600; color: $success; } .stat-label { font-size: 12px; color: $text-secondary; margin-top: 4px; } &.warning .stat-value { color: $warning; } &.danger .stat-value { color: $danger; } }

.action-buttons { display: flex; gap: 12px; margin-bottom: 24px; }
.action-btn { flex: 1; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px; font-size: 14px; color: $text-secondary; background: $bg-primary; border: 1px solid $border; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $primary; border-color: $primary; background: rgba($primary, 0.05); } }

// 备用打卡入口（已注释）
// .backup-section { margin-top: 16px; padding-top: 16px; border-top: 1px dashed $border; }
// .backup-btn { width: 100%; display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px; font-size: 13px; color: $text-secondary; background: transparent; border: 1px dashed $border; border-radius: 8px; cursor: not-allowed; opacity: 0.5; }

.dialog-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.35); display: flex; align-items: center; justify-content: center; z-index: 1000; backdrop-filter: blur(2px); }
.dialog { width: 100%; max-width: 480px; background: $bg-card; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 40px -10px rgba(0, 0, 0, 0.15); }
.dialog-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid $border; }
.dialog-title { margin: 0; font-size: 18px; font-weight: 500; color: $text-primary; }
.dialog-close { width: 32px; height: 32px; font-size: 20px; color: $text-secondary; background: transparent; border: none; border-radius: 8px; cursor: pointer; transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1); &:hover { color: $text-primary; background: $bg-primary; } }
.dialog-body { padding: 20px; max-height: 50vh; overflow-y: auto; }

.history-list { display: flex; flex-direction: column; gap: 12px; }
.history-item { display: flex; align-items: center; gap: 12px; padding: 12px; background: $bg-primary; border-radius: 8px; .history-date { font-size: 13px; font-weight: 500; color: $text-primary; min-width: 80px; } .history-detail { flex: 1; display: flex; gap: 16px; font-size: 13px; color: $text-secondary; .clock-in { color: $success; } .clock-out { color: $warning; } .history-source { color: $primary; } } }
.empty-text { text-align: center; color: $text-secondary; padding: 20px; }

// 响应式设计
@media (max-width: 768px) {
  .check-page { padding: 16px; }
  .clock-card { padding: 20px; }
  .time-display { font-size: 48px; }
  .date-display { font-size: 14px; }
  .location-text { font-size: 13px; }
  .action-btn { padding: 10px; font-size: 13px; }
  .dialog { max-width: 100%; margin: 16px; border-radius: 12px; }
  .dialog-body { padding: 16px; }
}

@media (max-width: 480px) {
  .check-page { padding: 12px; }
  .clock-card { padding: 16px; }
  .time-display { font-size: 40px; }
}
</style>