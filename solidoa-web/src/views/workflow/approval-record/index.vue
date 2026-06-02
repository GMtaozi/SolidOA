<template>
  <div class="approval-record-container">
    <el-card class="filter-card">
      <div class="filter-section">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="我发起的" name="my-apply" />
          <el-tab-pane label="我审批的" name="my-approved" />
          <el-tab-pane label="全部记录" name="all" />
        </el-tabs>
        <div class="filter-row">
          <el-select v-model="queryParams.businessType" placeholder="审批类型" clearable class="filter-item">
            <el-option label="请假" value="LEAVE" />
            <el-option label="加班" value="OVERTIME" />
            <el-option label="报销" value="EXPENSE" />
            <el-option label="用印" value="STAMP" />
            <el-option label="采购" value="PURCHASE" />
          </el-select>
          <el-select v-model="queryParams.status" placeholder="状态" clearable class="filter-item">
            <el-option label="审批中" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已拒绝" value="REJECTED" />
            <el-option label="已撤回" value="CANCELLED" />
          </el-select>
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
            end-placeholder="结束日期" value-format="YYYY-MM-DD" class="filter-item" @change="handleDateChange" />
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleExport">导出</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="list-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="recordNo" label="单号" width="140" />
        <el-table-column prop="businessType" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.businessType)">{{ getTypeName(row.businessType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="userName" label="申请人" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status)">{{ getStatusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="100" />
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">
            <span v-if="row.amount">¥{{ row.amount.toLocaleString() }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="queryParams.pageNum" v-model:page-size="queryParams.pageSize"
          :total="total" :page-sizes="[10, 20, 50, 100]" layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="currentRecord?.title || '审批详情'" width="480px" destroy-on-close>
      <div v-if="currentRecord" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="单号">{{ currentRecord.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="getTypeTag(currentRecord.businessType)">{{ getTypeName(currentRecord.businessType) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentRecord.userName }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentRecord.deptName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTag(currentRecord.status)">{{ getStatusName(currentRecord.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDate(currentRecord.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ currentRecord.startDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ currentRecord.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="金额" :span="2">
            <span v-if="currentRecord.amount">¥{{ currentRecord.amount.toLocaleString() }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="progress-section">
          <h4>审批进度</h4>
          <OaApprovalFlow
            v-if="mappedFlowData && mappedFlowData.length > 0"
            :nodes="mappedFlowData"
            :current-node-order="currentNodeOrder"
          />
          <el-empty v-else description="暂无审批节点" :image-size="60" />
        </div>

        <div class="node-section" v-if="nodeList.length > 0">
          <h4>审批详情</h4>
          <el-timeline>
            <el-timeline-item v-for="node in nodeList" :key="node.id"
              :color="getNodeColor(node.nodeStatus)" :timestamp="formatDate(node.operateTime)">
              <p><strong>{{ node.nodeName }}</strong> - {{ node.approverName }}</p>
              <p>状态：{{ getNodeStatusName(node.nodeStatus) }}</p>
              <p v-if="node.comment">意见：{{ node.comment }}</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { workflowApi } from '@/api'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/composables/useTime'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const activeTab = ref('my-apply')
const dateRange = ref([])
const detailVisible = ref(false)
const currentRecord = ref(null)
const flowData = ref([])
const nodeList = ref([])
const currentNodeOrder = computed(() => {
  // 找到第一个 PENDING 节点的 order
  const pending = flowData.value.find(n => n.status === 'PENDING')
  return pending ? (pending.order || pending.nodeOrder) : -1
})
// 字段映射：approval-record 老 API 返回 nodeOrder/nodeName，OaApprovalFlow 期望 order/name
const mappedFlowData = computed(() => flowData.value.map(n => ({
  id: n.id,
  order: n.order ?? n.nodeOrder,
  name: n.name ?? n.nodeName,
  status: n.status,
  approverName: n.approverName,
  approverId: n.approverId,
  approvedTime: n.approvedTime ?? n.operateTime,
  comment: n.comment
})))

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  businessType: '',
  status: '',
  startDate: '',
  endDate: ''
})

const getTypeName = (type) => {
  const map = {
    LEAVE: '请假',
    OVERTIME: '加班',
    EXPENSE: '报销',
    STAMP: '用印',
    PURCHASE: '采购'
  }
  return map[type] || type
}

const getTypeTag = (type) => {
  const map = {
    LEAVE: 'success',
    OVERTIME: 'warning',
    EXPENSE: 'danger',
    STAMP: 'primary',
    PURCHASE: 'warning'
  }
  return map[type] || 'info'
}

const getStatusName = (status) => {
  const map = {
    PENDING: '审批中',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CANCELLED: '已撤回'
  }
  return map[status] || status
}

const getStatusTag = (status) => {
  const map = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CANCELLED: 'info'
  }
  return map[status] || 'info'
}

const getNodeColor = (status) => {
  const map = {
    PENDING: '#FBBF24',
    APPROVED: '#34D399',
    REJECTED: '#FCA5A5'
  }
  return map[status] || '#909399'
}

const getNodeStatusName = (status) => {
  const map = {
    PENDING: '待处理',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const handleTabChange = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleDateChange = () => {
  if (dateRange.value && dateRange.value.length === 2) {
    queryParams.startDate = dateRange.value[0]
    queryParams.endDate = dateRange.value[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

const handleSearch = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handleReset = () => {
  queryParams.businessType = ''
  queryParams.status = ''
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = []
  handleSearch()
}

const handleSizeChange = () => {
  queryParams.pageNum = 1
  fetchData()
}

const handlePageChange = () => {
  fetchData()
}

const handleView = async (row) => {
  try {
    const res = await workflowApi.getApprovalRecordById(row.id)
    currentRecord.value = res.data?.data || res.data || row
    try {
      const flowRes = await workflowApi.getApprovalRecordFlow(row.id)
      // 分离flowData和nodeList数据源
      const flowResult = flowRes.data?.data || flowRes.data || {}
      flowData.value = Array.isArray(flowResult) ? flowResult : (flowResult.nodes || [])
      nodeList.value = Array.isArray(flowResult) ? flowResult : (flowResult.details || [])
    } catch (e) {
      flowData.value = []
      nodeList.value = []
    }
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleExport = async () => {
  try {
    const res = await workflowApi.exportApprovalRecord(queryParams)
    // 处理Blob响应
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/vnd.ms-excel' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `审批记录_${Date.now()}.xlsx`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error('导出失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    let res
    if (activeTab.value === 'my-apply') {
      res = await workflowApi.getMyApplyList(queryParams)
    } else if (activeTab.value === 'my-approved') {
      res = await workflowApi.getMyApprovedList(queryParams)
    } else {
      res = await workflowApi.getApprovalRecordList(queryParams)
    }
    tableData.value = res.data?.data?.records || res.data?.data || []
    total.value = res.data?.data?.total || 0
  } catch (error) {
    ElMessage.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
$warning: #FBBF24;
$success: #34D399;
$danger: #FCA5A5;
$text-secondary: #9CA3AF;

.approval-record-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;

  .filter-section {
    .filter-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      margin-top: 16px;
    }

    .filter-item {
      width: 176px;
    }
  }
}

.list-card {
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}

.detail-content {
  .progress-section {
    margin-top: 24px;

    h4 {
      margin-bottom: 16px;
      font-size: 14px;
      color: var(--text-primary);
    }
  }

  .node-section {
    margin-top: 24px;

    h4 {
      margin-bottom: 16px;
      font-size: 14px;
      color: var(--text-primary);
    }
  }
}
</style>