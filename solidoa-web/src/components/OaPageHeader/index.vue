<template>
  <div class="oa-page-header">
    <div class="oa-page-header__title-area">
      <h2 v-if="title" class="oa-page-header__title">{{ title }}</h2>
      <p v-if="subtitle" class="oa-page-header__subtitle">{{ subtitle }}</p>
    </div>
    <div v-if="$slots.extra" class="oa-page-header__extra">
      <slot name="extra" />
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'OaPageHeader' })

defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' }
})
</script>

<style lang="scss" scoped>
.oa-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #F0EDE9;

  &__title {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #3B3B3B;
  }

  &__subtitle {
    margin: 4px 0 0 0;
    font-size: 12px;
    color: #6B7280;
  }

  &__extra {
    display: flex;
    gap: 12px;
  }
}
</style>
