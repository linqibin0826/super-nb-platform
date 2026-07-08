import { describe, expect, it } from 'vitest'
import { composeSize, deriveFromSize } from '../spec'

describe('deriveFromSize 收敛越界画质档（gpt-image-2 约束）', () => {
  it('历史/灵感里的 1:1 4K(3840x3840) 反推落到有效 2K，不复现越界尺寸', () => {
    expect(deriveFromSize('3840x3840')).toMatchObject({ ratio: '1:1', tier: '2K' })
  })
  it('有效组合原样反推', () => {
    expect(deriveFromSize('1152x2048')).toMatchObject({ ratio: '9:16', tier: '2K' })
    expect(deriveFromSize('2160x3840')).toMatchObject({ ratio: '9:16', tier: '4K' })
  })
  it('auto / 无法识别的尺寸落兜底 1:1；自定义尺寸正常反推', () => {
    expect(deriveFromSize('auto')).toMatchObject({ ratio: '1:1' })
    expect(deriveFromSize('1234x567')).toMatchObject({ ratio: 'custom', w: '1234', h: '567' })
  })
})

describe('composeSize custom：正整数即产出，合法性交上层 isValidGptImageSize 判定', () => {
  it('合法自定义尺寸原样产出', () => {
    expect(composeSize({ ratio: 'custom', tier: '2K', w: '2048', h: '1344' })).toBe('2048x1344')
  })
  it('超出旧 128-4096 范围也产出字符串（不再在此拦，交上层校验）', () => {
    expect(composeSize({ ratio: 'custom', tier: '4K', w: '5000', h: '5000' })).toBe('5000x5000')
  })
  it('半输入 / 非正整数返回 null（不上抛）', () => {
    expect(composeSize({ ratio: 'custom', tier: '1K', w: '', h: '1024' })).toBeNull()
    expect(composeSize({ ratio: 'custom', tier: '1K', w: '0', h: '1024' })).toBeNull()
  })
})
