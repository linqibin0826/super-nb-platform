import { describe, expect, it } from 'vitest'
import { createT } from '../core'

const messages = {
  zh: { a: { b: '你好 {name}', plain: '素' } },
  en: { a: { b: 'hi {name}', plain: 'plain' } },
}

describe('createT', () => {
  it('取嵌套 key 并插值', () => {
    expect(createT(messages, 'zh')('a.b', { name: '站长' })).toBe('你好 站长')
    expect(createT(messages, 'en')('a.plain')).toBe('plain')
  })
  it('缺失 key 原样返回', () => {
    expect(createT(messages, 'zh')('a.missing')).toBe('a.missing')
  })
})
