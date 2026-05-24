<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>报销管理</span>
        <el-button type="primary">新增报销</el-button>
      </div>
    </template>
    <el-empty description="暂无报销记录" />
  </el-card>
</template>

<script setup>
</script>

<style scoped lang="scss">
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>