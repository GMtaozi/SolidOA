<template>
  <div class="oa-pagination">
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      :background="background"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'

defineOptions({ name: 'OaPagination' })

const props = defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },
  background: { type: Boolean, default: true }
})

const emit = defineEmits(['update:page', 'update:size', 'change'])

const currentPage = computed({
  get: () => props.page,
  set: v => emit('update:page', v)
})

const pageSize = computed({
  get: () => props.size,
  set: v => emit('update:size', v)
})

function handleSizeChange(size) {
  emit('update:size', size)
  emit('change', { page: 1, size })
}

function handleCurrentChange(page) {
  emit('update:page', page)
  emit('change', { page, size: props.size })
}
</script>

<style lang="scss" scoped>
.oa-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0;
}
</style>
