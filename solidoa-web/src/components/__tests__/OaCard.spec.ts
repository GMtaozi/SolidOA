/**
 * OaCard 组件 - Vitest 烟雾测试
 * 验证: title、插槽、padded/hoverable
 */
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import OaCard from '../OaCard/index.vue'

describe('OaCard', () => {
  it('应渲染 title', () => {
    const wrapper = mount(OaCard, {
      props: { title: '卡片标题' }
    })
    expect(wrapper.find('.oa-card__title').text()).toBe('卡片标题')
  })

  it('应渲染默认插槽', () => {
    const wrapper = mount(OaCard, {
      slots: { default: '<p>卡片内容</p>' }
    })
    expect(wrapper.html()).toContain('卡片内容')
  })

  it('应渲染 header 插槽', () => {
    const wrapper = mount(OaCard, {
      slots: { header: '<h3>自定义头部</h3>' }
    })
    expect(wrapper.find('.oa-card__header').html()).toContain('自定义头部')
  })

  it('应渲染 footer 插槽', () => {
    const wrapper = mount(OaCard, {
      slots: {
        default: '内容',
        footer: '<div>底部</div>'
      }
    })
    expect(wrapper.find('.oa-card__footer').exists()).toBe(true)
    expect(wrapper.text()).toContain('底部')
  })

  it('应支持 padded=false', () => {
    const wrapper = mount(OaCard, {
      props: { title: 'test', padded: false }
    })
    expect(wrapper.find('.oa-card--padded').exists()).toBe(false)
  })

  it('应支持 hoverable=false', () => {
    const wrapper = mount(OaCard, {
      props: { title: 'test', hoverable: false }
    })
    expect(wrapper.find('.oa-card--hoverable').exists()).toBe(false)
  })
})
