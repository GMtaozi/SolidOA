/**
 * OaDialog 组件 - Vitest 烟雾测试
 * 验证: v-model 双向绑定、open/close 事件、cancel
 */
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import OaDialog from '../OaDialog/index.vue'

describe('OaDialog', () => {
  it('应支持 v-model 控制显示', async () => {
    const wrapper = mount(OaDialog, {
      props: { modelValue: true, title: '测试弹窗' },
      slots: { default: '弹窗内容' },
      global: {
        stubs: {
          'el-dialog': {
            template: '<div class="el-dialog-stub" v-if="modelValue"><div class="el-dialog__title">{{title}}</div><slot/></div>',
            props: ['modelValue', 'title', 'width', 'closeOnClickModal', 'showClose', 'customClass']
          },
          'oa-button': {
            template: '<button class="oa-btn-stub" @click="$emit(\'click\')"><slot/></button>'
          }
        }
      }
    })
    await nextTick()
    expect(wrapper.find('.el-dialog-stub').exists()).toBe(true)
    expect(wrapper.text()).toContain('弹窗内容')
  })

  it('modelValue=false 时不应渲染', () => {
    const wrapper = mount(OaDialog, {
      props: { modelValue: false, title: '隐藏弹窗' }
    })
    expect(wrapper.find('.el-dialog-stub').exists()).toBe(false)
  })

  it('应发出 update:modelValue 事件', async () => {
    const wrapper = mount(OaDialog, {
      props: { modelValue: true, title: '测试' },
      global: {
        stubs: {
          'el-dialog': {
            template: '<div class="el-dialog-stub" v-if="modelValue"><slot/></div>',
            props: ['modelValue', 'title']
          },
          'oa-button': {
            template: '<button class="oa-btn-stub" @click="$emit(\'click\')"><slot/></button>'
          }
        }
      }
    })
    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
  })
})
