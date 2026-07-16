import { describe, expect, it } from 'vitest'
import { aiParsedPatch } from './pasteParse'

describe('aiParsedPatch', () => {
  it('有值收进来并归一,null 缺席', () => {
    const patch = aiParsedPatch({
      title: ' 腾讯科技（深圳）有限公司 ',
      taxNo: '9144030071526726xg',
      regAddress: null,
      regPhone: '0755-86013388',
      bankName: null,
      bankAccount: '8172 8182 3910 001',
    })
    expect(patch).toEqual({
      title: '腾讯科技（深圳）有限公司',
      taxNo: '9144030071526726XG',
      regPhone: '0755-86013388',
      bankAccount: '817281823910001',
    })
  })

  it('AI 回的假税号被校验位拦下', () => {
    const patch = aiParsedPatch({
      title: '某某公司',
      taxNo: '9144030071526726XA',
      regAddress: null,
      regPhone: null,
      bankName: null,
      bankAccount: null,
    })
    expect(patch.taxNo).toBeUndefined()
    expect(patch.title).toBe('某某公司')
  })

  it('全 null 得空补丁,不带任何显式 undefined 键', () => {
    const patch = aiParsedPatch({
      title: null,
      taxNo: null,
      regAddress: null,
      regPhone: null,
      bankName: null,
      bankAccount: null,
    })
    expect(patch).toEqual({})
    expect(Object.keys(patch)).toHaveLength(0)
  })
})
