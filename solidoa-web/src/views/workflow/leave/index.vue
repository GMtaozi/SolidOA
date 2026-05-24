<template>
  <div class="leave-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>请假申请</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增请假
          </el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="我提交的" name="my" />
        <el-tab-pane label="待我审批" name="pending" />
      </el-tabs>

      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="leaveNo" label="请假单号" width="150" />
        <el-table-column prop="leaveType" label="请假类型" width="100">
          <template #default="{ row }">
            <el-tag>{{ leaveTypeMap[row.leaveType] || row.leaveType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="120" />
        <el-table-column prop="endDate" label="结束日期" width="120" />
        <el-table-column prop="days" label="天数" width="80" />
        <el-table-column prop="reason" label="请假事由" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            <el-button v-if="row.status === 'PENDING' && activeTab === 'my'" type="warning" link @click="handleCancel(row)">撤回</el-button>
            <el-button v-if="activeTab === 'pending'" type="success" link @click="handleApprove(row)">审批</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 请假表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择请假类型">
            <el-option label="病假" value="SICK" />
            <el-option label="事假" value="PERSONAL" />
            <el-option label="年假" value="ANNUAL" />
            <el-option label="出差" value="BUSINESS" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择开始日期" />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期" />
        </el-form-item>
        <el-form-item label="请假天数" prop="days">
          <el-input-number v-model="form.days" :min="0.5" :step="0.5" />
        </el-form-item>
        <el-form-item label="请假事由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入请假事由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { workflowApi } from '@/api/workflow'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('my')
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增请假')
const formRef = ref()

const leaveTypeMap = {
  SICK: '病假',
  PERSONAL: '事假',
  ANNUAL: '年假',
  BUSINESS: '出差'
}

const form = reactive({
  leaveType: '',
  startDate: '',
  endDate: '',
  days: 1,
  reason: ''
})

const rules = {
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入请假事由', trigger: 'blur' }]
}

const getStatusType = (status) => {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { PENDING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝' }
  return map[status] || status
}

const loadData = async () => {
  try {
    const res = activeTab.value === 'my'
      ? await workflowApi.getLeaveList({})
      : await workflowApi.getMyTasks()
    tableData.value = res.data?.records || []
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增请假'
  Object.assign(form, { leaveType: '', startDate: '', endDate: '', days: 1, reason: '' })
  dialogVisible.value = true
}

const handleView = (row) => {
  console.log('查看', row)
}

const handleCancel = async (row) => {
  await ElMessageBox.confirm('确定要撤回该请假申请吗？', '提示', { type: 'warning' })
  await workflowApi.cancelLeave(row.id)
  ElMessage.success('撤回成功')
  loadData()
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确定要通过该请假申请吗？', '提示', { type: 'warning' })
  await workflowApi.approveLeave(row.id, { approveResult: 'APPROVE', comment: '同意' })
  ElMessage.success('审批成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  await workflowApi.createLeave(form)
  ElMessage.success('提交成功')
  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.leave-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>