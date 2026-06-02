<template>
  <el-form :model="model" :inline="inline" class="oa-search-form" @submit.prevent="handleSearch">
    <slot :model="model" />
    <el-form-item>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
      <el-button :icon="Refresh" @click="handleReset">重置</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { Search, Refresh } from '@element-plus/icons-vue'

defineOptions({ name: 'OaSearchForm' })

const props = defineProps({
  model: { type: Object, required: true },
  inline: { type: Boolean, default: true }
})

const emit = defineEmits(['search', 'reset'])

function handleSearch() {
  emit('search', props.model)
}

function handleReset() {
  Object.keys(props.model).forEach(k => {
    const v = props.model[k]
    if (Array.isArray(v)) props.model[k] = []
    else if (typeof v === 'string') props.model[k] = ''
    else if (typeof v === 'number') props.model[k] = 0
    else props.model[k] = null
  })
  emit('reset', props.model)
}
</script>

<style lang="scss" scoped>
.oa-search-form {
  background: #ffffff;
  padding: 16px 20px;
  border-radius: 16px;
  margin-bottom: 16px;
  border: 1px solid #F0EDE9;
}
</style>
