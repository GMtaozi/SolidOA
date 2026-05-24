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
            <el-progress :percentage="row.usageRate" :color="getProgressColor(row.usageRate)" />
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

const handleAdd = () => {
  ElMessage.info('设置预算')
}

const handleEdit = (row) => {
  ElMessage.info('调整预算: ' + row.deptName)
}

const handleView = (row) => {
  ElMessage.info('查看详情: ' + row.deptName)
}

onMounted(() => {
  // 实际从 API 加载数据
})
</script>

<style scoped lang="scss">
.budget-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>