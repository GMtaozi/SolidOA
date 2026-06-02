/**
 * OaButton 组件 - Vitest 烟雾测试
 * 验证: 渲染、变体、点击事件
 */
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OaButton from '../OaButton/index.vue'

describe('OaButton', () => {
  it('应正确渲染默认文本', () => {
    const wrapper = mount(OaButton, {
      props: { variant: 'primary' },
      slots: { default: '保存' }
    })
    expect(wrapper.text()).toBe('保存')
  })

  it('应应用 oa-button--primary 类名', () => {
    const wrapper = mount(OaButton, { props: { variant: 'primary' } })
    expect(wrapper.find('.oa-button--primary').exists()).toBe(true)
  })

  it('应支持 ghost 变体', () => {
    const wrapper = mount(OaButton, { props: { variant: 'ghost' } })
    expect(wrapper.find('.oa-button--ghost').exists()).toBe(true)
  })

  it('应支持 danger 变体', () => {
    const wrapper = mount(OaButton, { props: { variant: 'danger' } })
    expect(wrapper.find('.oa-button--danger').exists()).toBe(true)
  })

  it('应触发 click 事件', async () => {
    const wrapper = mount(OaButton, {
      props: { variant: 'primary' },
      slots: { default: '点击' }
    })
    await wrapper.find('.oa-button').trigger('click')
    expect(wrapper.emitted('click')).toBeTruthy()
  })

  it('应支持 block 属性', () => {
    const wrapper = mount(OaButton, {
      props: { variant: 'primary', block: true }
    })
    expect(wrapper.find('.oa-button--block').exists()).toBe(true)
  })
})
