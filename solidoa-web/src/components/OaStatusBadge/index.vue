<template>
  <span :class="['oa-badge', `oa-badge--${type}`]">
    <span v-if="dot" class="oa-badge__dot" aria-hidden="true" />
    <slot>{{ text }}</slot>
  </span>
</template>

<script setup>
defineOptions({ name: 'OaStatusBadge' })

defineProps({
  /** 类型: success/warning/danger/info/primary/default */
  type: {
    type: String,
    default: 'default',
    validator: v => ['success', 'warning', 'danger', 'info', 'primary', 'default'].includes(v)
  },
  /** 状态文字（无 slot 时显示） */
  text: { type: String, default: '' },
  /** 是否显示左侧圆点 */
  dot: { type: Boolean, default: true }
})
</script>

<style lang="scss" scoped>
.oa-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: $radius-pill;
  font-size: $fz-aux;
  font-weight: $fw-medium;
  line-height: 1.5;
  white-space: nowrap;

  &__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
    flex-shrink: 0;
  }

  &--success { @include status-badge($success-deep, rgba(52, 211, 153, 0.12)); }
  &--warning { @include status-badge($warning-deep, rgba(251, 191, 36, 0.15)); }
  &--danger  { @include status-badge($danger-deep, rgba(252, 165, 165, 0.15)); }
  &--info    { @include status-badge($info, rgba(148, 163, 184, 0.12)); }
  &--primary { @include status-badge($primary-deep, $primary-tint-2); }
  &--default { @include status-badge($text-2, $bg-input); }
}
</style>
