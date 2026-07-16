import { describe, expect, it } from 'vitest'
import { parseInvoiceInfo } from './pasteParse'

describe('parseInvoiceInfo', () => {
  it('标准带标签六行全识别', () => {
    const out = parseInvoiceInfo(
      `公司名称：腾讯科技（深圳）有限公司
纳税人识别号：9144030071526726XG
地址：深圳市南山区高新区科技中一路腾讯大厦35层
电话：0755-86013388
开户银行：招商银行深圳汉京中心支行
银行账号：817281823910001`,
    )
    expect(out).toEqual({
      title: '腾讯科技（深圳）有限公司',
      taxNo: '9144030071526726XG',
      regAddress: '深圳市南山区高新区科技中一路腾讯大厦35层',
      regPhone: '0755-86013388',
      bankName: '招商银行深圳汉京中心支行',
      bankAccount: '817281823910001',
    })
  })

  it('地址电话/开户行及账号两种混排标签能拆开', () => {
    const out = parseInvoiceInfo(
      `名称:腾讯科技（深圳）有限公司
税号:9144030071526726XG
地址电话:深圳市南山区腾讯大厦35层 0755-86013388
开户行及账号:招商银行深圳汉京中心支行 817281823910001`,
    )
    expect(out.regAddress).toBe('深圳市南山区腾讯大厦35层')
    expect(out.regPhone).toBe('0755-86013388')
    expect(out.bankName).toBe('招商银行深圳汉京中心支行')
    expect(out.bankAccount).toBe('817281823910001')
  })

  it('无标签多行按特征分类(税号走校验位)', () => {
    const out = parseInvoiceInfo(
      `腾讯科技（深圳）有限公司
9144030071526726XG
深圳市南山区高新区科技中一路腾讯大厦35层
招商银行深圳汉京中心支行
817281823910001`,
    )
    expect(out.title).toBe('腾讯科技（深圳）有限公司')
    expect(out.taxNo).toBe('9144030071526726XG')
    expect(out.regAddress).toBe('深圳市南山区高新区科技中一路腾讯大厦35层')
    expect(out.bankName).toBe('招商银行深圳汉京中心支行')
    expect(out.bankAccount).toBe('817281823910001')
  })

  it('单行逗号分隔也能拆', () => {
    const out = parseInvoiceInfo(
      '腾讯科技（深圳）有限公司，9144030071526726XG，深圳市南山区腾讯大厦35层，0755-86013388',
    )
    expect(out.title).toBe('腾讯科技（深圳）有限公司')
    expect(out.taxNo).toBe('9144030071526726XG')
    expect(out.regAddress).toBe('深圳市南山区腾讯大厦35层')
    expect(out.regPhone).toBe('0755-86013388')
  })

  it('小写税号归一大写、账号去空格', () => {
    const out = parseInvoiceInfo('税号:9144030071526726xg\n账号:8172 8182 3910 001')
    expect(out.taxNo).toBe('9144030071526726XG')
    expect(out.bankAccount).toBe('817281823910001')
  })

  it('假税号不会被无标签轮误认(校验位不过)', () => {
    const out = parseInvoiceInfo('随便一段话 9144030071526726XA 什么都不是')
    expect(out.taxNo).toBeUndefined()
  })

  it('纯废话识别为空对象', () => {
    expect(parseInvoiceInfo('今天天气不错，适合开发票')).toEqual({})
  })
})
