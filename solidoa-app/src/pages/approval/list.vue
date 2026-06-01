<template>
  <view class="approval-container">
    <view class="tabs">
      <view
        :class="['tab', { active: activeTab === 'pending' }]"
        @click="switchTab('pending')"
      >
        待审批({{ pendingList.length }})
      </view>
      <view
        :class="['tab', { active: activeTab === 'processed' }]"
        @click="switchTab('processed')"
      >
        已处理
      </view>
    </view>

    <view class="list">
      <view
        v-for="item in currentList"
        :key="item.id"
        class="list-item"
        @click="handleDetail(item)"
      >
        <view class="item-header">
          <text class="type">{{ getLeaveTypeName(item.leaveType) }}</text>
          <text class="status" :class="item.status">{{ getStatusName(item.status) }}</text>
        </view>

        <view class="item-body">
          <text class="reason">{{ item.reason || '请假申请' }}</text>
        </view>

        <view class="item-footer">
          <text class="date">{{ item.startDate || '-' }} ~ {{ item.endDate || '-' }}</text>
          <text class="days">{{ item.days }}天</text>
        </view>

        <view v-if="activeTab === 'pending' && item.canApprove" class="item-actions">
          <button class="btn approve" @click.stop="handleApprove(item)">通过</button>
          <button class="btn reject" @click.stop="handleReject(item)">拒绝</button>
        </view>
      </view>

      <view v-if="currentList.length === 0" class="empty">
        <text>暂无数据</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '@/api/index'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const activeTab = ref('pending')
const pendingList = ref([])
const processedList = ref([])

const currentList = computed(() => {
  return activeTab.value === 'pending' ? pendingList.value : processedList.value
})

const leaveTypeMap = {
  SICK: '病假',
  PERSONAL: '事假',
  ANNUAL: '年假',
  BUSINESS: '出差'
}

const getLeaveTypeName = (type) => leaveTypeMap[type] || type

const getStatusName = (status) => {
  const map = { PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[status] || status
}

const switchTab = (tab) => {
  activeTab.value = tab
}

const handleDetail = (item) => {
  console.log('查看详情', item)
}

const handleApprove = async (item) => {
  uni.showModal({
    title: '确认',
    content: '确定要通过该审批吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await api.approveLeave(item.id, { approveResult: 'APPROVE', comment: '同意' })
          uni.showToast({ title: '审批成功', icon: 'success' })
          loadData()
        } catch (e) {
          console.error('审批失败', e)
        }
      }
    }
  })
}

const handleReject = async (item) => {
  uni.showModal({
    title: '确认',
    content: '确定要拒绝该审批吗？',
    success: async (res) => {
      if (res.confirm) {
        try {
          await api.approveLeave(item.id, { approveResult: 'REJECT', comment: '拒绝' })
          uni.showToast({ title: '已拒绝', icon: 'success' })
          loadData()
        } catch (e) {
          console.error('审批失败', e)
        }
      }
    }
  })
}

const loadData = async () => {
  try {
    // 并行加载待审批和已处理任务
    const [pendingRes, processedRes] = await Promise.all([
      api.getMyTasks(),
      api.getProcessedTasks()
    ])
    pendingList.value = pendingRes.data || []
    processedList.value = processedRes.data || []
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.approval-container {
  min-height: 100vh;
  background: #f5f7fa;
}

.tabs {
  display: flex;
  background: #fff;
  padding: 0 30rpx;

  .tab {
    flex: 1;
    height: 88rpx;
    line-height: 88rpx;
    text-align: center;
    font-size: 28rpx;
    color: #606266;
    border-bottom: 4rpx solid transparent;

    &.active {
      color: #409eff;
      border-bottom-color: #409eff;
    }
  }
}

.list {
  padding: 20rpx;

  .list-item {
    background: #fff;
    border-radius: 16rpx;
    padding: 30rpx;
    margin-bottom: 20rpx;

    .item-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20rpx;

      .type {
        font-size: 32rpx;
        font-weight: bold;
        color: #303133;
      }

      .status {
        font-size: 24rpx;
        padding: 4rpx 16rpx;
        border-radius: 8rpx;

        &.PENDING {
          background: #fef0f0;
          color: #f56c6c;
        }

        &.APPROVED {
          background: #f0f9eb;
          color: #67c23a;
        }

        &.REJECTED {
          background: #f4f4f5;
          color: #909399;
        }
      }
    }

    .item-body {
      margin-bottom: 20rpx;

      .reason {
        font-size: 28rpx;
        color: #606266;
        line-height: 1.5;
      }
    }

    .item-footer {
      display: flex;
      justify-content: space-between;
      font-size: 24rpx;
      color: #909399;
      margin-bottom: 20rpx;
    }

    .item-actions {
      display: flex;
      justify-content: flex-end;
      gap: 20rpx;

      .btn {
        width: 160rpx;
        height: 64rpx;
        line-height: 64rpx;
        font-size: 26rpx;
        border-radius: 32rpx;
        padding: 0;
        margin: 0;

        &.approve {
          background: #409eff;
          color: #fff;
        }

        &.reject {
          background: #fff;
          color: #909399;
          border: 1rpx solid #dcdfe6;
        }
      }
    }
  }

  .empty {
    text-align: center;
    padding: 100rpx 0;
    color: #909399;
    font-size: 28rpx;
  }
}
</style>