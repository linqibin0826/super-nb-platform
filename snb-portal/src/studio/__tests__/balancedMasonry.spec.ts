import { describe, expect, it } from 'vitest'
import { assignPending, emptyLayout } from '../BalancedMasonry'
import type { PromptListItem } from '../../lib/galleryApi'

function item(id: number, w = 480, h = 640): PromptListItem {
  return {
    id: String(id),
    title: `t${id}`,
    authorName: 'a',
    imageUrl: `u${id}`,
    imageW: w,
    imageH: h,
    likeCount: 0,
    favCount: 0,
  }
}

describe('BalancedMasonry 分列算法', () => {
  it('贪心分给最矮列：等高卡片轮转铺满所有列', () => {
    const layout = assignPending(emptyLayout(4), Array.from({ length: 8 }, (_, i) => item(i)))
    expect(layout.cols.map((c) => c.length)).toEqual([2, 2, 2, 2])
  })

  it('高卡占住的列会被绕开（不是死板轮转）', () => {
    const items = [item(0, 480, 1920), item(1), item(2), item(3)] // 0 号超高
    const layout = assignPending(emptyLayout(2), items)
    // 0 号落列 0 后其余全去列 1
    expect(layout.cols[0].map((s) => s.item.id)).toEqual(['0'])
    expect(layout.cols[1].map((s) => s.item.id)).toEqual(['1', '2', '3'])
  })

  it('追加只补后缀：老卡不挪列（无限滚动核心保证）', () => {
    const page1 = Array.from({ length: 5 }, (_, i) => item(i))
    const layout = assignPending(emptyLayout(3), page1)
    const before = layout.cols.map((c) => c.map((s) => s.item.id))
    const page2 = [...page1, ...Array.from({ length: 5 }, (_, i) => item(10 + i))]
    assignPending(layout, page2)
    layout.cols.forEach((col, ci) => {
      expect(col.slice(0, before[ci].length).map((s) => s.item.id)).toEqual(before[ci])
    })
    expect(layout.count).toBe(10)
  })

  it('新批次分散到多列，而不是堆在末尾几列（CSS columns 旧病）', () => {
    const layout = assignPending(emptyLayout(5), Array.from({ length: 24 }, (_, i) => item(i)))
    const grew = layout.cols.map((c) => c.length)
    assignPending(layout, Array.from({ length: 48 }, (_, i) => item(i)))
    const touched = layout.cols.filter((c, ci) => c.length > grew[ci]).length
    expect(touched).toBe(5) // 24 张新卡 5 列全都有份
  })

  it('幂等：同一 items 重复喂不重复分配（StrictMode 双渲染安全）', () => {
    const items = Array.from({ length: 6 }, (_, i) => item(i))
    const layout = assignPending(emptyLayout(3), items)
    assignPending(layout, items)
    expect(layout.cols.flat().length).toBe(6)
  })

  it('批次内错峰延迟递增有上限', () => {
    const layout = assignPending(emptyLayout(2), Array.from({ length: 20 }, (_, i) => item(i)))
    const delays = layout.cols.flat().map((s) => s.delay)
    expect(Math.min(...delays)).toBe(0)
    expect(Math.max(...delays)).toBeLessThanOrEqual(0.5)
  })

  it('缺宽高按 3:4 记账不炸', () => {
    const layout = assignPending(emptyLayout(2), [item(0, 0, 0), item(1, 0, 0)])
    expect(layout.cols[0].length + layout.cols[1].length).toBe(2)
    expect(layout.heights.every((h) => h > 0)).toBe(true)
  })
})
