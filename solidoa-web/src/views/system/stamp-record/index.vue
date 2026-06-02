<template>
  <div class="stamp-record-container">
    <el-card class="cyber-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">用印记录</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn" @click="handleExport">
            <el-icon><Download /></el-icon>
            导出记录
          </el-button>
        </div>
      </template>

      <!-- 筛选条件 -->
      <el-form :inline="true" class="search-form">
        <el-form-item label="印章类型">
          <el-select v-model="searchForm.stampType" placeholder="全部" clearable>
            <el-option label="公章" value="公章" />
            <el-option label="合同专用章" value="合同专用章" />
            <el-option label="财务专用章" value="财务专用章" />
            <el-option label="人事专用章" value="人事专用章" />
            <el-option label="部门章" value="部门章" />
          </el-select>
        </el-form-item>
        <el-form-item label="用印日期">
          <el-date-picker v-model="searchForm.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 记录列表 -->
      <el-table :data="tableData" border class="cyber-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="usageNo" label="用印单号" width="150" />
        <el-table-column prop="stampName" label="印章名称" />
        <el-table-column prop="applicantName" label="申请人" />
        <el-table-column prop="fileName" label="文件名称" />
        <el-table-column prop="fileCount" label="份数" width="80" />
        <el-table-column prop="reason" label="用印事由" show-overflow-tooltip />
        <el-table-column prop="usageTime" label="用印时间" width="180" />
        <el-table-column prop="operatorName" label="经办人" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link class="cyber-link" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData"
        @current-change="loadData"
        class="cyber-pagination"
      />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用印详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用印单号">{{ detail.usageNo }}</el-descriptions-item>
        <el-descriptions-item label="印章名称">{{ detail.stampName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="文件名称">{{ detail.fileName }}</el-descriptions-item>
        <el-descriptions-item label="份数">{{ detail.fileCount }}</el-descriptions-item>
        <el-descriptions-item label="用印事由" :span="2">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item label="用印时间">{{ detail.usageTime }}</el-descriptions-item>
        <el-descriptions-item label="经办人">{{ detail.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'

const tableData = ref([])
const detailVisible = ref(false)
const detail = ref({})

const searchForm = reactive({
  stampType: '',
  dateRange: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadData = async () => {
  try {
    const params = {
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      stampType: searchForm.stampType
    }
    if (searchForm.dateRange) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    const res = await workflowApi.getStampRecords(params)
    const data = res.data?.data || res.data || {}
    tableData.value = data.records || []
    pagination.total = data.total || 0
  } catch (error) {
    console.error('加载用印记录失败', error)
  }
}

const handleReset = () => {
  searchForm.stampType = ''
  searchForm.dateRange = null
  loadData()
}

const handleDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

const handleExport = () => {
  // 导出功能
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.stamp-record-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;

  .cyber-card {
    background: $bg-card;
    border-radius: 16px;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);

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
  }

  .search-form {
    margin-bottom: 20px;

    :deep(.el-form-item) {
      margin-bottom: 0;
      margin-right: 16px;
    }
  }

  .cyber-table {
    background: transparent;
    border: none;

    :deep(.el-table__header-wrapper) {
      th {
        background: $bg-primary;
        color: $text-primary;
        font-weight: 600;
        border: none;
      }
    }

    :deep(.el-table__body-wrapper) {
      tr {
        background: $bg-card;

        &:hover > td {
          background: #fcfaf7;
        }

        td {
          border: none;
          color: $text-primary;
        }
      }
    }
  }

  .cyber-link {
    font-weight: 500;
    color: $primary;
  }

  .cyber-btn {
    background: $bg-card;
    border: 1px solid $border-color;
    color: $text-primary;
    font-weight: 500;
    border-radius: 12px;
    padding: 10px 20px;

    &:hover {
      border-color: $primary;
      color: $primary;
    }

    &[type="primary"] {
      background: $primary;
      border-color: $primary;
      color: #ffffff;
    }
  }

  .cyber-pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }
}
</style>
