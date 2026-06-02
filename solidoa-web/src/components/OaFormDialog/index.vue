<template>
  <OaDialog
    :model-value="modelValue"
    :title="title"
    :width="width"
    @update:model-value="v => emit('update:modelValue', v)"
    @confirm="handleSubmit"
  >
    <el-form
      ref="formRef"
      :model="model"
      :rules="rules"
      label-width="100px"
      label-position="right"
      class="oa-form-dialog"
      @submit.prevent
    >
      <slot :model="model" />
    </el-form>

    <template #footer>
      <OaButton variant="ghost" @click="handleCancel">取消</OaButton>
      <OaButton variant="primary" :loading="submitting" @click="handleSubmit">
        {{ submitText }}
      </OaButton>
    </template>
  </OaDialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import OaDialog from '../OaDialog/index.vue'
import OaButton from '../OaButton/index.vue'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'OaFormDialog' })

const props = defineProps({
  modelValue: { type: Boolean, required: true },
  title: { type: String, default: '' },
  width: { type: [String, Number], default: '520px' },
  /** 表单数据 (v-model:model) */
  model: { type: Object, required: true },
  /** 验证规则 */
  rules: { type: Object, default: () => ({}) },
  /** 提交按钮文案 */
  submitText: { type: String, default: '确定' },
  /** 异步提交函数 - 返回 Promise, 成功关闭弹窗, 失败保留 */
  onSubmit: { type: Function, default: null }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref()
const submitting = ref(false)

function handleCancel() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请检查表单填写')
    return
  }
  if (!props.onSubmit) {
    emit('success', props.model)
    emit('update:modelValue', false)
    return
  }
  submitting.value = true
  try {
    const result = await props.onSubmit(props.model)
    emit('success', result)
    emit('update:modelValue', false)
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// 关闭时清空验证
watch(
  () => props.modelValue,
  v => {
    if (!v && formRef.value) {
      formRef.value.clearValidate()
    }
  }
)
</script>

<style lang="scss" scoped>
.oa-form-dialog {
  :deep(.el-form-item) {
    margin-bottom: $gap-lg;
  }
  :deep(.el-form-item:last-child) {
    margin-bottom: 0;
  }
}
</style>
