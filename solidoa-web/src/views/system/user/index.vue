<template>
  <div class="user-container">
    <el-card class="cyber-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">用户管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="用户名" class="cyber-form-item">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item label="姓名" class="cyber-form-item">
          <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable class="cyber-input" />
        </el-form-item>
        <el-form-item>
          <el-button class="cyber-btn" type="primary" @click="handleSearch">查询</el-button>
          <el-button class="cyber-btn" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <OaTable
        :data="tableData"
        :columns="columns"
        :total="pagination.total"
        :page="pagination.pageNum"
        :size="pagination.pageSize"
        @update:page="p => { pagination.pageNum = p; loadData() }"
        @update:size="s => { pagination.pageSize = s; pagination.pageNum = 1; loadData() }"
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
          <OaButton variant="ghost" size="small" @click="handleResetPassword(row)">重置密码</OaButton>
          <OaButton variant="danger" size="small" @click="handleDelete(row)">删除</OaButton>
        </template>
      </OaTable>
    </el-card>

    <!-- 用户表单弹窗 -->
    <OaFormDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      :model="form"
      :rules="rules"
      :on-submit="handleSubmit"
      @success="handleSuccess"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="!!form.id" class="cyber-input" />
      </el-form-item>
      <el-form-item label="姓名" prop="realName">
        <el-input v-model="form.realName" class="cyber-input" />
      </el-form-item>
      <el-form-item label="手机号" prop="mobile">
        <el-input v-model="form.mobile" class="cyber-input" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" class="cyber-input" />
      </el-form-item>
    </OaFormDialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { systemApi } from '@/api/system'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const userStore = useUserStore()

const searchForm = reactive({
  username: '',
  realName: ''
})

const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

// 表格列定义
const columns = [
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'username', label: '用户名', width: 140 },
  { prop: 'realName', label: '姓名', width: 120 },
  { prop: 'mobile', label: '手机号', width: 140 },
  { prop: 'email', label: '邮箱', minWidth: 200 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'createTime', label: '创建时间', minWidth: 180 }
]

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const form = reactive({
  id: null,
  username: '',
  realName: '',
  mobile: '',
  email: '',
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

const loadData = async () => {
  try {
    const res = await systemApi.getUserList({
      ...searchForm,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize
    })
    // 注意：res.data 是后端的 Result 对象 {code, message, data}
    // res.data.data 才是分页数据 {records, total, ...}
    tableData.value = res.data?.data?.records || []
    pagination.total = res.data?.data?.total || 0
  } catch (error) {
    console.error('加载数据失败', error)
  }
}

const handleSearch = () => {
  pagination.pageNum = 1
  loadData()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  handleSearch()
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  Object.assign(form, { id: null, username: '', realName: '', mobile: '', email: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  // 安全校验：禁止删除自己
  const currentUserId = userStore.userInfo?.id
  if (row.id === currentUserId) {
    ElMessage.warning('不能删除当前登录用户')
    return
  }

  // 安全校验：禁止删除管理员账户
  const isAdmin = userStore.roles?.some(r => {
    const roleCode = typeof r === 'string' ? r : r.code
    return roleCode === 'SYSTEM_ADMIN' || roleCode === 'admin' || roleCode === 'system_admin'
  })
  if (isAdmin && row.username === 'admin') {
    ElMessage.warning('不能删除系统管理员账户')
    return
  }

  await ElMessageBox.confirm(`确定要删除用户"${row.realName || row.username}"吗？删除后将无法恢复。`, '删除用户', { type: 'warning' })
  try {
    await systemApi.deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除用户失败', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

const handleResetPassword = async (row) => {
  await ElMessageBox.confirm(
    `确定要重置用户"${row.realName || row.username}"的密码吗？重置后将显示新密码。`,
    '重置密码',
    { type: 'warning' }
  )
  try {
    const res = await systemApi.resetPassword(row.id)
    const newPassword = res.data?.data
    if (newPassword) {
      await ElMessageBox.alert(
        `<div style="line-height: 1.8;">
          <p>用户 <strong>${row.realName || row.username}</strong> 的密码已重置成功！</p>
          <p>新密码：<code style="background: #f5f5f5; padding: 4px 8px; border-radius: 4px; font-size: 14px; color: #e6a23c;">${newPassword}</code></p>
          <p style="color: #999; font-size: 12px;">请妥善保管此密码，关闭后将无法再次查看。</p>
        </div>`,
        '密码重置成功',
        { dangerouslyUseHTMLString: true, confirmButtonText: '我已记录' }
      )
    } else {
      ElMessage.success('密码重置成功')
    }
    loadData()
  } catch (error) {
    console.error('重置密码失败', error)
    ElMessage.error('重置密码失败，请稍后重试')
  }
}

const handleSubmit = async (formData) => {
  if (formData.id) {
    await systemApi.updateUser(formData.id, formData)
    return '更新成功'
  } else {
    await systemApi.createUser(formData)
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

.user-container {
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

  .cyber-pagination {
    margin-top: 24px;
    justify-content: flex-end;

    :deep(.el-pagination__total) {
      color: $text-secondary;
    }

    :deep(.el-pager li) {
      background: $bg-card;
      color: $text-secondary;
      border: 1px solid $border-color;
      margin: 0 4px;
      border-radius: 12px;
      min-width: 36px;
      height: 36px;
      line-height: 36px;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

      &:hover {
        color: $primary;
        border-color: $primary;
      }

      &.is-active {
        background: $primary;
        color: #ffffff;
        border-color: $primary;
        box-shadow: 0 4px 12px rgba(96, 165, 250, 0.3);
      }
    }

    :deep(.btn-prev),
    :deep(.btn-next) {
      background: $bg-card;
      border: 1px solid $border-color;
      border-radius: 12px;
      color: $text-secondary;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05), 0 8px 10px -6px rgba(0, 0, 0, 0.02);

      &:hover {
        color: $primary;
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

  .cyber-input {
    :deep(.el-input__wrapper) {
      background: $bg-card;
      border: 1px solid $border-color;
      border-radius: 12px;
      box-shadow: none;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
      padding: 8px 16px;

      &:hover {
        border-color: $primary;
      }

      &.is-focus {
        border-color: $primary;
        box-shadow: 0 0 0 3px rgba(96, 165, 250, 0.1);
      }
    }

    :deep(.el-input__inner) {
      color: $text-primary;

      &::placeholder {
        color: $text-secondary;
      }
    }
  }
}

.cyber-dialog {
  :deep(.el-dialog) {
    background: $bg-card;
    border-radius: 16px;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15);

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
</style>