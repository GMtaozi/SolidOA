<template>
  <div class="dept-container">
    <el-card class="cyber-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">部门管理</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增部门
          </el-button>
        </div>
      </template>

      <el-tree
        :data="treeData"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        default-expand-all
        highlight-current
        @current-change="handleNodeClick"
        class="cyber-tree"
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span class="node-icon">
              <el-icon><OfficeBuilding /></el-icon>
            </span>
            <span class="node-label">{{ data.name }}</span>
            <span class="tree-actions">
              <el-button type="primary" link class="cyber-link" @click.stop="handleEdit(data)">编辑</el-button>
              <el-button type="success" link class="cyber-link success" @click.stop="handleAddChild(data)">添加子部门</el-button>
              <el-button type="danger" link class="cyber-link danger" @click.stop="handleDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="cyber-form">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" class="cyber-input" />
        </el-form-item>
        <el-form-item label="父部门">
          <el-select v-model="form.parentId" placeholder="选择父部门" clearable class="cyber-input" style="width: 100%">
            <el-option :value="0" label="无（顶级部门）" />
            <el-option
              v-for="dept in flatDeptList"
              :key="dept.id"
              :value="dept.id"
              :label="dept.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="999" class="cyber-input" />
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
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '@/api/system'

const treeData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const formRef = ref()
const selectedDept = ref(null) // 当前选中的部门

// 父部门选择框选项
const deptOptions = computed(() => {
  return [{ id: 0, name: '无（顶级部门）', children: treeData.value }]
})

// 扁平化部门列表（用于下拉选择）
const flatDeptList = computed(() => {
  const result = []
  const flatten = (depts, level = 0) => {
    depts.forEach(dept => {
      result.push({ id: dept.id, name: '　'.repeat(level) + dept.name })
      if (dept.children?.length) {
        flatten(dept.children, level + 1)
      }
    })
  }
  flatten(treeData.value)
  return result
})

const form = reactive({
  id: null,
  name: '',
  parentId: 0,
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

const loadData = async () => {
  try {
    const res = await systemApi.getDeptTree()
    treeData.value = res.data?.data || res.data || []
  } catch (e) {
    console.error('加载部门数据失败', e)
    treeData.value = []
  }
}

// 点击树节点选中
const handleNodeClick = (data) => {
  selectedDept.value = data
}

const handleAdd = () => {
  dialogTitle.value = '新增顶级部门'
  Object.assign(form, { id: null, name: '', parentId: 0, sort: 0 })
  dialogVisible.value = true
}

// 添加子部门（自动设置父部门）
const handleAddChild = (data) => {
  dialogTitle.value = `添加子部门 - ${data.name}`
  Object.assign(form, { id: null, name: '', parentId: data.id, sort: 0 })
  dialogVisible.value = true
}

const handleEdit = (data) => {
  dialogTitle.value = '编辑部门'
  Object.assign(form, { id: data.id, name: data.name, parentId: data.parentId, sort: data.sort })
  dialogVisible.value = true
}

const handleDelete = async (data) => {
  await ElMessageBox.confirm('确定要删除该部门吗？', '提示', { type: 'warning' })
  try {
    await systemApi.deleteDept(data.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    console.error('删除部门失败', e)
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (form.id) {
      await systemApi.updateDept(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await systemApi.createDept(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    console.error('保存部门失败', e)
  }
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

.dept-container {
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

  .cyber-tree {
    background: $bg-card;
    border: 1px solid $border-color;
    border-radius: 16px;
    padding: 16px;

    :deep(.el-tree-node__content) {
      background: transparent;
      height: 48px;
      border-radius: 12px;
      transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

      &:hover {
        background: #fcfaf7;
      }
    }

    :deep(.el-tree-node__children) {
      .el-tree-node__content {
        margin-left: 16px;
        padding-left: 16px;
      }
    }
  }

  .tree-node {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    padding-right: 20px;

    .node-icon {
      color: $primary;
      font-size: 18px;
    }

    .node-label {
      color: $text-primary;
      font-weight: 500;
      flex: 1;
    }

    .tree-actions {
      display: flex;
      gap: 12px;
    }
  }

  .cyber-link {
    font-weight: 500;
    color: $primary;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      color: darken($primary, 10%);
    }

    &.success {
      color: $success;

      &:hover {
        color: darken($success, 10%);
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