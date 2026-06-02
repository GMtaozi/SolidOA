<template>
  <div class="oa-empty">
    <div class="oa-empty__icon">
      <el-icon v-if="!icon" :size="64"><DocumentRemove /></el-icon>
      <component v-else :is="icon" :size="64" />
    </div>
    <p class="oa-empty__text">{{ text }}</p>
    <p v-if="description" class="oa-empty__desc">{{ description }}</p>
    <slot />
  </div>
</template>

<script setup>
import { DocumentRemove } from '@element-plus/icons-vue'

defineOptions({ name: 'OaEmpty' })

defineProps({
  text: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  icon: { type: [String, Object], default: null }
})
</script>

<style lang="scss" scoped>
.oa-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: #9CA3AF;

  &__icon {
    color: #9CA3AF;
    margin-bottom: 16px;
    opacity: 0.6;
  }

  &__text {
    font-size: 14px;
    color: #6B7280;
    margin: 0 0 8px 0;
  }

  &__desc {
    font-size: 12px;
    color: #9CA3AF;
    margin: 0;
    text-align: center;
    max-width: 320px;
  }
}
</style>
