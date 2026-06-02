<template>
  <div class="role-container">
    <el-card class="cyber-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">角色管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <OaTable
        :data="tableData"
        :columns="columns"
        :total="total"
        :page="query.page"
        :size="query.size"
        :show-pagination="false"
      >
        <template #status="{ row }">
          <OaStatusBadge
            :type="row.status === 1 ? 'success' : 'default'"
            :text="row.status === 1 ? '启用' : '禁用'"
            :dot="false"
          />
        </template>
        <template #actions="{ row }">
          <OaButton variant="primary" size="small" @click="handleEdit(row)">编辑</OaButton>
          <OaButton variant="ghost" size="small" @click="handlePermission(row)">权限</OaButton>
          <OaButton variant="danger" size="small" @click="handleDelete(row)">删除</OaButton>
        </template>
      </OaTable>
    </el-card>

    <!-- 角色表单弹窗 -->
    <OaFormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :model="form"
      :rules="rules"
      :on-submit="handleSubmit"
      @success="handleSuccess"
    >
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" class="cyber-input" />
      </el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" class="cyber-input" />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="form.description" type="textarea" :rows="3" class="cyber-input" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </OaFormDialog>

    <!-- 权限配置弹窗 -->
    <el-dialog v-model="permissionVisible" title="配置权限" width="500px" class="cyber-dialog">
      <el-tree
        ref="treeRef"
        :data="permissionTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        :default-expand-all="true"
        show-checkbox
        class="permission-tree"
      />
      <template #footer>
        <el-button class="cyber-btn" @click="permissionVisible = false">取消</el-button>
        <el-button class="cyber-btn primary" type="primary" @click="handlePermissionSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/system'

const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const loading = ref(false)

// 表格列定义
const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '角色名称', minWidth: 140 },
  { prop: 'code', label: '角色编码', minWidth: 140 },
  { prop: 'description', label: '描述', minWidth: 200 },
  { prop: 'status', label: '状态', width: 100 }
]

const dialogVisible = ref(false)
const permissionVisible = ref(false)
const dialogTitle = ref('新增角色')
const treeRef = ref()

const form = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const permissionTree = ref([])
const currentRoleId = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const res = await systemApi.getRoleList()
    // res.data 是 Result 对象 {code, message, data}
    // res.data.data 才是分页数据 {records, total, ...}
    tableData.value = res.data?.data?.records || res.data?.data || []
  } catch (error) {
    console.error('加载角色列表失败', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增角色'
  Object.assign(form, { id: null, name: '', code: '', description: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑角色'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const handlePermission = async (row) => {
  currentRoleId.value = row.id
  try {
    const res = await systemApi.getRolePermissions(row.id)
    permissionTree.value = res.data?.data || res.data || []
    permissionVisible.value = true
    // 设置已有权限的选中状态
    if (treeRef.value) {
      const checkedIds = flattenPermissions(res.data?.data || res.data)
      treeRef.value.setCheckedKeys(checkedIds)
    }
  } catch (error) {
    console.error('加载权限列表失败', error)
    ElMessage.error('加载权限失败')
  }
}

// 扁平化权限树获取已选中的权限ID
const flattenPermissions = (nodes) => {
  const result = []
  const traverse = (items) => {
    for (const item of items) {
      // 使用 getCheckedKeys(true) 获取半选中和选中状态的节点
      if (item.checked || item.indeterminate) {
        result.push(item.id)
      }
      if (item.children && item.children.length > 0) {
        traverse(item.children)
      }
    }
  }
  traverse(nodes)
  return result
}

const handlePermissionSubmit = async () => {
  if (!treeRef.value) return
  // 使用 getCheckedKeys(true) 获取所有选中和半选中的权限
  const checkedKeys = treeRef.value.getCheckedKeys(true)
  try {
    await systemApi.assignPermissions(currentRoleId.value, { permissionIds: checkedKeys })
    ElMessage.success('权限配置成功')
    permissionVisible.value = false
  } catch (error) {
    console.error('保存权限失败', error)
    ElMessage.error('保存权限失败')
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
  try {
    await systemApi.deleteRole(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除角色失败', error)
    ElMessage.error('删除失败')
  }
}

const handleSubmit = async (formData) => {
  if (formData.id) {
    await systemApi.updateRole(formData.id, formData)
    return '更新成功'
  } else {
    await systemApi.createRole(formData)
    return '新增成功'
  }
}

const handleSuccess = (msg) => {
  ElMessage.success(msg || '操作成功')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
// 柔和舒适风格配色变量
$bg-primary: #f7f5f2;
$bg-card: #ffffff;
$primary: #60A5FA;
$success: #34D399;
$warning: #FBBF24;
$text-primary: #3B3B3B;
$text-secondary: #9CA3AF;
$border-color: #F0EDE9;

.role-container {
  min-height: 100vh;
  padding: 20px;
  background: $bg-primary;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;

  .cyber-card {
    background: $bg-card;
    border-radius: 16px;
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
    transition: box-shadow 0.3s ease;

    &:hover {
      box-shadow: 0 20px 35px -12px rgba(0, 0, 0, 0.1);
    }

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
        font-size: 14px;
        border: none;
        padding: 16px 12px;

        .cell {
          padding: 0 8px;
        }
      }
    }

    :deep(.el-table__body-wrapper) {
      tr {
        background: $bg-card;
        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

        &:hover > td {
          background: #fcfaf7;
        }

        td {
          border: none;
          padding: 16px 12px;
          color: $text-primary;

          .cell {
            padding: 0 8px;
          }
        }
      }
    }

    :deep(.el-table__row) {
      &:hover {
        background: #fcfaf7;
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
      color: #FBBF24;
    }
  }

  .cyber-link {
    font-weight: 500;
    color: $primary;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      color: darken($primary, 10%);
    }

    &.warning {
      color: $warning;

      &:hover {
        color: darken($warning, 10%);
      }
    }

    &.danger {
      color: #FBBF24;

      &:hover {
        color: #ea580c;
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
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      border-color: $primary;
      color: $primary;
      box-shadow: 0 4px 12px rgba(96, 165, 250, 0.15);
    }

    &.primary {
      background: $primary;
      border-color: $primary;
      color: #ffffff;

      &:hover {
        background: darken($primary, 5%);
        border-color: darken($primary, 5%);
        box-shadow: 0 6px 16px rgba(96, 165, 250, 0.3);
      }
    }
  }
}
</style>