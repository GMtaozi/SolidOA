<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :close-on-click-modal="closeOnClickModal"
    :show-close="false"
    custom-class="oa-dialog"
    @open="emit('open')"
    @close="handleClose"
  >
    <div class="oa-dialog__body">
      <slot />
    </div>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
    <template v-else #footer>
      <div class="oa-dialog__default-footer">
        <OaButton variant="ghost" @click="handleCancel">取消</OaButton>
        <OaButton variant="primary" @click="emit('confirm')">确定</OaButton>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed } from 'vue'
import OaButton from '../OaButton/index.vue'

defineOptions({ name: 'OaDialog' })

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  width: { type: [String, Number], default: '520px' },
  closeOnClickModal: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'close', 'confirm', 'open'])

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

function handleClose() {
  visible.value = false
  emit('close')
}
function handleCancel() {
  visible.value = false
}
</script>

<style lang="scss">
/* 必须用全局（非 scoped）才能覆盖 el-dialog */
.oa-dialog {
  border-radius: $radius-lg;
  overflow: hidden;
  box-shadow: $shadow-xl;

  .el-dialog__header {
    padding: $gap-xl $gap-2xl;
    border-bottom: 1px solid $divider;
    margin-right: 0;
  }

  .el-dialog__title {
    font-size: $fz-h3;
    font-weight: $fw-semibold;
    color: $text-1;
  }

  .el-dialog__body {
    padding: $gap-2xl;
    color: $text-1;
  }

  .el-dialog__footer {
    padding: $gap-lg $gap-2xl;
    border-top: 1px solid $divider;
    background: $bg-hover;
  }
}

.oa-dialog__default-footer {
  display: flex;
  justify-content: flex-end;
  gap: $gap-md;
}
</style>
