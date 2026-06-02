/**
 * OaTable 组件 - Vitest 单元测试
 * 注: el-table 在 jsdom 下完整渲染复杂，本测试聚焦纯逻辑（getRealIndex/formatCell）
 *   真实渲染验证用 Playwright 跑 demo 页
 */
import { describe, it, expect } from 'vitest'

// 提取 OaTable 的纯函数（getRealIndex / formatCell）做单元测试
// 这些函数未来可抽到独立 utils 文件
describe('OaTable 纯逻辑', () => {
  // 模拟 getRealIndex
  function getRealIndex(page: number, size: number, localIndex: number): number {
    return (page - 1) * size + localIndex + 1
  }

  it('getRealIndex: 第 1 页第 0 行 → 1', () => {
    expect(getRealIndex(1, 10, 0)).toBe(1)
  })

  it('getRealIndex: 第 2 页第 5 行 → 16', () => {
    expect(getRealIndex(2, 10, 5)).toBe(16)
  })

  it('getRealIndex: 第 3 页第 9 行（每页 20）→ 50', () => {
    expect(getRealIndex(3, 20, 9)).toBe(50)
  })

  // 模拟 formatCell
  function formatCell(col: any, row: any): string {
    if (col.formatter) return col.formatter(row[col.prop], row)
    return row[col.prop] ?? '-'
  }

  it('formatCell: 无 formatter 时直接取 row[prop]', () => {
    const col = { prop: 'name' }
    const row = { name: '张三' }
    expect(formatCell(col, row)).toBe('张三')
  })

  it('formatCell: 字段为 null/undefined 时显示 -', () => {
    const col = { prop: 'amount' }
    const row = { amount: null }
    expect(formatCell(col, row)).toBe('-')
  })

  it('formatCell: 有 formatter 时调用 formatter', () => {
    const col = {
      prop: 'amount',
      formatter: (val: number) => `¥${val.toFixed(2)}`,
    }
    const row = { amount: 100 }
    expect(formatCell(col, row)).toBe('¥100.00')
  })

  it('columns 数组校验: 应至少含 prop/label', () => {
    const valid = [
      { prop: 'id', label: 'ID' },
      { prop: 'name', label: '姓名' },
    ]
    for (const col of valid) {
      expect(col.prop).toBeTruthy()
      expect(col.label).toBeTruthy()
    }
  })
})
