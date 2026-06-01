<template>
  <div class="shifts-container">
    <el-card class="shifts-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">班次管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn primary" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增班次
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="班次名称" class="cyber-form-item">
          <el-input v-model="searchForm.name" placeholder="请输入班次名称" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="状态" class="cyber-form-item">
          <el-select v-model="searchForm.status" placeholder="全部" clearable class="cyber-select">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="cyber-btn" type="primary" @click="handleSearch">查询</el-button>
          <el-button class="cyber-btn" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border class="shifts-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="班次名称" min-width="120">
          <template #default="{ row }">
            <div class="shift-name-cell">
              <el-icon><Clock /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="上班时间" width="120">
          <template #default="{ row }">
            {{ row.onDutyTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="下班时间" width="120">
          <template #default="{ row }">
            {{ row.offDutyTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="弹性时间" width="100" align="center">
          <template #default="{ row }">
            {{ row.flexibleMinutes || 0 }}分钟
          </template>
        </el-table-column>
        <el-table-column label="宽限时间" width="100" align="center">
          <template #default="{ row }">
            {{ row.graceMinutes || 0 }}分钟
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'status-active' : 'status-disabled'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link class="cyber-link" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link class="cyber-link danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 班次表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="cyber-form">
        <el-form-item label="班次名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入班次名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="上班时间" prop="onDutyTime">
          <el-time-select
            v-model="form.onDutyTime"
            placeholder="选择上班时间"
            start="00:00"
            step="00:15"
            end="23:45"
            class="cyber-time-select"
          />
        </el-form-item>
        <el-form-item label="下班时间" prop="offDutyTime">
          <el-time-select
            v-model="form.offDutyTime"
            placeholder="选择下班时间"
            start="00:00"
            step="00:15"
            end="23:45"
            class="cyber-time-select"
          />
        </el-form-item>
        <el-form-item label="弹性时间" prop="flexibleMinutes">
          <el-input-number v-model="form.flexibleMinutes" :min="0" :max="120" class="cyber-input-number" />
          <span class="form-tip">允许晚到的时间（分钟）</span>
        </el-form-item>
        <el-form-item label="宽限时间" prop="graceMinutes">
          <el-input-number v-model="form.graceMinutes" :min="0" :max="60" class="cyber-input-number" />
          <span class="form-tip">打卡宽限时间（分钟）</span>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" class="cyber-input-number" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注说明" class="cyber-textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cyber-btn" @click="dialogVisible = false">取消</el-button>
        <el-button class="cyber-btn primary" type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { hrApi } from '@/api/hr'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Clock } from '@element-plus/icons-vue'

const loading = ref(false)
const searchForm = reactive({
  name: '',
  status: null
})

const tableData = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增班次')
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  onDutyTime: '09:00',
  offDutyTime: '18:00',
  flexibleMinutes: 30,
  graceMinutes: 15,
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入班次名称', trigger: 'blur' }],
  onDutyTime: [{ required: true, message: '请选择上班时间', trigger: 'change' }],
  offDutyTime: [{ required: true, message: '请选择下班时间', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await hrApi.getShifts()
    let data = res.data?.data || res.data || []
    if (!Array.isArray(data)) data = []
    if (searchForm.name) {
      data = data.filter(item => item.name?.includes(searchForm.name))
    }
    if (searchForm.status !== null) {
      data = data.filter(item => item.status === searchForm.status)
    }
    tableData.value = data
  } catch (error) {
    console.error('加载班次数据失败', error)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  loadData()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.status = null
  loadData()
}

const handleAdd = () => {
  dialogTitle.value = '新增班次'
  Object.assign(form, {
    id: null,
    name: '',
    onDutyTime: '09:00',
    offDutyTime: '18:00',
    flexibleMinutes: 30,
    graceMinutes: 15,
    sortOrder: 0,
    status: 1,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑班次'
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    onDutyTime: row.onDutyTime || '09:00',
    offDutyTime: row.offDutyTime || '18:00',
    flexibleMinutes: row.flexibleMinutes || 30,
    graceMinutes: row.graceMinutes || 15,
    sortOrder: row.sortOrder || 0,
    status: row.status || 1,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除班次「${row.name}」吗？`, '提示', { type: 'warning' })
  await hrApi.deleteShift(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()

  const submitData = { ...form }
  if (form.id) {
    await hrApi.updateShift(form.id, submitData)
    ElMessage.success('更新成功')
  } else {
    await hrApi.createShift(submitData)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
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

.shifts-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.shifts-card {
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
  margin-bottom: 24px;

  :deep(.el-form-item) {
    margin-bottom: 0;
    margin-right: 20px;
  }

  :deep(.el-form-item__label) {
    color: $text-secondary;
    font-weight: 500;
  }
}

.shifts-table {
  :deep(.el-table__header-wrapper) {
    th {
      background: $bg-primary;
      color: $text-primary;
      font-weight: 600;
      border: none;
      padding: 16px 12px;
    }
  }

  :deep(.el-table__body-wrapper) {
    tr:hover > td {
      background: #fcfaf7;
    }

    td {
      border: none;
      padding: 14px 12px;
    }
  }
}

.shift-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: $primary;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;

  &.status-active {
    background: rgba($success, 0.15);
    color: $success;
  }

  &.status-disabled {
    background: rgba(#FBBF24, 0.15);
    color: #FBBF24;
  }
}

.cyber-link {
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

  &:hover {
    opacity: 0.8;
  }

  &.danger {
    color: #FBBF24;
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

  &.primary {
    background: $primary;
    border-color: $primary;
    color: #ffffff;

    &:hover {
      background: #5a95f7;
      border-color: #5a95f7;
    }
  }
}

.cyber-dialog {
  :deep(.el-dialog) {
    background: $bg-card;
    border-radius: 16px;

    .el-dialog__header {
      border-bottom: 1px solid $border-color;
      padding: 20px 24px;

      .el-dialog__title {
        color: $text-primary;
        font-weight: 600;
      }
    }

    .el-dialog__body {
      padding: 30px 24px;
    }

    .el-dialog__footer {
      border-top: 1px solid $border-color;
      padding: 16px 24px;
    }
  }

  .cyber-form {
    :deep(.el-form-item__label) {
      color: $text-secondary;
    }
  }
}

.cyber-input {
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
  }
}

.cyber-select {
  width: 120px;
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
  }
}

.cyber-time-select {
  width: 180px;
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
  }
}

.cyber-input-number {
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 0 12px;
  }
}

.cyber-textarea {
  :deep(.el-textarea__inner) {
    border-radius: 12px;
    padding: 12px 16px;
    resize: none;
  }
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: $text-secondary;
}
</style>