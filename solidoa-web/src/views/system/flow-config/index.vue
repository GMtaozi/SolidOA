<template>
  <div class="flow-config-container">
    <el-card class="cyber-card">
      <template #header>
        <div class="card-header">
          <div class="title-wrapper">
            <span class="title-text">流程配置</span>
            <span class="title-line"></span>
          </div>
          <el-button class="cyber-btn" type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增流程
          </el-button>
        </div>
      </template>

      <!-- 业务类型筛选 -->
      <el-tabs v-model="activeTab" @tab-change="loadData">
        <el-tab-pane label="请假审批" name="LEAVE" />
        <el-tab-pane label="报销审批" name="EXPENSE" />
        <el-tab-pane label="采购审批" name="PURCHASE" />
        <el-tab-pane label="用印审批" name="STAMP" />
        <el-tab-pane label="加班审批" name="OVERTIME" />
        <el-tab-pane label="出差审批" name="BUSINESS_TRIP" />
      </el-tabs>

      <!-- 流程列表 -->
      <el-table :data="tableData" border class="cyber-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="flowName" label="流程名称" />
        <el-table-column prop="isDefault" label="默认流程" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isDefault ? 'success' : 'info'" size="small">
              {{ row.isDefault ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button type="primary" link class="cyber-link" @click="handleEdit(row)">编辑</el-button>
            <el-button type="warning" link class="cyber-link warning" @click="handleSetDefault(row)" v-if="!row.isDefault">设为默认</el-button>
            <el-button type="danger" link class="cyber-link danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 流程配置弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" class="cyber-dialog">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="cyber-form">
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="form.businessType" placeholder="选择业务类型" class="cyber-input" style="width: 100%">
            <el-option label="请假审批" value="LEAVE" />
            <el-option label="报销审批" value="EXPENSE" />
            <el-option label="采购审批" value="PURCHASE" />
            <el-option label="用印审批" value="STAMP" />
            <el-option label="加班审批" value="OVERTIME" />
            <el-option label="出差审批" value="BUSINESS_TRIP" />
          </el-select>
        </el-form-item>
        <el-form-item label="流程名称" prop="flowName">
          <el-input v-model="form.flowName" placeholder="请输入流程名称" class="cyber-input" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
        </el-form-item>

        <!-- 审批节点配置 -->
        <el-form-item label="审批节点">
          <div class="node-list">
            <div v-for="(node, index) in form.nodes" :key="index" class="node-item">
              <div class="node-header">
                <span class="node-order">节点 {{ index + 1 }}</span>
                <el-button type="danger" link @click="removeNode(index)" v-if="form.nodes.length > 1">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-input v-model="node.name" placeholder="节点名称" />
                </el-col>
                <el-col :span="8">
                  <el-select v-model="node.approverType" placeholder="审批人类型" @change="onApproverTypeChange(index)">
                    <el-option label="指定人员" value="FIXED" />
                    <el-option label="直属上级" value="DIRECT_MANAGER" />
                    <el-option label="角色" value="ROLE" />
                  </el-select>
                </el-col>
                <el-col :span="8">
                  <!-- 指定人员 -->
                  <el-select v-if="node.approverType === 'FIXED'" v-model="node.approverId" placeholder="选择审批人" filterable style="width: 100%">
                    <el-option v-for="user in userList" :key="user.id" :label="user.realName" :value="user.id" />
                  </el-select>
                  <!-- 角色 -->
                  <el-select v-else-if="node.approverType === 'ROLE'" v-model="node.roleCode" placeholder="选择角色" style="width: 100%">
                    <el-option v-for="role in roleList" :key="role.code" :label="role.name" :value="role.code" />
                  </el-select>
                  <!-- 直属上级 -->
                  <el-input v-else disabled value="自动获取" />
                </el-col>
              </el-row>
              <el-row :gutter="12" style="margin-top: 8px;">
                <el-col :span="12">
                  <el-select v-model="node.mode" placeholder="审批模式">
                    <el-option label="或签（一人通过即可）" value="ANY" />
                    <el-option label="会签（所有人通过）" value="ALL" />
                  </el-select>
                </el-col>
              </el-row>
            </div>
            <el-button type="dashed" @click="addNode" class="add-node-btn">
              <el-icon><Plus /></el-icon>
              添加审批节点
            </el-button>
          </div>
        </el-form-item>

        <!-- 抄送人配置 -->
        <el-form-item label="抄送人">
          <div class="cc-list">
            <div v-for="(cc, index) in form.ccUsers" :key="index" class="cc-item">
              <el-select v-model="cc.type" placeholder="抄送人类型" style="width: 150px">
                <el-option label="指定人员" value="FIXED" />
                <el-option label="直属上级" value="DIRECT_MANAGER" />
                <el-option label="部门管理员" value="DEPARTMENT_ADMIN" />
              </el-select>
              <el-select v-if="cc.type === 'FIXED'" v-model="cc.userId" placeholder="选择人员" filterable style="width: 200px; margin-left: 8px">
                <el-option v-for="user in userList" :key="user.id" :label="user.realName" :value="user.id" />
              </el-select>
              <el-button type="danger" link @click="removeCc(index)" style="margin-left: 8px">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="dashed" @click="addCc" class="add-node-btn">
              <el-icon><Plus /></el-icon>
              添加抄送人
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { workflowApi } from '@/api/workflow'
import { systemApi } from '@/api/system'

const activeTab = ref('LEAVE')
const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增流程')
const formRef = ref()
const userList = ref([])
const roleList = ref([])

const form = reactive({
  id: null,
  businessType: 'LEAVE',
  flowName: '',
  isDefault: false,
  nodes: [
    { order: 1, name: '直属领导审批', approverType: 'DIRECT_MANAGER', approverId: null, approverName: '', roleCode: '', mode: 'ANY' }
  ],
  ccUsers: []
})

const rules = {
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  flowName: [{ required: true, message: '请输入流程名称', trigger: 'blur' }]
}

// 加载流程列表
const loadData = async () => {
  try {
    const res = await workflowApi.getFlowConfigList(activeTab.value)
    tableData.value = res.data?.data || res.data || []
  } catch (error) {
    console.error('加载流程配置失败', error)
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

// 加载角色列表
const loadRoles = async () => {
  try {
    const res = await systemApi.getRoleList({ pageSize: 1000 })
    roleList.value = res.data?.data?.records || []
  } catch (error) {
    console.error('加载角色列表失败', error)
  }
}

// 新增流程
const handleAdd = () => {
  dialogTitle.value = '新增流程'
  Object.assign(form, {
    id: null,
    businessType: activeTab.value,
    flowName: '',
    isDefault: false,
    nodes: [
      { order: 1, name: '直属领导审批', approverType: 'DIRECT_MANAGER', approverId: null, approverName: '', roleCode: '', mode: 'ANY' }
    ],
    ccUsers: []
  })
  dialogVisible.value = true
}

// 编辑流程
const handleEdit = (row) => {
  dialogTitle.value = '编辑流程'
  const config = row.config ? JSON.parse(row.config) : {}
  Object.assign(form, {
    id: row.id,
    businessType: row.businessType,
    flowName: row.flowName,
    isDefault: row.isDefault,
    nodes: config.nodes || [],
    ccUsers: config.ccUsers || []
  })
  dialogVisible.value = true
}

// 设置默认流程
const handleSetDefault = async (row) => {
  try {
    await workflowApi.setDefaultFlow(row.id, row.businessType)
    ElMessage.success('设置成功')
    loadData()
  } catch (error) {
    ElMessage.error('设置失败')
  }
}

// 删除流程
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定要删除该流程配置吗？', '提示', { type: 'warning' })
  try {
    await workflowApi.deleteFlowConfig(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 添加节点
const addNode = () => {
  const order = form.nodes.length + 1
  form.nodes.push({
    order,
    name: `审批节点${order}`,
    approverType: 'DIRECT_MANAGER',
    approverId: null,
    approverName: '',
    roleCode: '',
    mode: 'ANY'
  })
}

// 删除节点
const removeNode = (index) => {
  form.nodes.splice(index, 1)
  // 重新排序
  form.nodes.forEach((node, i) => {
    node.order = i + 1
  })
}

// 审批人类型变更
const onApproverTypeChange = (index) => {
  form.nodes[index].approverId = null
  form.nodes[index].approverName = ''
  form.nodes[index].roleCode = ''
}

// 添加抄送人
const addCc = () => {
  form.ccUsers.push({ type: 'FIXED', userId: null, userName: '' })
}

// 删除抄送人
const removeCc = (index) => {
  form.ccUsers.splice(index, 1)
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  // 验证节点配置
  for (const node of form.nodes) {
    if (!node.name) {
      ElMessage.warning('请填写节点名称')
      return
    }
    if (node.approverType === 'FIXED' && !node.approverId) {
      ElMessage.warning('请选择审批人')
      return
    }
    if (node.approverType === 'ROLE' && !node.roleCode) {
      ElMessage.warning('请选择角色')
      return
    }
  }

  loading.value = true
  try {
    const data = {
      businessType: form.businessType,
      flowName: form.flowName,
      isDefault: form.isDefault,
      nodes: form.nodes,
      ccUsers: form.ccUsers
    }

    if (form.id) {
      await workflowApi.updateFlowConfig(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await workflowApi.createFlowConfig(data)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    loadData()
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  loadUsers()
  loadRoles()
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

.flow-config-container {
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
    margin-top: 16px;
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
        }
      }
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
    box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
      border-color: $primary;
      color: $primary;
      box-shadow: 0 4px 12px rgba(96, 165, 250, 0.15);
    }

    &.primary,
    &[type="primary"] {
      background: $primary;
      border-color: $primary;
      color: #ffffff;

      &:hover {
        background: darken($primary, 5%);
        border-color: darken($primary, 5%);
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
}

// 节点配置样式
.node-list {
  width: 100%;

  .node-item {
    background: #f9fafb;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 12px;

    .node-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .node-order {
        font-weight: 600;
        color: $primary;
      }
    }
  }

  .add-node-btn {
    width: 100%;
    border-style: dashed;
    border-color: $border-color;
    color: $text-secondary;

    &:hover {
      border-color: $primary;
      color: $primary;
    }
  }
}

// 抄送人配置样式
.cc-list {
  width: 100%;

  .cc-item {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
  }

  .add-node-btn {
    width: 100%;
    border-style: dashed;
    border-color: $border-color;
    color: $text-secondary;
    margin-top: 8px;

    &:hover {
      border-color: $primary;
      color: $primary;
    }
  }
}
</style>
