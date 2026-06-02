<template>
  <el-button
    :class="['oa-button', `oa-button--${variant}`, { 'oa-button--block': block }]"
    v-bind="$attrs"
  >
    <slot />
  </el-button>
</template>

<script setup>
defineOptions({ name: 'OaButton' })

defineProps({
  /** 变体: primary 主色 / ghost 幽灵 / danger 危险 / default 默认 */
  variant: { type: String, default: 'primary', validator: v => ['primary', 'ghost', 'danger', 'default'].includes(v) },
  /** 是否占满父容器宽度 */
  block: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
.oa-button {
  border-radius: $radius-md;
  font-weight: $fw-medium;
  transition: all $motion-base $ease-standard;
  min-height: 36px;
  padding: 8px 18px;

  &--primary {
    background: linear-gradient(135deg, $primary 0%, $primary-light 100%);
    border: none;
    color: $text-on-primary;
    box-shadow: 0 4px 12px $primary-tint-2;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px $primary-tint-2;
      background: linear-gradient(135deg, $primary-light 0%, $primary 100%);
    }
    &:active {
      transform: translateY(0);
      background: $primary-deep;
    }
  }

  &--ghost {
    background: $bg-card;
    border: 1px solid $border-input;
    color: $text-1;

    &:hover {
      border-color: $primary;
      color: $primary;
      background: $primary-tint;
    }
  }

  &--danger {
    background: $danger;
    border: 1px solid $danger-deep;
    color: $danger-deep;

    &:hover {
      background: $danger-deep;
      color: $text-on-primary;
    }
  }

  &--block {
    display: flex;
    width: 100%;
  }
}
</style>
