<template>
  <div class="leave-types-container">
    <el-card class="types-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">假期类型管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn primary" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增类型
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="类型名称" class="cyber-form-item">
          <el-input v-model="searchForm.name" placeholder="请输入类型名称" clearable class="cyber-input" />
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

      <el-table :data="tableData" border class="types-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="typeCode" label="类型编码" width="120">
          <template #default="{ row }">
            <span class="type-code" :style="{ color: getTypeColor(row.typeCode) }">{{ row.typeCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="typeName" label="类型名称" min-width="120">
          <template #default="{ row }">
            <div class="type-name-cell">
              <el-icon :style="{ color: getTypeColor(row.typeCode) }">
                <component :is="getTypeIcon(row.typeCode)" />
              </el-icon>
              <span>{{ row.typeName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="defaultDays" label="默认天数" width="100" align="center" />
        <el-table-column prop="unit" label="单位" width="80" align="center">
          <template #default="{ row }">
            {{ row.unit === 'DAY' ? '天' : '小时' }}
          </template>
        </el-table-column>
        <el-table-column prop="canAccrue" label="可结转" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.canAccrue" type="success" size="small">是</el-tag>
            <el-tag v-else type="info" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxAccrueDays" label="结转上限" width="100" align="center">
          <template #default="{ row }">
            {{ row.canAccrue ? (row.maxAccrueDays || '-') : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'status-active' : 'status-disabled'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link class="cyber-link" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link class="cyber-link danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 类型表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="cyber-form">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="form.typeCode" :disabled="!!form.id" placeholder="如：ANNUAL、SICK" class="cyber-input" />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="请输入类型名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="默认天数" prop="defaultDays">
          <el-input-number v-model="form.defaultDays" :min="0" :max="365" class="cyber-input-number" />
          <span class="form-tip">每年分配的默认天数</span>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-radio-group v-model="form.unit">
            <el-radio label="DAY">天</el-radio>
            <el-radio label="HOUR">小时</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="可结转" prop="canAccrue">
          <el-switch v-model="form.canAccrue" active-color="#34D399" inactive-color="#dcdfe6" />
          <span class="form-tip">启用后当年未用完的假期可结转到下年</span>
        </el-form-item>
        <el-form-item v-if="form.canAccrue" label="结转上限" prop="maxAccrueDays">
          <el-input-number v-model="form.maxAccrueDays" :min="0" :max="365" class="cyber-input-number" />
          <span class="form-tip">最多可结转的天数，0表示不限制</span>
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
import { Plus, Calendar, FirstAidKit, HomeFilled, Trophy, Coffee, Clock, Umbrella } from '@element-plus/icons-vue'

const loading = ref(false)
const searchForm = reactive({
  name: '',
  status: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增假期类型')
const formRef = ref()
const form = reactive({
  id: null,
  typeCode: '',
  typeName: '',
  defaultDays: 0,
  unit: 'DAY',
  canAccrue: false,
  maxAccrueDays: 0,
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules = {
  typeCode: [{ required: true, message: '请输入类型编码', trigger: 'blur' }],
  typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
  defaultDays: [{ required: true, message: '请输入默认天数', trigger: 'blur' }],
  unit: [{ required: true, message: '请选择单位', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

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
  'BEREAVEMENT': Umbrella,
  'MARRIAGE': Coffee,
  'MATERNITY': Coffee,
  'PATERNITY': Clock,
  'HOME': Calendar,
  'UNPAID': Clock
}

const getTypeColor = (code) => typeColorMap[code] || '#60A5FA'
const getTypeIcon = (code) => typeIconMap[code] || Calendar

const loadData = async () => {
  loading.value = true
  try {
    const res = await hrApi.getLeaveTypes()
    let data = res.data?.data || res.data || []
    if (Array.isArray(data)) {
      if (searchForm.name) {
        data = data.filter(item => item.typeName.includes(searchForm.name))
      }
      if (searchForm.status !== null) {
        data = data.filter(item => item.status === searchForm.status)
      }
      pagination.total = data.length
      const start = (pagination.pageNum - 1) * pagination.pageSize
      tableData.value = data.slice(start, start + pagination.pageSize)
    } else {
      tableData.value = []
      pagination.total = 0
    }
  } catch (error) {
    console.error('加载假期类型失败', error)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.status = null
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增假期类型'
  Object.assign(form, {
    id: null,
    typeCode: '',
    typeName: '',
    defaultDays: 0,
    unit: 'DAY',
    canAccrue: false,
    maxAccrueDays: 0,
    sortOrder: 0,
    status: 1,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑假期类型'
  Object.assign(form, {
    id: row.id,
    typeCode: row.typeCode,
    typeName: row.typeName,
    defaultDays: row.defaultDays || 0,
    unit: row.unit || 'DAY',
    canAccrue: row.canAccrue || false,
    maxAccrueDays: row.maxAccrueDays || 0,
    sortOrder: row.sortOrder || 0,
    status: row.status || 1,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除假期类型「${row.typeName}」吗？`, '提示', { type: 'warning' })
  await hrApi.deleteLeaveType(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()

  const submitData = { ...form }
  if (form.id) {
    await hrApi.updateLeaveType(form.id, submitData)
    ElMessage.success('更新成功')
  } else {
    await hrApi.createLeaveType(submitData)
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

.leave-types-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.types-card {
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

.types-table {
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

.type-code {
  font-family: 'SF Mono', 'Monaco', monospace;
  font-weight: 600;
  font-size: 13px;
}

.type-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
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