<template>
  <div class="contact-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>通讯录</span>
          <el-input
            v-model="keyword"
            placeholder="搜索姓名、手机号、职位"
            style="width: 300px"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </template>

      <el-table :data="tableData" border>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="deptName" label="部门" />
        <el-table-column prop="position" label="职位" />
        <el-table-column prop="mobile" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleCall(row)">拨号</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        layout="total, prev, pager, next"
        style="margin-top: 20px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'

const keyword = ref('')
const tableData = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

const handleSearch = () => {
  // 实际从 API 搜索
  tableData.value = [
    { id: 1, realName: '张三', deptName: '技术部', position: '开发工程师', mobile: '13800138000', email: 'zhangsan@solidoa.com' },
    { id: 2, realName: '李四', deptName: '销售部', position: '销售经理', mobile: '13800138001', email: 'lisi@solidoa.com' }
  ]
  pagination.total = 2
}

const handleCall = (row) => {
  console.log('拨号', row.mobile)
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped lang="scss">
.contact-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>