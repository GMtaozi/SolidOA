<template>
  <div class="balance-container">
    <el-card class="balance-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">假期余额</span>
            <span class="title-line"></span>
          </div>
          <div class="header-actions">
            <el-select v-model="selectedYear" placeholder="选择年份" class="year-select" @change="loadBalance">
              <el-option v-for="year in yearOptions" :key="year" :label="`${year}年`" :value="year" />
            </el-select>
            <el-button class="cyber-btn" @click="loadBalance">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="balance-content">
        <div v-if="balanceList.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无假期余额数据" />
        </div>

        <div v-else class="balance-grid">
          <div v-for="item in balanceList" :key="item.typeId" class="balance-item" :style="{ borderLeftColor: getTypeColor(item.typeCode) }">
            <div class="balance-header">
              <div class="balance-icon" :style="{ background: `${getTypeColor(item.typeCode)}15` }">
                <el-icon :style="{ color: getTypeColor(item.typeCode) }">
                  <component :is="getTypeIcon(item.typeCode)" />
                </el-icon>
              </div>
              <div class="balance-info">
                <span class="balance-name">{{ item.typeName }}</span>
                <span class="balance-code">{{ item.typeCode }}</span>
              </div>
            </div>

            <div class="balance-stats">
              <div class="stat-item available">
                <span class="stat-value">{{ item.availableDays }}</span>
                <span class="stat-label">可用天数</span>
              </div>
              <div class="stat-item used">
                <span class="stat-value">{{ item.usedDays }}</span>
                <span class="stat-label">已使用</span>
              </div>
              <div class="stat-item pending">
                <span class="stat-value">{{ item.pendingDays }}</span>
                <span class="stat-label">待审批</span>
              </div>
            </div>

            <div class="balance-progress">
              <div class="progress-bar">
                <div
                  class="progress-used"
                  :style="{ width: `${getUsedPercent(item)}%`, background: getTypeColor(item.typeCode) }"
                ></div>
                <div
                  class="progress-pending"
                  :style="{ width: `${getPendingPercent(item)}%`, background: `${getTypeColor(item.typeCode)}60` }"
                ></div>
              </div>
              <div class="progress-labels">
                <span class="label-left">剩余 {{ item.availableDays }} 天</span>
                <span class="label-right">共 {{ item.totalDays }} 天</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 假期使用明细 -->
    <el-card class="detail-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">假期使用明细</span>
            <span class="title-line"></span>
          </div>
        </div>
      </template>

      <el-table :data="usageRecords" border class="detail-table">
        <el-table-column prop="leaveType" label="假期类型" width="120" />
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="days" label="天数" width="80" align="center" />
        <el-table-column prop="reason" label="事由" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="getStatusClass(row.status)">
              {{ getStatusText(row.status) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadUsageRecords"
        @current-change="loadUsageRecords"
        class="cyber-pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { hrApi } from '@/api/hr'
import { Refresh, Calendar, FirstAidKit, HomeFilled, Trophy, Coffee, Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const selectedYear = ref(new Date().getFullYear())
const balanceList = ref([])
const usageRecords = ref([])

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear()
  return [currentYear - 1, currentYear, currentYear + 1]
})

const typeColorMap = {
  'ANNUAL': '#60A5FA',
  'SICK': '#34D399',
  'PERSONAL': '#FBBF24',
  'BEREAVEMENT': '#A78BFA',
  'MARRIAGE': '#F472B6',
  'MATERNITY': '#FB923C',
  'PATERNITY': '#38BDF8',
  'HOME': '#10B981',
  'UNPAID': '#9CA3AF'
}

const typeIconMap = {
  'ANNUAL': Trophy,
  'SICK': FirstAidKit,
  'PERSONAL': HomeFilled,
  'BEREAVEMENT': Calendar,
  'MARRIAGE': Coffee,
  'MATERNITY': Coffee,
  'PATERNITY': Clock,
  'HOME': Calendar,
  'UNPAID': Clock
}

const getTypeColor = (code) => typeColorMap[code] || '#60A5FA'
const getTypeIcon = (code) => typeIconMap[code] || Calendar

const getUsedPercent = (item) => {
  if (!item.totalDays) return 0
  return Math.round((item.usedDays / item.totalDays) * 100)
}

const getPendingPercent = (item) => {
  if (!item.totalDays) return 0
  return Math.round((item.pendingDays / item.totalDays) * 100)
}

const getStatusClass = (status) => {
  const map = {
    'PENDING': 'status-pending',
    'APPROVED': 'status-approved',
    'REJECTED': 'status-rejected',
    'CANCELLED': 'status-cancelled'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已驳回',
    'CANCELLED': '已取消'
  }
  return map[status] || status
}

const loadBalance = async () => {
  loading.value = true
  try {
    const res = await hrApi.getMyLeaveBalance(selectedYear.value)
    if (res.data) {
      balanceList.value = Array.isArray(res.data) ? res.data : [res.data]
    } else {
      balanceList.value = []
    }
  } catch (error) {
    console.error('加载假期余额失败', error)
    balanceList.value = []
  } finally {
    loading.value = false
  }
}

const loadUsageRecords = async () => {
  try {
    const res = await hrApi.getRecords({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      year: selectedYear.value
    })
    if (res.data?.data) {
      usageRecords.value = res.data.data.records || res.data.data || []
      pagination.total = res.data.data.total || 0
    }
  } catch (error) {
    console.error('加载使用记录失败', error)
  }
}

onMounted(() => {
  loadBalance()
  loadUsageRecords()
})
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.balance-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.balance-card,
.detail-card {
  background: $bg-card;
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
  margin-bottom: 24px;

  :deep(.el-card__header) {
    border-bottom: 1px solid $border-color;
    padding: 20px 24px;
  }

  :deep(.el-card__body) {
    padding: 24px;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title-wrapper {
    display: flex;
    align-items: center;
    gap: 16px;

    .title-text {
      font-size: 20px;
      font-weight: 600;
      color: $text-primary;
    }

    .title-line {
      width: 40px;
      height: 3px;
      background: linear-gradient(90deg, $primary, transparent);
      border-radius: 2px;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
}

.year-select {
  width: 120px;

  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
  }
}

.balance-content {
  min-height: 200px;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.balance-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.balance-item {
  background: $bg-primary;
  border-radius: 12px;
  padding: 20px;
  
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    box-shadow: 0 8px 20px -8px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }
}

.balance-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;

  .balance-icon {
    width: 44px;
    height: 44px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
  }

  .balance-info {
    display: flex;
    flex-direction: column;
    gap: 4px;

    .balance-name {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
    }

    .balance-code {
      font-size: 12px;
      color: $text-secondary;
    }
  }
}

.balance-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;

  .stat-item {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 12px 8px;
    background: $bg-card;
    border-radius: 8px;
    margin: 0 4px;

    &:first-child {
      margin-left: 0;
    }

    &:last-child {
      margin-right: 0;
    }

    .stat-value {
      font-size: 22px;
      font-weight: 700;
      color: $text-primary;
    }

    .stat-label {
      font-size: 11px;
      color: $text-secondary;
      margin-top: 4px;
    }

    &.available .stat-value {
      color: $primary;
    }

    &.used .stat-value {
      color: $success;
    }

    &.pending .stat-value {
      color: $warning;
    }
  }
}

.balance-progress {
  .progress-bar {
    height: 8px;
    background: rgba($text-secondary, 0.15);
    border-radius: 4px;
    overflow: hidden;
    display: flex;

    .progress-used {
      height: 100%;
      transition: width 0.5s ease;
    }

    .progress-pending {
      height: 100%;
      transition: width 0.5s ease;
    }
  }

  .progress-labels {
    display: flex;
    justify-content: space-between;
    margin-top: 8px;
    font-size: 12px;
    color: $text-secondary;
  }
}

.detail-table {
  :deep(.el-table__header-wrapper) {
    th {
      background: $bg-primary;
      color: $text-primary;
      font-weight: 600;
      border: none;
    }
  }

  :deep(.el-table__body-wrapper) {
    tr:hover > td {
      background: #fcfaf7;
    }

    td {
      border: none;
    }
  }
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;

  &.status-pending {
    background: rgba($warning, 0.15);
    color: $warning;
  }

  &.status-approved {
    background: rgba($success, 0.15);
    color: $success;
  }

  &.status-rejected {
    background: rgba(#FBBF24, 0.15);
    color: #FBBF24;
  }

  &.status-cancelled {
    background: rgba($text-secondary, 0.15);
    color: $text-secondary;
  }
}

.cyber-pagination {
  margin-top: 24px;
  justify-content: flex-end;

  :deep(.el-pager li) {
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: 12px;
    margin: 0 4px;

    &.is-active {
      background: $primary;
      color: #ffffff;
      border-color: $primary;
    }
  }
}

.cyber-btn {
  background: $bg-card;
  border: 1px solid $border-color;
  color: $text-primary;
  font-weight: 500;
  border-radius: 12px;
  padding: 10px 20px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    border-color: $primary;
    color: $primary;
  }
}
</style>