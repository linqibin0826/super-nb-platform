import { describe, expect, it } from 'vitest'
import { aiParsedPatch, parseInvoiceInfo } from './pasteParse'

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

  it('话术型文本捕获卫生:剥口语前缀/脏地址砍断/税号靠段落轮捞回', () => {
    const out = parseInvoiceInfo(
      '帮我开下票哈，抬头是深圳市腾讯计算机系统有限公司，税号我记得是91440300708461136T来着，'
        + '地址就写深圳市南山区高新区科技中一路腾讯大厦，电话0755-86013388，'
        + '开户是招商银行深圳市分行科技园支行 账户7559123456109',
    )
    expect(out.title).toBe('深圳市腾讯计算机系统有限公司') // 「是」前缀已剥
    expect(out.taxNo).toBe('91440300708461136T')
    expect(out.regAddress).toBe('深圳市南山区高新区科技中一路腾讯大厦') // 「就写」已剥、电话开户已砍
    expect(out.regPhone).toBe('0755-86013388')
    expect(out.bankAccount).toBe('7559123456109')
  })

  it('标签捕到的假税号作废,不冒充识别成功', () => {
    const out = parseInvoiceInfo('名称:某某科技有限公司\n税号:9144030071526726XA')
    expect(out.taxNo).toBeUndefined()
    expect(out.title).toBe('某某科技有限公司')
  })
})

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
})
