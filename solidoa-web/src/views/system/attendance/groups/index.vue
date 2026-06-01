<template>
  <div class="groups-container">
    <el-card class="groups-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">考勤组管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn primary" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增考勤组
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="考勤组名称" class="cyber-form-item">
          <el-input v-model="searchForm.name" placeholder="请输入考勤组名称" clearable class="cyber-input" />
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

      <el-table :data="tableData" border class="groups-table" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="考勤组名称" min-width="150">
          <template #default="{ row }">
            <div class="group-name-cell">
              <el-icon><Collection /></el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="关联班次" width="120">
          <template #default="{ row }">
            {{ row.shiftName || row.shiftId || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="考勤部门" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.deptNames || row.deptIds || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="考勤地点" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.locationName || row.location || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="考勤范围" width="100" align="center">
          <template #default="{ row }">
            {{ row.rangeType === 'GPS' ? 'GPS定位' : (row.rangeType === 'WIFI' ? 'WiFi打卡' : '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'status-active' : 'status-disabled'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link class="cyber-link" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link class="cyber-link danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 考勤组表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="cyber-form">
        <el-form-item label="考勤组名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入考勤组名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="关联班次" prop="shiftId">
          <el-select v-model="form.shiftId" placeholder="请选择班次" clearable class="cyber-select">
            <el-option v-for="shift in shiftList" :key="shift.id" :label="shift.name" :value="shift.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤部门" prop="deptIds">
          <el-select v-model="form.deptIds" placeholder="请选择考勤部门" multiple clearable class="cyber-select">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="考勤地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入考勤地点" class="cyber-input" />
        </el-form-item>
        <el-form-item label="经度" prop="longitude">
          <el-input-number v-model="form.longitude" :precision="6" :step="0.000001" :min="-180" :max="180" class="cyber-input-number" />
        </el-form-item>
        <el-form-item label="纬度" prop="latitude">
          <el-input-number v-model="form.latitude" :precision="6" :step="0.000001" :min="-90" :max="90" class="cyber-input-number" />
        </el-form-item>
        <el-form-item label="考勤范围" prop="rangeType">
          <el-radio-group v-model="form.rangeType">
            <el-radio label="GPS">GPS定位</el-radio>
            <el-radio label="WIFI">WiFi打卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="允许范围" prop="rangeRadius">
          <el-input-number v-model="form.rangeRadius" :min="0" :max="10000" class="cyber-input-number" />
          <span class="form-tip">米</span>
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
import { systemApi } from '@/api/system'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Collection } from '@element-plus/icons-vue'

const loading = ref(false)
const searchForm = reactive({
  name: '',
  status: null
})

const tableData = ref([])
const shiftList = ref([])
const deptList = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增考勤组')
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  shiftId: null,
  deptIds: [],
  location: '',
  longitude: null,
  latitude: null,
  rangeType: 'GPS',
  rangeRadius: 200,
  sortOrder: 0,
  status: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入考勤组名称', trigger: 'blur' }],
  rangeType: [{ required: true, message: '请选择考勤方式', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await hrApi.getGroups()
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
    console.error('加载考勤组数据失败', error)
    tableData.value = []
  } finally {
    loading.value = false
  }
}

const loadShifts = async () => {
  try {
    const res = await hrApi.getShifts()
    shiftList.value = res.data?.data || res.data || []
  } catch (error) {
    console.error('加载班次列表失败', error)
    shiftList.value = []
  }
}

const loadDepts = async () => {
  try {
    const res = await systemApi.getDeptList()
    const flattenDepts = (depts) => {
      const result = []
      const traverse = (nodes) => {
        nodes.forEach(node => {
          result.push({ id: node.id, name: node.name })
          if (node.children?.length) traverse(node.children)
        })
      }
      traverse(depts)
      return result
    }
    deptList.value = flattenDepts(res.data?.data || res.data || [])
  } catch (error) {
    console.error('加载部门列表失败', error)
    deptList.value = []
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
  dialogTitle.value = '新增考勤组'
  Object.assign(form, {
    id: null,
    name: '',
    shiftId: null,
    deptIds: [],
    location: '',
    longitude: null,
    latitude: null,
    rangeType: 'GPS',
    rangeRadius: 200,
    sortOrder: 0,
    status: 1,
    remark: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑考勤组'
  Object.assign(form, {
    id: row.id,
    name: row.name || '',
    shiftId: row.shiftId || null,
    deptIds: row.deptIds || [],
    location: row.location || '',
    longitude: row.longitude || null,
    latitude: row.latitude || null,
    rangeType: row.rangeType || 'GPS',
    rangeRadius: row.rangeRadius || 200,
    sortOrder: row.sortOrder || 0,
    status: row.status || 1,
    remark: row.remark || ''
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确定要删除考勤组「${row.name}」吗？`, '提示', { type: 'warning' })
  await hrApi.deleteGroup(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()

  const submitData = { ...form }
  if (form.id) {
    await hrApi.updateGroup(form.id, submitData)
    ElMessage.success('更新成功')
  } else {
    await hrApi.createGroup(submitData)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
  loadShifts()
  loadDepts()
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

.groups-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

.groups-card {
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

.groups-table {
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

.group-name-cell {
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
  width: 200px;
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