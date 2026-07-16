/** 开票资料智能粘贴识别:整段粘贴全量交给自家中转 LLM 拆字段(规则识别 2026-07-17 退役,
 *  话术型文本识别率太差)。本模块只负责把 AI 结果收进表单:税号必须过校验位(后端防幻觉
 *  之上的最后一道),归一口径统一(税号大写、账号去空格)。识别不出的字段保持缺席,绝不瞎猜。 */
import { isValidTaxNo } from './taxno'

export interface ParsedInvoiceInfo {
  title?: string
  taxNo?: string
  regAddress?: string
  regPhone?: string
  bankName?: string
  bankAccount?: string
}

/** AI 识别结果 → 表单补丁:有值字段收进来,null/空白视为缺席并从补丁里删掉
 *  (显式 undefined 会在 set() 展开时抹掉用户已填的值)。 */
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
