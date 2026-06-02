<template>
  <el-icon :size="size" :color="color">
    <component :is="iconComponent" />
  </el-icon>
</template>

<script setup>
import * as ElIcons from '@element-plus/icons-vue'
import { computed } from 'vue'

defineOptions({ name: 'OaIcon' })

const props = defineProps({
  /** Element Plus 图标名, 例如: 'User', 'Setting', 'Plus' */
  name: { type: String, required: true },
  size: { type: [Number, String], default: 16 },
  color: { type: String, default: '' }
})

const iconComponent = computed(() => ElIcons[props.name])
</script>
