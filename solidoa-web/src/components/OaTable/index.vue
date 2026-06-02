<template>
  <div class="oa-table-wrapper">
    <!-- 顶部工具栏 -->
    <div v-if="$slots.toolbar" class="oa-table-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 表格 -->
    <el-table
      :data="data"
      v-loading="loading"
      :stripe="stripe"
      :border="border"
      :empty-text="emptyText"
      class="oa-table"
      @row-click="row => $emit('row-click', row)"
    >
      <!-- 选择列 -->
      <el-table-column v-if="selection" type="selection" width="48" />
      <!-- 序号列 -->
      <el-table-column v-if="index" type="index" label="#" width="56" align="center" />

      <!-- 动态列 -->
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align || 'left'"
        :sortable="col.sortable"
        :show-overflow-tooltip="col.tooltip"
      >
        <template #default="scope">
          <slot :name="col.prop" :row="scope.row" :index="getRealIndex(scope.$index)">
            <span :class="col.cellClass">{{ formatCell(col, scope.row) }}</span>
          </slot>
        </template>
      </el-table-column>

      <!-- 操作列 -->
      <el-table-column v-if="$slots.actions" label="操作" :width="actionWidth" :fixed="actionFixed" align="center">
        <template #default="{ row }">
          <slot name="actions" :row="row" />
        </template>
      </el-table-column>

      <!-- 空数据 -->
      <template #empty>
        <div class="oa-table__empty">
          <slot name="empty">
            <span>{{ emptyText }}</span>
          </slot>
        </div>
      </template>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="showPagination"
      class="oa-pagination"
      :total="total"
      :page-size="size"
      :current-page="page"
      :page-sizes="pageSizes"
      :layout="layout"
      :background="true"
      @current-change="p => $emit('update:page', p)"
      @size-change="s => $emit('update:size', s)"
    />
  </div>
</template>

<script setup>
defineOptions({ name: 'OaTable' })

const props = defineProps({
  /** 数据源 */
  data: { type: Array, required: true },
  /** 列定义: [{ prop, label, width, minWidth, align, sortable, tooltip, formatter, cellClass }] */
  columns: { type: Array, required: true },
  /** 加载状态 */
  loading: { type: Boolean, default: false },
  /** 斑马纹 */
  stripe: { type: Boolean, default: true },
  /** 边框 */
  border: { type: Boolean, default: true },
  /** 空数据文案 */
  emptyText: { type: String, default: '暂无数据' },

  // === 分页 ===
  total: { type: Number, default: 0 },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  showPagination: { type: Boolean, default: true },
  pageSizes: { type: Array, default: () => [10, 20, 50, 100] },
  layout: { type: String, default: 'total, sizes, prev, pager, next, jumper' },

  // === 增强列 ===
  selection: { type: Boolean, default: false },
  index: { type: Boolean, default: false },
  actionWidth: { type: [String, Number], default: 200 },
  actionFixed: { type: [Boolean, String], default: false }
})

defineEmits(['update:page', 'update:size', 'row-click'])

function formatCell(col, row) {
  if (col.formatter) return col.formatter(row[col.prop], row)
  return row[col.prop] ?? '-'
}

function getRealIndex(i) {
  return (props.page - 1) * props.size + i + 1
}
</script>

<style lang="scss">
.oa-table-wrapper {
  background: $bg-card;
  border-radius: $radius-md;
  overflow: hidden;
}

.oa-table-toolbar {
  padding: $gap-lg $gap-xl;
  border-bottom: 1px solid $divider;
  display: flex;
  align-items: center;
  gap: $gap-md;
  flex-wrap: wrap;
}

.oa-table {
  width: 100%;
  border-radius: 0;

  th.el-table__cell {
    background: $bg-input;
    color: $text-1;
    font-weight: $fw-semibold;
    font-size: $fz-body;
    border-bottom: 1px solid $divider;
  }

  td.el-table__cell {
    padding: 12px 0;
    font-size: $fz-body;
    color: $text-1;
  }

  tr.el-table__row:hover > td.el-table__cell {
    background: $primary-tint !important;
  }

  &__empty {
    padding: $gap-3xl;
    text-align: center;
    color: $text-3;
    font-size: $fz-aux;
  }
}

.oa-pagination {
  padding: $gap-lg $gap-xl;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid $divider;
  background: $bg-card;
}
</style>
