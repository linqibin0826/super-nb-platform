/** 开票资料智能粘贴识别:把用户整段粘贴的「开票信息」拆进表单字段。
 *  两轮策略——先认标签(名称:/税号:/开户行: 等常见叫法,含「地址电话」「开户行及账号」
 *  两种专票习惯的混排标签),没标签的再按特征分类(18 位税号走校验位验真、电话/账号按
 *  数字形态、公司名按后缀、地址按地名用字)。识别不出的字段保持缺席,绝不瞎猜。 */
import { isValidTaxNo } from './taxno'

export interface ParsedInvoiceInfo {
  title?: string
  taxNo?: string
  regAddress?: string
  regPhone?: string
  bankName?: string
  bankAccount?: string
}

const PHONE_RE = /(?:0\d{2,3}[- ]?\d{7,8}(?:[-\s]+\d{2,8})?|1[3-9]\d{9}|400[- ]?\d{3}[- ]?\d{4})/
const TITLE_SUFFIX_RE = /(?:有限公司|有限责任公司|股份有限公司|集团|合伙企业(?:（[^）]*）)?|事务所|工作室|研究院|学院|医院|中心|厂|店)$/

/** 标签别名 → 字段(顺序即优先级:具体叫法在前,防「税号」抢了「纳税人识别号」的值) */
const LABELED: { key: keyof ParsedInvoiceInfo; re: RegExp }[] = [
  { key: 'taxNo', re: /(?:纳税人识别号|统一社会信用代码|社会信用代码|信用代码|税务登记[证号]+|税号)\s*[:：]?\s*([0-9A-Za-z]{15,20})/ },
  { key: 'bankAccount', re: /(?:银行账[号户]|开户账[号户]|对公账[号户]|基本户账[号户]|账[号户]|卡号)\s*[:：]?\s*([0-9][0-9 ]{7,28}[0-9])/ },
  { key: 'bankName', re: /(?:开户银行|开户行名称|银行名称|开户行)(?:及账[号户])?\s*[:：]?\s*([^\n,，;；:：]+)/ },
  { key: 'regPhone', re: new RegExp('(?:注册电话|联系电话|电话|联系方式|手机)\\s*[:：]?\\s*(' + PHONE_RE.source + ')') },
  // 「地址电话」混排是专票四要素的习惯写法,值里的尾部电话由 splitTrailingPhone 拆走
  { key: 'regAddress', re: /(?:注册地址|单位地址|公司地址|经营地址|地址(?:[、和及]?电话)?)\s*[:：]?\s*([^\n;；]+)/ },
  { key: 'title', re: /(?:公司名称|单位名称|企业名称|发票抬头|购买?方名称|抬头|名称)\s*[:：]?\s*([^\n,，;；:：]+)/ },
]

/** 尾部电话串从地址里拆走(与后端 JumeiCompanyRegistryAdapter 同思路) */
function splitTrailingPhone(raw: string): { address: string; phone?: string } {
  const m = raw.match(/([0-9][0-9\- ()]{6,}[0-9])\s*$/)
  if (m && raw.slice(0, m.index).trim()) {
    return { address: raw.slice(0, m.index).trim(), phone: m[1].trim() }
  }
  return { address: raw.trim() }
}

const clean = (v: string | undefined): string | undefined => {
  const s = (v ?? '').trim()
  return s ? s : undefined
}

export function parseInvoiceInfo(text: string): ParsedInvoiceInfo {
  const out: ParsedInvoiceInfo = {}
  const src = text.replace(/ /g, ' ')

  // 第一轮:标签识别
  for (const { key, re } of LABELED) {
    if (out[key]) continue
    const m = src.match(re)
    if (m) out[key] = clean(m[1])
  }
  // 捕获卫生(话术型文本的标签捕获常拖泥带水,脏值不配冒充「识别成功」——注意一律 delete
  // 而不是置 undefined,显式 undefined 会在 set() 展开时抹掉用户已填的值):
  // 抬头剥「是/为/叫/写」一类口语前缀
  if (out.title) {
    const t = out.title.replace(/^(?:就?[是为叫]|写上?|用)\s*/, '').trim()
    if (t) out.title = t
    else delete out.title
  }
  // 标签捕到的「税号」必须过校验位,过不了当没捕到(留给无标签轮或 AI)
  if (out.taxNo && !isValidTaxNo(out.taxNo)) {
    delete out.taxNo
  }
  // 地址值:剥口语前缀 → 混进「电话/开户/账号/税号」关键词从关键词处砍断 → 尾部电话拆走
  if (out.regAddress) {
    const cut = out.regAddress
      .replace(/^(?:就?[是为写]|写上?|填)\s*/, '')
      .split(/[,，]?\s*(?:联系电话|电话|开户|银行账|账[号户]|税号|纳税人)/)[0]
      .trim()
    const { address, phone } = splitTrailingPhone(cut)
    if (address.length >= 6) out.regAddress = address
    else delete out.regAddress
    if (!out.regPhone && phone) out.regPhone = phone
  }
  // 「开户行及账号」混排:开户行值尾部的长数字串挪去账号
  if (out.bankName) {
    const m = out.bankName.match(/([0-9][0-9 ]{7,28}[0-9])\s*$/)
    if (m) {
      const head = out.bankName.slice(0, m.index).trim()
      if (head) {
        out.bankName = head
        if (!out.bankAccount) out.bankAccount = m[1]
      }
    }
  }

  // 第二轮:无标签特征分类(只补第一轮没认出的字段)
  const segments = src.split(/[\n,，;；\t]+/).map((s) => s.trim()).filter(Boolean)
  for (const seg of segments) {
    if (!out.taxNo) {
      const t = seg.match(/[0-9A-Za-z]{18}|(?<![0-9])[0-9]{15}(?![0-9])/)
      if (t && isValidTaxNo(t[0])) {
        out.taxNo = t[0]
        continue
      }
    }
    if (!out.regPhone) {
      const p = seg.match(new RegExp('(?<![0-9])' + PHONE_RE.source + '(?![0-9])'))
      if (p && seg.replace(/[0-9\- ()]/g, '').length === 0) {
        out.regPhone = p[0]
        continue
      }
    }
    if (!out.bankName && /银行|信用社|农商行/.test(seg) && !/[0-9]{9,}/.test(seg)) {
      out.bankName = seg
      continue
    }
    if (!out.bankAccount && /^[0-9][0-9 ]{10,28}[0-9]$/.test(seg) && seg.replace(/ /g, '') !== out.taxNo) {
      out.bankAccount = seg
      continue
    }
    if (!out.title && TITLE_SUFFIX_RE.test(seg) && seg.length <= 50) {
      out.title = seg
      continue
    }
    if (!out.regAddress && seg.length >= 8 && !/银行/.test(seg)
        && (seg.match(/[省市区县路街道号栋楼层室园区]/g) ?? []).length >= 2) {
      const { address, phone } = splitTrailingPhone(seg)
      out.regAddress = address
      if (!out.regPhone && phone) out.regPhone = phone
    }
  }

  // 归一:税号大写、账号去空格
  if (out.taxNo) out.taxNo = out.taxNo.toUpperCase()
  if (out.bankAccount) out.bankAccount = out.bankAccount.replace(/ /g, '')
  return out
}

/** AI 识别结果 → 表单补丁:有值字段收进来,税号必须过校验位(后端防幻觉之上的最后一道);
 *  归一与规则识别同口径(税号大写、账号去空格)。 */
export function aiParsedPatch(fields: { [K in keyof ParsedInvoiceInfo]-?: string | null }): ParsedInvoiceInfo {
  const out: ParsedInvoiceInfo = {}
  const pick = (v: string | null): string | undefined => {
    const s = (v ?? '').trim()
    return s ? s : undefined
  }
  out.title = pick(fields.title)
  const tax = pick(fields.taxNo)
  if (tax && isValidTaxNo(tax)) out.taxNo = tax.toUpperCase()
  out.regAddress = pick(fields.regAddress)
  out.regPhone = pick(fields.regPhone)
  out.bankName = pick(fields.bankName)
  out.bankAccount = pick(fields.bankAccount)?.replace(/ /g, '')
  for (const k of Object.keys(out) as (keyof ParsedInvoiceInfo)[]) {
    if (out[k] === undefined) delete out[k]
  }
  return out
}
