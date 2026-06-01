<template>
  <div class="holidays-container">
    <el-card class="holidays-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">节假日配置</span>
            <span class="title-line"></span>
          </div>
          <div class="header-actions">
            <el-button class="cyber-btn" @click="handleImport">
              <el-icon><Upload /></el-icon>
              批量导入
            </el-button>
            <el-button class="cyber-btn primary" type="primary" @click="handleAdd">
              <el-icon><Plus /></el-icon>
              新增节假日
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="节假日名称" class="cyber-form-item">
          <el-input v-model="searchForm.name" placeholder="请输入节假日名称" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="年份" class="cyber-form-item">
          <el-select v-model="searchForm.year" placeholder="请选择年份" clearable class="cyber-select">
            <el-option v-for="year in yearOptions" :key="year" :label="year + '年'" :value="year" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型" class="cyber-form-item">
          <el-select v-model="searchForm.type" placeholder="全部" clearable class="cyber-select">
            <el-option label="法定节假日" value="LEGAL" />
            <el-option label="调休工作日" value="WORKDAY" />
            <el-option label="公司福利假" value="COMPANY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button class="cyber-btn" type="primary" @click="handleSearch">查询</el-button>
          <el-button class="cyber-btn" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border class="holidays-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="节假日名称" min-width="150">
          <template #default="{ row }">
            <div class="holiday-name-cell">
              <el-icon><Calendar /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="日期" width="180">
          <template #default="{ row }">
            <div class="date-cell">
              {{ row.date }}
              <span v-if="row.endDate && row.endDate !== row.date" class="date-range">至 {{ row.endDate }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="天数" width="80" align="center">
          <template #default="{ row }">
            {{ row.days || 1 }}天
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <span class="type-badge" :class="getTypeClass(row.type)">
              {{ getTypeName(row.type) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="year" label="年份" width="80" align="center" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
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

    <!-- 节假日表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="cyber-form">
        <el-form-item label="节假日名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入节假日名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="开始日期" prop="date">
          <el-date-picker
            v-model="form.date"
            type="date"
            placeholder="选择开始日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="cyber-date-picker"
          />
        </el-form-item>
        <el-form-item label="结束日期" prop="endDate">
          <el-date-picker
            v-model="form.endDate"
            type="date"
            placeholder="选择结束日期（可选）"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            class="cyber-date-picker"
          />
          <span class="form-tip">不填表示单天</span>
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio label="LEGAL">法定节假日</el-radio>
            <el-radio label="WORKDAY">调休工作日</el-radio>
            <el-radio label="COMPANY">公司福利假</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年份" prop="year">
          <el-select v-model="form.year" placeholder="请选择年份" class="cyber-select">
            <el-option v-for="year in yearOptions" :key="year" :label="year + '年'" :value="year" />
          </el-select>
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

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="批量导入节假日" width="500px" class="cyber-dialog">
      <div class="import-template">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls,.csv"
          :on-change="handleFileChange"
          drag
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将文件拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="upload-tip">支持 .xlsx、.xls、.csv 格式，请先下载模板填写</div>
          </template>
        </el-upload>
        <el-button class="cyber-btn download-btn" @click="handleDownloadTemplate">
          <el-icon><Download /></el-icon>
          下载导入模板
        </el-button>
      </div>
      <template #footer>
        <el-button class="cyber-btn" @click="importDialogVisible = false">取消</el-button>
        <el-button class="cyber-btn primary" type="primary" @click="handleImportSubmit" :loading="importLoading">确定导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { hrApi } from '@/api/hr'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Calendar, Upload, UploadFilled, Download } from '@element-plus/icons-vue'

const loading = ref(false)
const importLoading = ref(false)
const uploadRef = ref()
const importFile = ref(null)

const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  const years = []
  for (let i = currentYear - 2; i <= currentYear + 1; i++) {
    years.push(i)
  }
  return years
})

const searchForm = reactive({
  name: '',
  year: currentYear,
  type: null
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

const dialogVisible = ref(false)
const importDialogVisible = ref(false)
const dialogTitle = ref('新增节假日')
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  date: '',
  endDate: '',
  type: 'LEGAL',
  year: currentYear,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入节假日名称', trigger: 'blur' }],
  date: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  year: [{ required: true, message: '请选择年份', trigger: 'change' }]
}

const getTypeName = (type) => {
  const typeMap = { 'LEGAL': '法定节假日', 'WORKDAY': '调休工作日', 'COMPANY': '公司福利假' }
  return typeMap[type] || type
}

const getTypeClass = (type) => {
  const classMap = { 'LEGAL': 'type-legal', 'WORKDAY': 'type-workday', 'COMPANY': 'type-company' }
  return classMap[type] || ''
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {}
    if (searchForm.year) params.year = searchForm.year
    if (searchForm.type) params.type = searchForm.type
    const res = await hrApi.getHolidays(params)
    let data = res.data?.data || res.data || []
    if (!Array.isArray(data)) data = []
    if (searchForm.name) {
      data = data.filter(item => searchForm.name && item.name?.includes(searchForm.name))
    }
    pagination.total = data.length
    const start = (pagination.pageNum - 1) * pagination.pageSize
    tableData.value = data.slice(start, start + pagination.pageSize)
  } catch (error) {
    console.error('加载节假日数据失败', error)
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
  searchForm.year = currentYear
  searchForm.type = null
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增节假日'
  Object.assign(form, {
    id: null,
    name: '',
    date: '',
    endDate: '',
    type: 'LEGAL',
    year: currentYear,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑节假日'
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    date: row.date || '',
    endDate: row.endDate || '',
    type: row.type || 'LEGAL',
    year: row.year || currentYear,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除节假日「${row.name}」吗？`, '提示', { type: 'warning' })
  await hrApi.deleteHoliday(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()

  const submitData = { ...form }
  if (form.id) {
    await hrApi.updateHoliday(form.id, submitData)
    ElMessage.success('更新成功')
  } else {
    await hrApi.createHoliday(submitData)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadData()
}

const handleImport = () => {
  importFile.value = null
  importDialogVisible.value = true
}

const handleFileChange = (file) => {
  importFile.value = file.raw
}

const handleDownloadTemplate = () => {
  ElMessage.info('模板下载功能开发中，请手动创建导入文件')
}

const handleImportSubmit = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }
  importLoading.value = true
  try {
    ElMessage.success('导入功能开发中')
    importDialogVisible.value = false
  } catch (error) {
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
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

.holidays-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.holidays-card {
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

  .header-actions {
    display: flex;
    gap: 12px;
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

.holidays-table {
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

.holiday-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  color: $primary;
}

.date-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;

  .date-range {
    font-size: 12px;
    color: $text-secondary;
  }
}

.type-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;

  &.type-legal {
    background: rgba($success, 0.15);
    color: $success;
  }

  &.type-workday {
    background: rgba(#FBBF24, 0.15);
    color: #FBBF24;
  }

  &.type-company {
    background: rgba($primary, 0.15);
    color: $primary;
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
  width: 150px;
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
  }
}

.cyber-date-picker {
  width: 180px;
  :deep(.el-input__wrapper) {
    border-radius: 12px;
    padding: 8px 16px;
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

.import-template {
  .upload-icon {
    font-size: 48px;
    color: $text-secondary;
    margin-bottom: 16px;
  }

  .upload-text {
    color: $text-secondary;
    em {
      color: $primary;
      font-style: normal;
    }
  }

  .upload-tip {
    margin-top: 8px;
    font-size: 12px;
    color: $text-secondary;
  }

  .download-btn {
    margin-top: 20px;
  }
}
</style>