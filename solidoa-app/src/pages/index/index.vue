<template>
  <view class="index-container">
    <!-- 用户信息卡片 -->
    <view class="user-card">
      <view class="user-info">
        <image class="avatar" src="/static/logo.png" mode="aspectFill" />
        <view class="info">
          <text class="name">{{ userStore.userInfo?.realName || '用户' }}</text>
          <text class="dept">技术部</text>
        </view>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-entry">
      <view class="entry-item" @click="navigateTo('/pages/approval/list')">
        <view class="entry-icon" style="background: #409eff">
          <text class="icon">📋</text>
        </view>
        <text class="entry-text">审批</text>
        <view class="badge" v-if="pendingCount > 0">{{ pendingCount }}</view>
      </view>

      <view class="entry-item" @click="navigateTo('/pages/attendance/check')">
        <view class="entry-icon" style="background: #67c23a">
          <text class="icon">⏰</text>
        </view>
        <text class="entry-text">打卡</text>
      </view>

      <view class="entry-item" @click="navigateTo('/pages/message/index')">
        <view class="entry-icon" style="background: #e6a23c">
          <text class="icon">💬</text>
        </view>
        <text class="entry-text">消息</text>
        <view class="badge" v-if="messageCount > 0">{{ messageCount }}</view>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="stat-cards">
      <view class="stat-item">
        <text class="stat-value">{{ todayCheckIn ? '已签到' : '未签到' }}</text>
        <text class="stat-label">今日考勤</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ pendingCount }}</text>
        <text class="stat-label">待审批</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ messageCount }}</text>
        <text class="stat-label">未读消息</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { api } from '@/api/index'

const userStore = useUserStore()
const pendingCount = ref(0)
const messageCount = ref(0)
const todayCheckIn = ref(false)

const navigateTo = (url) => {
  uni.navigateTo({ url })
}

const loadData = async () => {
  try {
    // 加载待审批数量
    const tasksRes = await api.getMyTasks()
    pendingCount.value = tasksRes.data?.length || 0

    // 加载未读消息数
    const messageRes = await api.getUnreadCount()
    messageCount.value = messageRes.data || 0

    // 加载今日签到状态
    const checkInRes = await api.getTodayCheckIn()
    todayCheckIn.value = checkInRes.data?.hasCheckedIn || false
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(async () => {
  if (!userStore.token) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }

  // 校验 token 有效性
  try {
    await api.getUserInfo()
  } catch (e) {
    userStore.logout()
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }

  loadData()
})
</script>

<style scoped lang="scss">
.index-container {
  padding: 20rpx;
}

.user-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;

  .user-info {
    display: flex;
    align-items: center;

    .avatar {
      width: 100rpx;
      height: 100rpx;
      border-radius: 50rpx;
      margin-right: 20rpx;
    }

    .info {
      .name {
        display: block;
        font-size: 36rpx;
        color: #fff;
        font-weight: bold;
      }

      .dept {
        display: block;
        font-size: 24rpx;
        color: rgba(255, 255, 255, 0.8);
        margin-top: 8rpx;
      }
    }
  }
}

.quick-entry {
  display: flex;
  justify-content: space-around;
  background: #fff;
  border-radius: 16rpx;
  padding: 40rpx 20rpx;
  margin-bottom: 30rpx;

  .entry-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;

    .entry-icon {
      width: 100rpx;
      height: 100rpx;
      border-radius: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16rpx;

      .icon {
        font-size: 48rpx;
      }
    }

    .entry-text {
      font-size: 26rpx;
      color: #303133;
    }

    .badge {
      position: absolute;
      top: -10rpx;
      right: -10rpx;
      background: #f56c6c;
      color: #fff;
      font-size: 20rpx;
      padding: 4rpx 12rpx;
      border-radius: 20rpx;
    }
  }
}

.stat-cards {
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 30rpx 0;

  .stat-item {
    flex: 1;
    text-align: center;

    .stat-value {
      display: block;
      font-size: 32rpx;
      font-weight: bold;
      color: #303133;
      margin-bottom: 8rpx;
    }

    .stat-label {
      font-size: 24rpx;
      color: #909399;
    }
  }
}
</style>