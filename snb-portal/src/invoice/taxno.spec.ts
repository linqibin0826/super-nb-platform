import { describe, expect, it } from 'vitest'
import { isValidTaxNo } from './taxno'

// 测试向量与后端 TaxNoFormatTest 同源,改必两边同步
describe('isValidTaxNo', () => {
  it('真实 18 位统一社会信用代码通过', () => {
    expect(isValidTaxNo('9144030071526726XG')).toBe(true) // 腾讯科技(深圳)
    expect(isValidTaxNo('91440300708461136T')).toBe(true) // 深圳市腾讯计算机系统
    expect(isValidTaxNo('  9144030071526726xg  ')).toBe(true) // 大小写/空白不敏感
  })

  it('抄错一位/校验码不符即拒', () => {
    expect(isValidTaxNo('9144030071526726XA')).toBe(false)
    expect(isValidTaxNo('9144030071526726YG')).toBe(false)
  })

  it('国标字符集外的字符拒(I/O 等)', () => {
    expect(isValidTaxNo('9I440300708461136T')).toBe(false)
    expect(isValidTaxNo('91440300708461136O')).toBe(false)
  })

  it('乱填与错误长度拒', () => {
    expect(isValidTaxNo('fdsa')).toBe(false)
    expect(isValidTaxNo('')).toBe(false)
    expect(isValidTaxNo('9144030071526726X')).toBe(false) // 17 位
    expect(isValidTaxNo('9144030071526726XG9')).toBe(false) // 19 位
  })

  it('15 位老税号纯数字放行、带字母拒', () => {
    expect(isValidTaxNo('123456789012345')).toBe(true)
    expect(isValidTaxNo('12345678901234A')).toBe(false)
  })
})
