<template>
  <div class="budget-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>预算管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            设置预算
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border>
        <el-table-column prop="deptName" label="部门" />
        <el-table-column prop="yearMonth" label="月份" width="120" />
        <el-table-column prop="totalAmount" label="预算总额" width="120" />
        <el-table-column prop="usedAmount" label="已使用" width="120" />
        <el-table-column prop="remainingAmount" label="剩余" width="120" />
        <el-table-column label="使用率" width="200">
          <template #default="{ row }">
            <el-progress :percentage="Math.min(row.usageRate, 100)" :color="getProgressColor(row.usageRate)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">调整</el-button>
            <el-button type="warning" link @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const tableData = ref([
  { id: 1, deptName: '技术部', yearMonth: '2026-05', totalAmount: 50000, usedAmount: 32000, remainingAmount: 18000, usageRate: 64 },
  { id: 2, deptName: '销售部', yearMonth: '2026-05', totalAmount: 80000, usedAmount: 76000, remainingAmount: 4000, usageRate: 95 },
  { id: 3, deptName: '财务部', yearMonth: '2026-05', totalAmount: 20000, usedAmount: 8000, remainingAmount: 12000, usageRate: 40 }
])

const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#f56c6c'
  if (percentage >= 70) return '#e6a23c'
  return '#67c23a'
}

const escapeHtml = (str) => {
  if (!str) return '-'
  return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')
}

const handleAdd = () => {
  ElMessage.info('设置预算')
}

const handleEdit = (row) => {
  ElMessage.info('调整预算: ' + escapeHtml(row.deptName))
}

const handleView = (row) => {
  ElMessage.info('查看详情: ' + escapeHtml(row.deptName))
}

onMounted(() => {
  // 实际从 API 加载数据
})
</script>

<style scoped lang="scss">
// Soft & Comfortable 预算管理样式
$bg-main: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$primary-light: #93c5fd;
$success: #A3E635;
$warning: #FDE047;
$expense: #34D399;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border: #F0EDE9;

.budget-container {
  padding: 20px;
  background: $bg-main;
  min-height: calc(100vh - 120px);
  border-radius: 16px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 0;
  }
}

:deep(.el-card) {
  background: $bg-card;
  border-radius: 16px;
  border: 1px solid $border;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.3s ease;

  &:hover {
    box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
  }

  .el-card__header {
    border-bottom: 1px solid $border;
    padding: 20px 24px;
    background: transparent;
  }

  .el-card__body {
    padding: 0;
  }
}

:deep(.el-table) {
  border-radius: 0 0 16px 16px;
  overflow: hidden;

  th.el-table__cell {
    background: $bg-main;
    color: $text-secondary;
    font-weight: 600;
    font-size: 13px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    padding: 16px 12px;
    border-bottom: 1px solid $border;
  }

  td.el-table__cell {
    padding: 18px 12px;
    color: $text-primary;
    font-size: 14px;
    border-bottom: 1px solid $border;
    transition: background 0.2s ease;
  }

  tr:hover > td.el-table__cell {
    background: rgba(96, 165, 250, 0.04);
  }

  .el-table__empty-text {
    color: $text-secondary;
    padding: 48px 0;
  }
}

:deep(.el-progress__text) {
  color: $text-secondary !important;
  font-size: 12px !important;
  font-weight: 500;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, $primary 0%, $primary-light 100%);
  border: none;
  border-radius: 40px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(96, 165, 250, 0.3);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  .el-icon {
    margin-right: 6px;
  }

  &:hover {
    background: linear-gradient(135deg, $primary-light 0%, $primary 100%);
    box-shadow: 0 8px 20px rgba(96, 165, 250, 0.4);
    transform: translateY(-2px);
  }

  &:active {
    transform: translateY(0);
  }
}

:deep(.el-button--warning) {
  color: $expense;
  background: rgba(52, 211, 153, 0.1);
  border: none;
  border-radius: 40px;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    background: rgba(52, 211, 153, 0.2);
    color: darken($expense, 10%);
  }
}

:deep(.el-progress-bar__outer) {
  background: $bg-main;
  border-radius: 8px;
}

:deep(.el-progress-bar__inner) {
  border-radius: 8px;
  transition: width 0.6s ease;
}
</style>