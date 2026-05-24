<template>
  <div class="dept-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" @click="handleAdd">
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
      >
        <template #default="{ node, data }">
          <span class="tree-node">
            <span>{{ data.name }}</span>
            <span class="tree-actions">
              <el-button type="primary" link @click.stop="handleEdit(data)">编辑</el-button>
              <el-button type="danger" link @click.stop="handleDelete(data)">删除</el-button>
            </span>
          </span>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="父部门">
          <el-input v-model="form.parentId" type="number" placeholder="0表示顶级部门" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input v-model="form.sort" type="number" />
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
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const treeData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const formRef = ref()
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
  // 实际从 API 加载
  treeData.value = [
    { id: 1, name: '总公司', children: [
      { id: 2, name: '技术部' },
      { id: 3, name: '销售部' },
      { id: 4, name: '财务部' }
    ]}
  ]
}

const handleAdd = () => {
  dialogTitle.value = '新增部门'
  Object.assign(form, { id: null, name: '', parentId: 0, sort: 0 })
  dialogVisible.value = true
}

const handleEdit = (data) => {
  dialogTitle.value = '编辑部门'
  Object.assign(form, { id: data.id, name: data.name, parentId: data.parentId, sort: data.sort })
  dialogVisible.value = true
}

const handleDelete = async (data) => {
  await ElMessageBox.confirm('确定要删除该部门吗？', '提示', { type: 'warning' })
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  await formRef.value.validate()
  ElMessage.success(form.id ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.dept-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .tree-node {
    display: flex;
    justify-content: space-between;
    width: 100%;
    padding-right: 20px;

    .tree-actions {
      display: flex;
      gap: 10px;
    }
  }
}
</style>