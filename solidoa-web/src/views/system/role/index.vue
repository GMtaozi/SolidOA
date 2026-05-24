<template>
  <div class="role-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增角色
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handlePermission(row)">权限</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref([
  { id: 1, name: '系统管理员', code: 'SYSTEM_ADMIN', description: '系统最高权限', status: 1 },
  { id: 2, name: '部门经理', code: 'DEPT_MANAGER', description: '部门负责人', status: 1 },
  { id: 3, name: '普通员工', code: 'EMPLOYEE', description: '普通员工', status: 1 }
])

const handleAdd = () => {
  ElMessage.info('新增角色')
}

const handleEdit = (row) => {
  ElMessage.info('编辑角色: ' + row.name)
}

const handlePermission = (row) => {
  ElMessage.info('配置权限: ' + row.name)
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该角色吗？', '提示', { type: 'warning' })
  ElMessage.success('删除成功')
}
</script>

<style scoped lang="scss">
.role-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>