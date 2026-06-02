<template>
  <div :class="['oa-card', { 'oa-card--hoverable': hoverable, 'oa-card--padded': padded }]">
    <div v-if="$slots.header || title" class="oa-card__header">
      <slot name="header">
        <h3 class="oa-card__title">{{ title }}</h3>
        <div v-if="$slots.extra" class="oa-card__extra">
          <slot name="extra" />
        </div>
      </slot>
    </div>
    <div class="oa-card__body">
      <slot />
    </div>
    <div v-if="$slots.footer" class="oa-card__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'OaCard' })

defineProps({
  /** 卡片标题（也可通过 #header 插槽自定义） */
  title: { type: String, default: '' },
  /** 鼠标悬停时显示阴影变化 */
  hoverable: { type: Boolean, default: true },
  /** 默认内边距 */
  padded: { type: Boolean, default: true }
})
</script>

<style lang="scss" scoped>
.oa-card {
  @include card-base;
  background: $bg-card;
  overflow: hidden;

  &--padded .oa-card__body {
    padding: $gap-xl;
  }

  &--hoverable:hover {
    box-shadow: $shadow-hover;
  }

  &__header {
    @include flex-between;
    padding: $gap-lg $gap-xl;
    border-bottom: 1px solid $divider;
  }

  &__title {
    font-size: $fz-h3;
    font-weight: $fw-semibold;
    color: $text-1;
    margin: 0;
  }

  &__extra {
    display: flex;
    align-items: center;
    gap: $gap-sm;
  }

  &__body {
    color: $text-1;
  }

  &__footer {
    padding: $gap-lg $gap-xl;
    border-top: 1px solid $divider;
    background: $bg-hover;
  }
}
</style>
