<template>
  <view class="check-container">
    <!-- 打卡卡片 -->
    <view class="check-card">
      <view class="time">{{ currentTime }}</view>
      <view class="date">{{ currentDate }}</view>

      <view class="location">
        <text class="icon">📍</text>
        <text class="text">{{ location || '正在获取位置...' }}</text>
      </view>

      <button
        class="check-btn"
        :class="checkType"
        @click="handleCheck"
        :disabled="checking"
      >
        {{ checking ? '打卡中...' : (checkType === 'SIGN_IN' ? '签 到' : '签 退') }}
      </button>
    </view>

    <!-- 今日记录 -->
    <view class="records-card">
      <view class="card-title">今日打卡记录</view>
      <view v-if="todayRecords.length > 0" class="record-list">
        <view v-for="record in todayRecords" :key="record.id" class="record-item">
          <view class="record-left">
            <text class="type">{{ record.checkType === 'SIGN_IN' ? '签到' : '签退' }}</text>
            <text class="time">{{ formatTime(record.checkTime) }}</text>
          </view>
          <view class="record-right">
            <text class="status" :class="record.isLate ? 'late' : ''">
              {{ record.isLate ? '迟到' : (record.isEarlyLeave ? '早退' : '正常') }}
            </text>
          </view>
        </view>
      </view>
      <view v-else class="no-record">今日暂无打卡记录</view>
    </view>

    <!-- 月度汇总 -->
    <view class="summary-card">
      <view class="card-title">本月考勤</view>
      <view class="summary-stats">
        <view class="stat-item">
          <text class="value">{{ summary.workDays || 0 }}</text>
          <text class="label">应到天数</text>
        </view>
        <view class="stat-item">
          <text class="value">{{ summary.checkDays || 0 }}</text>
          <text class="label">实到天数</text>
        </view>
        <view class="stat-item">
          <text class="value">{{ summary.lateDays || 0 }}</text>
          <text class="label">迟到次数</text>
        </view>
        <view class="stat-item">
          <text class="value">{{ summary.leaveDays || 0 }}</text>
          <text class="label">请假天数</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { api } from '@/api/index'

const currentTime = ref('')
const currentDate = ref('')
const location = ref('')
const checkType = ref('SIGN_IN')
const checking = ref(false)
const todayRecords = ref([])
const summary = ref({})

let timer = null

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  return new Date(timeStr).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', {
    weekday: 'long',
    month: 'long',
    day: 'numeric'
  })
}

const getLocation = () => {
  uni.getLocation({
    type: 'gcj02',
    success: (res) => {
      location.value = `位置: ${res.latitude.toFixed(4)}, ${res.longitude.toFixed(4)}`
    },
    fail: () => {
      location.value = '办公室'
    }
  })
}

const handleCheck = async () => {
  checking.value = true
  try {
    await api.check({
      checkType: checkType.value,
      location: location.value
    })
    uni.showToast({ title: checkType.value === 'SIGN_IN' ? '签到成功' : '签退成功', icon: 'success' })
    checkType.value = checkType.value === 'SIGN_IN' ? 'SIGN_OUT' : 'SIGN_IN'
    loadTodayRecords()
  } catch (e) {
    console.error('打卡失败', e)
  } finally {
    checking.value = false
  }
}

const loadTodayRecords = async () => {
  try {
    const today = new Date().toISOString().split('T')[0]
    const res = await api.getAttendanceRecords({ checkDate: today })
    todayRecords.value = res.data?.records || []
  } catch (e) {
    console.error('加载记录失败', e)
  }
}

const loadSummary = async () => {
  try {
    const now = new Date()
    const yearMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
    const res = await api.getAttendanceSummary({ yearMonth })
    summary.value = res.data || {}
  } catch (e) {
    console.error('加载汇总失败', e)
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  getLocation()
  loadTodayRecords()
  loadSummary()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.check-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 20rpx;
}

.check-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 24rpx;
  padding: 50rpx 40rpx;
  text-align: center;
  margin-bottom: 20rpx;

  .time {
    font-size: 80rpx;
    font-weight: bold;
    color: #fff;
  }

  .date {
    font-size: 28rpx;
    color: rgba(255, 255, 255, 0.8);
    margin: 16rpx 0 30rpx;
  }

  .location {
    margin-bottom: 40rpx;

    .icon {
      margin-right: 8rpx;
    }

    .text {
      font-size: 26rpx;
      color: rgba(255, 255, 255, 0.9);
    }
  }

  .check-btn {
    width: 280rpx;
    height: 280rpx;
    border-radius: 140rpx;
    font-size: 40rpx;
    font-weight: bold;
    color: #fff;
    margin: 0 auto;

    &.SIGN_IN {
      background: rgba(255, 255, 255, 0.2);
      border: 4rpx solid #fff;
    }

    &.SIGN_OUT {
      background: #67c23a;
    }

    &::after {
      border: none;
    }
  }
}

.records-card, .summary-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 20rpx;

  .card-title {
    font-size: 32rpx;
    font-weight: bold;
    color: #303133;
    margin-bottom: 20rpx;
  }

  .record-list {
    .record-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20rpx 0;
      border-bottom: 1rpx solid #f0f0f0;

      &:last-child {
        border-bottom: none;
      }

      .record-left {
        .type {
          display: block;
          font-size: 28rpx;
          color: #303133;
          margin-bottom: 8rpx;
        }

        .time {
          font-size: 24rpx;
          color: #909399;
        }
      }

      .record-right {
        .status {
          font-size: 24rpx;
          color: #67c23a;

          &.late {
            color: #f56c6c;
          }
        }
      }
    }
  }

  .no-record {
    text-align: center;
    color: #909399;
    padding: 40rpx 0;
  }
}

.summary-stats {
  display: flex;
  justify-content: space-between;

  .stat-item {
    text-align: center;

    .value {
      display: block;
      font-size: 40rpx;
      font-weight: bold;
      color: #409eff;
    }

    .label {
      display: block;
      font-size: 24rpx;
      color: #909399;
      margin-top: 8rpx;
    }
  }
}
</style>