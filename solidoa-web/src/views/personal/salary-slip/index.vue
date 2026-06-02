<template>
  <div class="salary-slip-container">
    <el-card class="filter-card">
      <div class="filter-section">
        <div class="filter-row">
          <el-date-picker v-model="queryParams.startMonth" type="month" placeholder="开始月份" value-format="YYYY-MM"
            class="filter-item" />
          <el-date-picker v-model="queryParams.endMonth" type="month" placeholder="结束月份" value-format="YYYY-MM"
            class="filter-item" />
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card class="list-card" v-loading="loading">
      <el-empty v-if="tableData.length === 0" description="暂无工资记录" />
      <div v-else class="salary-cards">
        <el-card v-for="item in tableData" :key="item.id" class="salary-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="month">{{ item.yearMonth }}</span>
              <el-tag :type="getStatusType(item.status)">{{ getStatusName(item.status) }}</el-tag>
            </div>
          </template>
          <div class="salary-content">
            <div class="salary-amount">
              <span class="label">实发工资</span>
              <span class="value">¥{{ item.netSalary?.toLocaleString() || '0' }}</span>
            </div>
            <div class="salary-details">
              <div class="detail-row">
                <span class="label">应发工资</span>
                <span class="value success">¥{{ item.grossSalary?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">基本工资</span>
                <span class="value">¥{{ item.baseSalary?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">岗位工资</span>
                <span class="value">¥{{ item.positionSalary?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">绩效工资</span>
                <span class="value">¥{{ item.performanceSalary?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">加班费</span>
                <span class="value">¥{{ item.overtimeSalary?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">奖金</span>
                <span class="value">¥{{ item.bonus?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">补贴</span>
                <span class="value">¥{{ item.subsidy?.toLocaleString() || '0' }}</span>
              </div>
            </div>
            <el-divider />
            <div class="deductions">
              <div class="detail-row">
                <span class="label">社保公积金</span>
                <span class="value danger">-¥{{ item.socialInsurance?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">个人所得税</span>
                <span class="value danger">-¥{{ item.personalTax?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">迟到扣款</span>
                <span class="value danger">-¥{{ item.lateFine?.toLocaleString() || '0' }}</span>
              </div>
              <div class="detail-row">
                <span class="label">其他扣款</span>
                <span class="value danger">-¥{{ item.otherDeduction?.toLocaleString() || '0' }}</span>
              </div>
            </div>
          </div>
          <template #footer>
            <div class="card-footer">
              <span class="date">发放时间：{{ formatDate(item.paidTime) }}</span>
              <div class="actions">
                <el-button type="primary" link @click="handleViewDetail(item)">查看详情</el-button>
                <el-button v-if="item.status === 'PAID' && !item.confirmed" type="success" link @click="handleConfirm(item)">确认工资</el-button>
                <el-tag v-if="item.confirmed" type="success" size="small">已确认</el-tag>
                <el-button v-if="item.status === 'PAID' && !item.confirmed" type="warning" link @click="handleDispute(item)">提出异议</el-button>
              </div>
            </div>
          </template>
        </el-card>
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`${currentSalary?.yearMonth} 工资条详情`" width="600px" destroy-on-close>
      <div v-if="currentSalary" class="salary-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工资月份">{{ currentSalary.yearMonth }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentSalary.status)">{{ getStatusName(currentSalary.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="基本工资">¥{{ currentSalary.baseSalary?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="岗位工资">¥{{ currentSalary.positionSalary?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="绩效工资">¥{{ currentSalary.performanceSalary?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="加班费">¥{{ currentSalary.overtimeSalary?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="奖金">¥{{ currentSalary.bonus?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="补贴">¥{{ currentSalary.subsidy?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="其他加项">¥{{ currentSalary.otherAddition?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="社保公积金">¥{{ currentSalary.socialInsurance?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="个人所得税">¥{{ currentSalary.personalTax?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="迟到扣款">¥{{ currentSalary.lateFine?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="其他扣款">¥{{ currentSalary.otherDeduction?.toLocaleString() }}</el-descriptions-item>
          <el-descriptions-item label="应发工资" class-name="success-text">
            <span class="success">¥{{ currentSalary.grossSalary?.toLocaleString() }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="实发工资" :span="2" class-name="net-salary-text">
            <span class="primary">¥{{ currentSalary.netSalary?.toLocaleString() }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="发放时间" :span="2">{{ formatDate(currentSalary.paidTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 异议弹窗 -->
    <el-dialog v-model="disputeVisible" title="提出工资异议" width="500px">
      <div v-if="currentSalary" class="dispute-info">
        <p><strong>工资月份：</strong>{{ currentSalary.yearMonth }}</p>
        <p><strong>实发工资：</strong>¥{{ currentSalary.netSalary?.toLocaleString() }}</p>
      </div>
      <el-form label-width="80px">
        <el-form-item label="异议原因">
          <el-input v-model="disputeForm.reason" type="textarea" :rows="4" placeholder="请详细描述工资条中存在的问题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="disputeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDispute">提交异议</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { hrApi } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDate } from '@/composables/useTime'

const loading = ref(false)
const tableData = ref([])
const detailVisible = ref(false)
const disputeVisible = ref(false)
const currentSalary = ref(null)
const disputeForm = ref({ reason: '' })

const queryParams = reactive({
  startMonth: '',
  endMonth: ''
})

const getStatusName = (status) => {
  const map = {
    DRAFT: '草稿',
    PENDING: '审批中',
    APPROVED: '已审批',
    PAID: '已发放'
  }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    PAID: 'primary'
  }
  return map[status] || 'info'
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  queryParams.startMonth = ''
  queryParams.endMonth = ''
  fetchData()
}

const handleViewDetail = (item) => {
  currentSalary.value = item
  detailVisible.value = true
}

// 确认工资条
const handleConfirm = async (item) => {
  try {
    await ElMessageBox.confirm('确认工资条无误？确认后将无法提出异议。', '确认工资', { type: 'warning' })
    await hrApi.confirmSalary(item.id)
    ElMessage.success('确认成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('确认失败')
    }
  }
}

// 提出异议
const handleDispute = (item) => {
  currentSalary.value = item
  disputeForm.value.reason = ''
  disputeVisible.value = true
}

// 提交异议
const submitDispute = async () => {
  if (!disputeForm.value.reason) {
    ElMessage.warning('请填写异议原因')
    return
  }
  try {
    await hrApi.disputeSalary(currentSalary.value.id, disputeForm.value)
    ElMessage.success('异议提交成功，HR将会处理')
    disputeVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await hrApi.getMySalary(queryParams)
    tableData.value = res.data?.data || res.data || []
  } catch (error) {
    ElMessage.error('获取工资记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.salary-slip-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 16px;

  .filter-section {
    .filter-row {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
    }

    .filter-item {
      width: 160px;
    }
  }
}

.list-card {
  .salary-cards {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 16px;
  }

  .salary-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .month {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary);
      }
    }

    .salary-content {
      .salary-amount {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 0;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        border-radius: 12px;
        padding: 20px;
        margin-bottom: 16px;

        .label {
          color: rgba(255, 255, 255, 0.9);
          font-size: 14px;
        }

        .value {
          color: #fff;
          font-size: 24px;
          font-weight: 600;
        }
      }

      .salary-details,
      .deductions {
        .detail-row {
          display: flex;
          justify-content: space-between;
          padding: 8px 0;

          .label {
            color: var(--text-secondary);
            font-size: 13px;
          }

          .value {
            font-size: 13px;
            font-weight: 500;

            &.success {
              color: var(--success);
            }

            &.danger {
              color: var(--danger);
            }
          }
        }
      }
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .date {
        color: var(--text-secondary);
        font-size: 12px;
      }

      .actions {
        display: flex;
        gap: 8px;
        align-items: center;
      }
    }
  }
}

.dispute-info {
  background: #f9fafb;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;

  p {
    margin: 8px 0;
    color: #374151;
  }
}

.salary-detail {
  :deep(.success-text) {
    color: var(--success);
    font-weight: 600;
  }

  :deep(.net-salary-text) {
    color: var(--primary);
    font-weight: 600;
  }
}
</style>