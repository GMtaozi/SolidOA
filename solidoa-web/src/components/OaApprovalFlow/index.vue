<template>
  <div class="oa-approval-flow">
    <!-- 水平节点连线 -->
    <div class="oa-approval-flow__nodes">
      <div
        v-for="(node, i) in nodes"
        :key="node.id || i"
        class="oa-flow-node"
        :class="['status-' + (node.status || 'PENDING').toLowerCase(), {
          'is-current': currentNodeOrder !== undefined && node.order === currentNodeOrder
        }]"
      >
        <div class="oa-flow-node__circle">
          <el-icon v-if="node.status === 'APPROVED'"><Check /></el-icon>
          <el-icon v-else-if="node.status === 'REJECTED'"><Close /></el-icon>
          <span v-else>{{ node.order }}</span>
        </div>
        <div class="oa-flow-node__info">
          <div class="oa-flow-node__name">{{ node.name }}</div>
          <div class="oa-flow-node__approver">
            <el-avatar :size="20" class="oa-flow-node__avatar">
              {{ node.approverName?.[0] || '?' }}
            </el-avatar>
            <span>{{ node.approverName || '未指定' }}</span>
          </div>
          <div v-if="node.approvedTime" class="oa-flow-node__time">{{ formatTime(node.approvedTime) }}</div>
          <div v-if="node.comment" class="oa-flow-node__comment" :title="node.comment">
            {{ truncate(node.comment, 20) }}
          </div>
        </div>
        <!-- 连线 -->
        <div v-if="i < nodes.length - 1" class="oa-flow-connector" :class="{ 'is-completed': isCompleted(node, nodes[i + 1]) }" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { Check, Close } from '@element-plus/icons-vue'

defineOptions({ name: 'OaApprovalFlow' })

defineProps({
  /** 节点列表（含 order/name/status/approverName/comment/approvedTime） */
  nodes: { type: Array, required: true },
  /** 当前节点 order（高亮） */
  currentNodeOrder: { type: Number, default: -1 }
})

function isCompleted(current, next) {
  return current.status === 'APPROVED'
}

function formatTime(t) {
  if (!t) return ''
  try {
    const d = new Date(t)
    return d.toLocaleString('zh-CN', { hour12: false })
  } catch {
    return t
  }
}

function truncate(s, n) {
  if (!s) return ''
  return s.length > n ? s.substring(0, n) + '...' : s
}
</script>

<style lang="scss" scoped>
.oa-approval-flow {
  width: 100%;
  overflow-x: auto;
  padding: $gap-lg 0;
}

.oa-approval-flow__nodes {
  display: flex;
  align-items: flex-start;
  gap: 0;
  min-width: max-content;
}

.oa-flow-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
  position: relative;

  &__circle {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: $bg-input;
    border: 2px solid $border;
    @include flex-center;
    font-weight: $fw-semibold;
    font-size: $fz-body;
    color: $text-2;
    transition: all $motion-base $ease-standard;
    z-index: 2;
  }

  &.status-approved &__circle {
    background: $success;
    border-color: $success-deep;
    color: $text-on-primary;
  }

  &.status-rejected &__circle {
    background: $danger;
    border-color: $danger-deep;
    color: $text-on-primary;
  }

  &.is-current &__circle {
    background: $primary;
    border-color: $primary-deep;
    color: $text-on-primary;
    box-shadow: 0 0 0 4px $primary-tint-2;
    animation: pulse 1.5s ease-in-out infinite;
  }

  @keyframes pulse {
    0%, 100% { box-shadow: 0 0 0 4px $primary-tint-2; }
    50% { box-shadow: 0 0 0 8px $primary-tint; }
  }

  &__info {
    margin-top: $gap-sm;
    text-align: center;
    max-width: 140px;
  }

  &__name {
    font-size: $fz-aux;
    font-weight: $fw-medium;
    color: $text-1;
    margin-bottom: 4px;
  }

  &__approver {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    font-size: $fz-aux;
    color: $text-2;
  }

  &__avatar {
    flex-shrink: 0;
  }

  &__time {
    font-size: $fz-mini;
    color: $text-3;
    margin-top: 2px;
  }

  &__comment {
    font-size: $fz-mini;
    color: $text-2;
    margin-top: 2px;
    font-style: italic;
  }
}

.oa-flow-connector {
  position: absolute;
  top: 18px;
  left: calc(50% + 18px);
  right: calc(-50% + 18px);
  height: 2px;
  background: $border;
  z-index: 1;
  min-width: 60px;

  &.is-completed {
    background: $success;
  }
}
</style>
