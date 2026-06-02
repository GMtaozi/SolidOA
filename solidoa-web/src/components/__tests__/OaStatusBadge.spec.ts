/**
 * OaStatusBadge 组件 - Vitest 烟雾测试
 * 验证: 类型渲染、dot、文本
 */
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OaStatusBadge from '../OaStatusBadge/index.vue'

describe('OaStatusBadge', () => {
  it('应渲染默认文本', () => {
    const wrapper = mount(OaStatusBadge, {
      props: { type: 'success', text: '已通过' }
    })
    expect(wrapper.text()).toBe('已通过')
  })

  it('应应用 oa-badge--success 类名', () => {
    const wrapper = mount(OaStatusBadge, { props: { type: 'success' } })
    expect(wrapper.find('.oa-badge--success').exists()).toBe(true)
  })

  it('应支持 6 种类型', () => {
    const types = ['success', 'warning', 'danger', 'info', 'primary', 'default']
    for (const type of types) {
      const wrapper = mount(OaStatusBadge, { props: { type } })
      expect(wrapper.find(`.oa-badge--${type}`).exists()).toBe(true)
    }
  })

  it('dot=true 时应显示左侧圆点', () => {
    const wrapper = mount(OaStatusBadge, {
      props: { type: 'success', dot: true }
    })
    expect(wrapper.find('.oa-badge__dot').exists()).toBe(true)
  })

  it('dot=false 时不应显示圆点', () => {
    const wrapper = mount(OaStatusBadge, {
      props: { type: 'success', dot: false }
    })
    expect(wrapper.find('.oa-badge__dot').exists()).toBe(false)
  })

  it('应支持插槽内容', () => {
    const wrapper = mount(OaStatusBadge, {
      props: { type: 'warning' },
      slots: { default: '插槽文本' }
    })
    expect(wrapper.text()).toBe('插槽文本')
  })
})
