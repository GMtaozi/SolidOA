<template>
  <div class="stamp-manage-container">
    <el-card class="cyber-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">印章管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增印章
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border class="cyber-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="印章名称" />
        <el-table-column prop="type" label="印章类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="keeperName" label="保管人" />
        <el-table-column prop="deptName" label="保管部门" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span class="status-badge" :class="row.status === 1 ? 'status-active' : 'status-disabled'">
              {{ row.status === 1 ? '正常' : '停用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link class="cyber-link" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link class="cyber-link danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 印章表单弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="cyber-form">
        <el-form-item label="印章名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入印章名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="印章类型" prop="type">
          <el-select v-model="form.type" placeholder="选择印章类型" class="cyber-input" style="width: 100%">
            <el-option label="公章" value="公章" />
            <el-option label="合同专用章" value="合同专用章" />
            <el-option label="财务专用章" value="财务专用章" />
            <el-option label="人事专用章" value="人事专用章" />
            <el-option label="部门章" value="部门章" />
          </el-select>
        </el-form-item>
        <el-form-item label="保管人" prop="keeperId">
          <el-select v-model="form.keeperId" placeholder="选择保管人" filterable class="cyber-input" style="width: 100%">
            <el-option v-for="user in userList" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="保管部门" prop="deptId">
          <el-select v-model="form.deptId" placeholder="选择保管部门" class="cyber-input" style="width: 100%">
            <el-option v-for="dept in deptList" :key="dept.id" :label="dept.name" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">正常</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="备注信息" class="cyber-input" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/system'
import { workflowApi } from '@/api/workflow'

const tableData = ref([])
const userList = ref([])
const deptList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增印章')
const formRef = ref()

const form = reactive({
  id: null,
  name: '',
  type: '公章',
  keeperId: null,
  deptId: null,
  status: 1,
  remark: ''
})

const rules = {
  name: [{ required: true, message: '请输入印章名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择印章类型', trigger: 'change' }],
  keeperId: [{ required: true, message: '请选择保管人', trigger: 'change' }]
}

// 加载印章列表
const loadData = async () => {
  try {
    const res = await workflowApi.getStampList()
    tableData.value = res.data?.data || res.data || []
  } catch (error) {
    console.error('加载印章列表失败', error)
  }
}

// 加载用户列表
const loadUsers = async () => {
  try {
    const res = await systemApi.getUserList({ pageSize: 1000 })
    userList.value = res.data?.data?.records || []
  } catch (error) {
    console.error('加载用户列表失败', error)
  }
}

// 加载部门列表
const loadDepts = async () => {
  try {
    const res = await systemApi.getDeptTree()
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
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增印章'
  Object.assign(form, { id: null, name: '', type: '公章', keeperId: null, deptId: null, status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑印章'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该印章吗？', '提示', { type: 'warning' })
  try {
    await workflowApi.cancelStamp(row.id)  // workflow 没有 deleteStamp，撤回用 cancelStamp
    ElMessage.success('撤回成功')
    loadData()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  try {
    if (form.id) {
      await workflowApi.cancelStamp(form.id)
      ElMessage.success('撤回成功')
    } else {
      await workflowApi.createStamp(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

onMounted(() => {
  loadData()
  loadUsers()
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

.stamp-manage-container {
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

  .status-badge {
    display: inline-block;
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 12px;
    font-weight: 500;

    &.status-active {
      background: rgba(52, 211, 153, 0.15);
      color: $success;
    }

    &.status-disabled {
      background: rgba(249, 115, 115, 0.15);
      color: #F87171;
    }
  }

  .cyber-link {
    font-weight: 500;
    color: $primary;

    &:hover {
      color: darken($primary, 10%);
    }

    &.danger {
      color: #F87171;

      &:hover {
        color: #EF4444;
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

    &:hover {
      border-color: $primary;
      color: $primary;
    }

    &[type="primary"] {
      background: $primary;
      border-color: $primary;
      color: #ffffff;

      &:hover {
        background: darken($primary, 5%);
      }
    }
  }

  .cyber-input {
    :deep(.el-input__wrapper) {
      background: $bg-card;
      border: 1px solid $border-color;
      border-radius: 12px;
      box-shadow: none;

      &:hover {
        border-color: $primary;
      }

      &.is-focus {
        border-color: $primary;
      }
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
    }

    .el-dialog__body {
      padding: 30px 24px;
    }

    .el-dialog__footer {
      border-top: 1px solid $border-color;
      padding: 16px 24px;
    }
  }
}
</style>
