<template>
  <div class="message-container">
    <div class="message-wrapper">
      <!-- 消息列表 -->
      <div class="message-list-panel cyber-card">
        <div class="panel-header">
          <span class="panel-icon">◈</span>
          <span>消息中心</span>
          <span class="status-indicator">
            <span class="pulse-dot"></span>
            <span class="status-text">{{ messages.length }} 条消息</span>
          </span>
        </div>

        <div class="message-list">
          <div
            v-for="(msg, index) in messages"
            :key="msg.id"
            class="message-item"
            :class="{ unread: !msg.read, expanded: expandedId === msg.id }"
            @click="handleExpand(msg.id)"
          >
            <div class="timeline-marker">
              <div class="marker-dot" :class="getMessageType(msg.type)"></div>
              <div v-if="index < messages.length - 1" class="marker-line"></div>
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-type-tag" :class="getMessageType(msg.type)">
                  {{ getTypeLabel(msg.type) }}
                </span>
                <span class="message-time">{{ msg.time }}</span>
              </div>
              <div class="message-title">{{ msg.title }}</div>
              <div class="message-sender">
                <span class="sender-avatar" :style="{ background: getAvatarGradient(msg.sender) }">
                  {{ msg.sender?.charAt(0) || '?' }}
                </span>
                <span class="sender-name">{{ msg.sender }}</span>
              </div>
              <transition name="expand">
                <div v-if="expandedId === msg.id" class="message-body">
                  <p v-html="escapeHtml(msg.content)"></p>
                </div>
              </transition>
            </div>
          </div>

          <div v-if="messages.length === 0" class="empty-state">
            <div class="empty-icon">◇</div>
            <div class="empty-text">暂无消息</div>
          </div>
        </div>
      </div>

      <!-- 消息详情 -->
      <div class="message-detail-panel cyber-card">
        <div class="panel-header">
          <span class="panel-icon">◆</span>
          <span>消息详情</span>
        </div>
        <div class="detail-content">
          <div v-if="selectedMessage" class="detail-view">
            <div class="detail-type">
              <span class="type-badge" :class="getMessageType(selectedMessage.type)">
                {{ getTypeLabel(selectedMessage.type) }}
              </span>
            </div>
            <h2 class="detail-title">{{ selectedMessage.title }}</h2>
            <div class="detail-meta">
              <span class="meta-item">
                <span class="meta-icon">◈</span>
                {{ selectedMessage.sender }}
              </span>
              <span class="meta-item">
                <span class="meta-icon">◇</span>
                {{ selectedMessage.time }}
              </span>
            </div>
            <div class="detail-divider"></div>
            <div class="detail-body">
              <p v-html="escapeHtml(selectedMessage.content)"></p>
            </div>
            <div class="detail-actions">
              <button class="cyber-btn" @click="handleReply">
                <span class="btn-text">回复</span>
              </button>
              <button class="cyber-btn secondary" @click="handleMarkRead">
                <span class="btn-text">标记已读</span>
              </button>
            </div>
          </div>
          <div v-else class="empty-detail">
            <div class="empty-icon">◌</div>
            <div class="empty-text">选择一条消息查看详情</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/system'

const expandedId = ref(null)
const selectedMessageId = ref(null)
const replyLoading = ref(false)

const messages = ref([
  { id: 1, type: 'system', title: '系统通知：服务器更新完成', sender: '系统管理员', time: '10:30', content: '尊敬的用户，服务器已完成例行更新维护，各项服务已恢复正常。感谢您的耐心等待。', read: false },
  { id: 2, type: 'approval', title: '请假申请已通过', sender: '张经理', time: '09:15', content: '您提交的请假申请已审批通过，审批意见：同意，请注意安全。', read: false },
  { id: 3, type: 'task', title: '新任务指派：项目评审', sender: '李总监', time: '昨天', content: '您有一个新的项目评审任务需要在本周五前完成，请及时处理。', read: true },
  { id: 4, type: 'meeting', title: '会议提醒：团队周会', sender: '行政部', time: '昨天', content: '提醒您明天下午2点在会议室A有团队周会，请准时参加。', read: true }
])

const selectedMessage = computed(() => {
  return messages.value.find(m => m.id === selectedMessageId.value)
})

const getMessageType = (type) => {
  const map = {
    system: 'type-system',
    approval: 'type-approval',
    task: 'type-task',
    meeting: 'type-meeting'
  }
  return map[type] || 'type-system'
}

const getTypeLabel = (type) => {
  const map = {
    system: '系统',
    approval: '审批',
    task: '任务',
    meeting: '会议'
  }
  return map[type] || '通知'
}

const getAvatarGradient = (name) => {
  const colors = [
    'linear-gradient(135deg, #60A5FA 0%, #A78BFA 100%)',
    'linear-gradient(135deg, #A78BFA 0%, #60A5FA 100%)',
    'linear-gradient(135deg, #34D399 0%, #60A5FA 100%)'
  ]
  const index = name ? name.charCodeAt(0) % colors.length : 0
  return colors[index]
}

// HTML转义防止XSS攻击
const escapeHtml = (str) => {
  if (!str) return ''
  const div = document.createElement('div')
  div.textContent = str
  return div.innerHTML
}

const handleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
  if (!selectedMessageId.value) {
    selectedMessageId.value = id
  }
}

const handleReply = async () => {
  if (!selectedMessageId.value) {
    ElMessage.warning('请先选择要回复的消息')
    return
  }

  try {
    const msg = selectedMessage.value
    const { value: replyContent } = await ElMessageBox.prompt('请输入回复内容', `回复: ${msg.title}`, {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      inputType: 'textarea'
    })

    if (!replyContent || !replyContent.trim()) {
      ElMessage.warning('回复内容不能为空')
      return
    }

    replyLoading.value = true
    await systemApi.replyMessage(selectedMessageId.value, { content: replyContent.trim() })
    ElMessage.success('回复发送成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('回复消息失败:', error)
      ElMessage.error('回复发送失败，请稍后重试')
    }
  } finally {
    replyLoading.value = false
  }
}

const handleMarkRead = async () => {
  if (selectedMessageId.value) {
    const msg = messages.value.find(m => m.id === selectedMessageId.value)
    if (msg) {
      msg.read = true
      // 同步到后端持久化已读状态
      try {
        await systemApi.markMessageRead(selectedMessageId.value)
      } catch (error) {
        console.error('标记已读失败', error)
      }
    }
  }
}
</script>

<style scoped lang="scss">
// 柔和舒适风格配色变量
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.message-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;

  .message-wrapper {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
  }

  .cyber-card {
    background: $bg-card;
    border-radius: 16px;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
    transition: box-shadow 0.3s ease;
    overflow: hidden;

    &:hover {
      box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
    }
  }

  .panel-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 16px 20px;
    border-bottom: 1px solid $border-color;
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;

    .panel-icon {
      color: $primary;
      font-size: 18px;
    }

    .status-indicator {
      margin-left: auto;
      display: flex;
      align-items: center;
      gap: 6px;

      .pulse-dot {
        width: 8px;
        height: 8px;
        background: $success;
        border-radius: 50%;
        animation: pulse 2s ease-in-out infinite;
      }

      .status-text {
        font-size: 12px;
        color: $text-secondary;
        font-weight: normal;
      }
    }
  }

  @keyframes pulse {
    0%, 100% { transform: scale(1); opacity: 1; }
    50% { transform: scale(1.2); opacity: 0.7; }
  }

  // 消息列表
  .message-list-panel {
    max-height: calc(100vh - 40px);
    overflow: hidden;
    display: flex;
    flex-direction: column;

    .message-list {
      flex: 1;
      overflow-y: auto;
      padding: 16px;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: $bg-primary;
        border-radius: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background: $border-color;
        border-radius: 3px;
      }
    }

    .message-item {
      display: flex;
      gap: 12px;
      padding: 12px;
      margin-bottom: 12px;
      background: $bg-primary;
      border-radius: 16px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

      &:hover {
        background: #fcfaf7;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      }

      &.unread {
        
      }

      .timeline-marker {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding-top: 4px;

        .marker-dot {
          width: 12px;
          height: 12px;
          border-radius: 50%;
          background: $primary;
          flex-shrink: 0;

          &.type-system { background: $warning; }
          &.type-approval { background: $success; }
          &.type-task { background: #a78bfa; }
          &.type-meeting { background: #f87171; }
        }

        .marker-line {
          width: 2px;
          flex: 1;
          background: $border-color;
          margin-top: 8px;
        }
      }

      .message-content {
        flex: 1;
        min-width: 0;

        .message-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 6px;

          .message-type-tag {
            font-size: 10px;
            padding: 2px 8px;
            border-radius: 12px;
            background: rgba(96, 165, 250, 0.1);
            color: $primary;

            &.type-system { background: rgba(251, 191, 36, 0.1); color: $warning; }
            &.type-approval { background: rgba(52, 211, 153, 0.1); color: $success; }
            &.type-task { background: rgba(167, 139, 250, 0.1); color: #a78bfa; }
            &.type-meeting { background: rgba(248, 113, 113, 0.1); color: #f87171; }
          }

          .message-time {
            font-size: 11px;
            color: $text-secondary;
          }
        }

        .message-title {
          font-size: 14px;
          font-weight: 500;
          color: $text-primary;
          margin-bottom: 8px;
        }

        .message-sender {
          display: flex;
          align-items: center;
          gap: 6px;

          .sender-avatar {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 10px;
            color: #ffffff;
            background: linear-gradient(135deg, $primary, lighten($primary, 15%));
          }

          .sender-name {
            font-size: 12px;
            color: $text-secondary;
          }
        }

        .message-body {
          margin-top: 10px;
          padding: 10px;
          background: $bg-card;
          border-radius: 12px;
          font-size: 12px;
          color: $text-secondary;
          line-height: 1.6;
        }
      }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      color: $text-secondary;

      .empty-icon {
        font-size: 48px;
        color: $border-color;
        margin-bottom: 16px;
      }

      .empty-text {
        font-size: 14px;
      }
    }
  }

  // 消息详情
  .message-detail-panel {
    .detail-content {
      padding: 20px;
    }

    .detail-view {
      .detail-type {
        margin-bottom: 12px;

        .type-badge {
          font-size: 11px;
          padding: 4px 12px;
          border-radius: 12px;
          background: rgba(96, 165, 250, 0.1);
          color: $primary;

          &.type-system { background: rgba(251, 191, 36, 0.1); color: $warning; }
          &.type-approval { background: rgba(52, 211, 153, 0.1); color: $success; }
          &.type-task { background: rgba(167, 139, 250, 0.1); color: #a78bfa; }
          &.type-meeting { background: rgba(248, 113, 113, 0.1); color: #f87171; }
        }
      }

      .detail-title {
        font-size: 20px;
        font-weight: 600;
        color: $text-primary;
        margin: 0 0 16px 0;
      }

      .detail-meta {
        display: flex;
        gap: 20px;
        margin-bottom: 20px;

        .meta-item {
          display: flex;
          align-items: center;
          gap: 6px;
          font-size: 13px;
          color: $text-secondary;

          .meta-icon {
            color: $primary;
          }
        }
      }

      .detail-divider {
        height: 1px;
        background: $border-color;
        margin-bottom: 20px;
      }

      .detail-body {
        font-size: 14px;
        color: $text-secondary;
        line-height: 1.8;
      }

      .detail-actions {
        display: flex;
        gap: 12px;
        margin-top: 24px;

        .cyber-btn {
          padding: 10px 20px;
          background: $bg-card;
          border: 1px solid $border-color;
          border-radius: 40px;
          color: $text-primary;
          font-size: 13px;
          cursor: pointer;
          transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
          box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

          &:hover {
            border-color: $primary;
            color: $primary;
            box-shadow: 0 4px 12px rgba(96, 165, 250, 0.15);
          }

          &.secondary {
            background: $primary;
            border-color: $primary;
            color: #ffffff;

            &:hover {
              background: darken($primary, 5%);
              border-color: darken($primary, 5%);
              box-shadow: 0 6px 16px rgba(96, 165, 250, 0.3);
            }
          }
        }
      }
    }

    .empty-detail {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 80px 20px;
      color: $text-secondary;

      .empty-icon {
        font-size: 64px;
        color: $border-color;
        margin-bottom: 16px;
      }

      .empty-text {
        font-size: 14px;
      }
    }
  }

  // 展开动画
  .expand-enter-active,
  .expand-leave-active {
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
    overflow: hidden;
  }

  .expand-enter-from,
  .expand-leave-to {
    opacity: 0;
    max-height: 0;
    margin-top: 0;
  }

  .expand-enter-to,
  .expand-leave-from {
    opacity: 1;
    max-height: 200px;
  }
}
</style>