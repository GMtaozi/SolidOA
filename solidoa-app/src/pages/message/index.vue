<template>
  <view class="message-container">
    <view v-if="messageList.length > 0" class="message-list">
      <view
        v-for="item in messageList"
        :key="item.id"
        class="message-item"
        :class="{ unread: isUnread(item) }"
        @click="handleRead(item)"
      >
        <view class="message-icon">
          <text>{{ getTypeIcon(item.type) }}</text>
        </view>
        <view class="message-content">
          <view class="message-header">
            <text class="title">{{ item.title }}</text>
            <text class="time">{{ formatTime(item.createTime) }}</text>
          </view>
          <text class="content" v-html="escapeHtml(item.content)"></text>
        </view>
      </view>
    </view>

    <view v-else class="empty">
      <text>暂无消息</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '@/api/index'

const messageList = ref([])

const getTypeIcon = (type) => {
  const map = {
    APPROVAL: '📋',
    SYSTEM: '⚙️',
    REMINDER: '🔔',
    NOTICE: '📢'
  }
  return map[type] || '💬'
}

const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleDateString('zh-CN')
}

// 判断消息是否未读（兼容多种字段名）
const isUnread = (item) => {
  return item.read === false || item.isRead === false || item.unread === true
}

// HTML转义防止XSS攻击（纯JS实现，兼容uni-app所有环境）
const escapeHtml = (str) => {
  if (!str) return ''
  return str.replace(/[&<>"']/g, (c) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  }[c]))
}

const loadData = async () => {
  try {
    const res = await api.getMessageList({})
    messageList.value = res.data?.records || []
  } catch (e) {
    console.error('加载消息失败', e)
  }
}

const handleRead = (item) => {
  console.log('查看消息', item)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.message-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.message-list {
  .message-item {
    display: flex;
    background: #fff;
    padding: 30rpx;
    margin-bottom: 2rpx;

    &.unread {
      background: #f0f9ff;

      .title {
        font-weight: bold;
      }
    }

    .message-icon {
      width: 80rpx;
      height: 80rpx;
      background: #f0f0f0;
      border-radius: 16rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 40rpx;
      margin-right: 20rpx;
      flex-shrink: 0;
    }

    .message-content {
      flex: 1;

      .message-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 12rpx;

        .title {
          font-size: 28rpx;
          color: #303133;
        }

        .time {
          font-size: 24rpx;
          color: #909399;
        }
      }

      .content {
        font-size: 26rpx;
        color: #606266;
        line-height: 1.5;
      }
    }
  }
}

.empty {
  text-align: center;
  padding: 200rpx 0;
  color: #909399;
  font-size: 28rpx;
}
</style>