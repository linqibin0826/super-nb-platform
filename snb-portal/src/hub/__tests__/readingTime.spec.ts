import { describe, expect, it } from 'vitest'
import { readingMinutes } from '../readingTime'

describe('readingMinutes', () => {
  it('极短内容至少 1 分钟', () => {
    expect(readingMinutes('<p>hi</p>')).toBe(1)
    expect(readingMinutes('')).toBe(1)
  })

  it('CJK 按 400 字/分向上取整（标签与实体不计入）', () => {
    expect(readingMinutes(`<p>${'字'.repeat(400)}</p>`)).toBe(1)
    expect(readingMinutes(`<h2>${'字'.repeat(401)}</h2>&nbsp;`)).toBe(2)
  })

  it('拉丁按 180 词/分，与 CJK 累加', () => {
    expect(readingMinutes(`<p>${'word '.repeat(360)}</p>`)).toBe(2)
    expect(readingMinutes(`<p>${'字'.repeat(200)}${'word '.repeat(90)}</p>`)).toBe(1)
  })
})
