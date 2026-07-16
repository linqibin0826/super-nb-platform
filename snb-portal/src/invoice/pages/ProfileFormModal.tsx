import { useRef, useState } from 'react'
import { Button, Input, Modal } from '../../ui'
import { t } from '../../i18n'
import { api, type ProfileT, type RegistryOfficialT } from '../api'
import { isValidTaxNo } from '../taxno'
import { aiParsedPatch, parseInvoiceInfo } from '../pasteParse'
import { ErrorBar } from './shared'

/** 核验面板状态机:未核验 → 查询中 → 查得(比对)/查无/通道异常 */
type VerifyState =
  | { kind: 'loading' }
  | { kind: 'found'; o: RegistryOfficialT }
  | { kind: 'miss' }
  | { kind: 'error'; msg: string }

export type ProfileDraft = Omit<ProfileT, 'id' | 'verifiedAt'>

export const EMPTY_DRAFT: ProfileDraft = {
  type: 'COMPANY',
  title: '',
  taxNo: '',
  regAddress: '',
  regPhone: '',
  bankName: '',
  bankAccount: '',
}

/** 抬头表单弹窗(新增/编辑共用):ProfilesPage 管理 + ApplyPage 就地新增复用。
 *  保存成功回调 onSaved(id),父组件决定关闭/选中/刷新。 */
export function ProfileFormModal({
  id,
  initial,
  onClose,
  onSaved,
}: {
  id: string | null
  initial: ProfileDraft
  onClose: () => void
  onSaved: (id: string) => void
}) {
  const [draft, setDraft] = useState(initial)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [check, setCheck] = useState<VerifyState | null>(null)
  const [pasteText, setPasteText] = useState('')
  const [pasteMsg, setPasteMsg] = useState('')

  const set = (patch: Partial<ProfileDraft>) => setDraft((d) => ({ ...d, ...patch }))

  const aiSeq = useRef(0)
  const aiTimer = useRef<number | undefined>(undefined)
  const lastAiText = useRef('')

  /** AI 兜底:规则识别吃不下时静默升级(自家中转 LLM);竞态用序号守卫,同文本只调一次;
   *  force=手动按钮显式触发,跳过同文本去重(用户点了就是要重试) */
  const runAiParse = (value: string, ruleCount: number, force = false) => {
    const text = value.trim()
    if (text.length < 10 || (!force && text === lastAiText.current)) return
    lastAiText.current = text
    const seq = ++aiSeq.current
    setPasteMsg(t('invoice.profiles.pasteAiLoading'))
    const fallbackMsg = () =>
      ruleCount > 0
        ? t('invoice.profiles.pasteApplied', { n: String(ruleCount) })
        : t('invoice.profiles.pasteNone')
    api
      .pasteAiParse(text)
      .then((r) => {
        if (seq !== aiSeq.current) return
        const patch = r.found && r.fields ? aiParsedPatch(r.fields) : {}
        const n = Object.values(patch).filter(Boolean).length
        if (n > 0) {
          set(patch)
          setPasteMsg(t('invoice.profiles.pasteAiApplied', { n: String(n) }))
        } else {
          setPasteMsg(fallbackMsg())
        }
      })
      .catch(() => {
        // 未达门槛/超配额/通道未配——都静默退回规则识别的结果,粘贴功能不因 AI 缺席而失灵
        if (seq === aiSeq.current) setPasteMsg(fallbackMsg())
      })
  }

  /** 智能粘贴级联:规则先跑(零延迟零成本)。「够用」看质量不看数量——只有干净抬头+过
   *  校验位的税号都在手才不升级;否则 700ms 防抖后静默走 AI(手动按钮随时可强制) */
  const handlePasteText = (value: string) => {
    setPasteText(value)
    window.clearTimeout(aiTimer.current)
    if (value.trim().length < 8) {
      setPasteMsg('')
      return
    }
    const parsed = parseInvoiceInfo(value)
    const count = Object.values(parsed).filter(Boolean).length
    if (count > 0) set(parsed)
    const rulesEnough = !!(parsed.title && parsed.taxNo)
    if (rulesEnough) {
      setPasteMsg(t('invoice.profiles.pasteApplied', { n: String(count) }))
      return
    }
    setPasteMsg(count > 0
      ? t('invoice.profiles.pasteApplied', { n: String(count) })
      : t('invoice.profiles.pasteAiLoading'))
    aiTimer.current = window.setTimeout(() => runAiParse(value, count), 700)
  }

  /** 手动强制 AI 识别(按钮):不管规则判定,直接走 LLM;服务端配额照常把门 */
  const forceAiParse = () => {
    window.clearTimeout(aiTimer.current)
    const count = Object.values(parseInvoiceInfo(pasteText)).filter(Boolean).length
    runAiParse(pasteText, count, true)
  }

  // 税号填了但格式不合法(18 位国标校验位/15 位老号,与后端同规则)——红提示 + 禁保存
  const taxNoBad = !!(draft.taxNo ?? '').trim() && !isValidTaxNo(draft.taxNo ?? '')

  const incomplete =
    !draft.title.trim() || (draft.type === 'COMPANY' && !(draft.taxNo ?? '').trim()) || taxNoBad

  /** 官方档案核验:查得后逐字段比对 + 自动补全空字段,不拦保存(核验是辅助不是门槛)。
   *  用户只需要输全称——税号/地址/开户行都能由这里带出(必填的税号也算回填满足)。 */
  const verify = async () => {
    setCheck({ kind: 'loading' })
    try {
      const r = await api.registryLookup(draft.title.trim())
      if (r.found && r.official) {
        setCheck({ kind: 'found', o: r.official })
        fillEmptyFrom(r.official)
      } else {
        setCheck({ kind: 'miss' })
      }
    } catch (e) {
      setCheck({ kind: 'error', msg: String((e as Error).message) })
    }
  }

  /** 只补空:官方档案填进还空着的字段,已填内容一律不动(开票地址与注册地址不同是合法场景);
   *  名称不动——它是用户输入的查询键,对不上由 ✗ 提示、按回填按钮显式覆盖 */
  const fillEmptyFrom = (o: RegistryOfficialT) => {
    const keep = (mine: string | null | undefined, official: string | null) =>
      (mine ?? '').trim() ? (mine as string) : (official ?? mine ?? '')
    setDraft((d) => ({
      ...d,
      taxNo: keep(d.taxNo, o.taxNo),
      regAddress: keep(d.regAddress, o.address),
      regPhone: keep(d.regPhone, o.phone),
      bankName: keep(d.bankName, o.bankName),
      bankAccount: keep(d.bankAccount, o.bankAccount),
    }))
  }

  /** 一键回填(纠错用):官方档案有值的字段全部覆盖,含名称;残缺字段保留手填 */
  const fillFromOfficial = () => {
    if (check?.kind !== 'found') return
    const o = check.o
    set({
      title: o.name ?? draft.title,
      taxNo: o.taxNo ?? draft.taxNo,
      regAddress: o.address ?? draft.regAddress,
      regPhone: o.phone ?? draft.regPhone,
      bankName: o.bankName ?? draft.bankName,
      bankAccount: o.bankAccount ?? draft.bankAccount,
    })
  }

  const nameMatches = check?.kind === 'found' && draft.title.trim() === (check.o.name ?? '')
  const taxMatches =
    check?.kind === 'found' &&
    !!check.o.taxNo &&
    (draft.taxNo ?? '').trim().toUpperCase() === check.o.taxNo.toUpperCase()

  const save = async () => {
    setSaving(true)
    setError('')
    try {
      if (id) {
        await api.updateProfile(id, draft)
        onSaved(id)
      } else {
        const r = await api.createProfile(draft)
        onSaved(r.id)
      }
    } catch (e) {
      setError(String((e as Error).message))
      setSaving(false)
    }
  }

  const fields: { key: keyof ProfileDraft; label: string; hint?: string; companyOnly?: boolean }[] = [
    { key: 'taxNo', label: t('invoice.profiles.taxNo'), hint: t('invoice.profiles.taxHint'), companyOnly: true },
    { key: 'regAddress', label: t('invoice.profiles.regAddress'), companyOnly: true },
    { key: 'regPhone', label: t('invoice.profiles.regPhone'), companyOnly: true },
    { key: 'bankName', label: t('invoice.profiles.bankName'), companyOnly: true },
    { key: 'bankAccount', label: t('invoice.profiles.bankAccount'), companyOnly: true },
  ]

  return (
    <Modal open onClose={onClose} title={id ? t('invoice.profiles.editTitle') : t('invoice.profiles.add')}>
      <div className="space-y-3">
        {error && <ErrorBar msg={error} />}
        <div className="flex gap-2">
          {(['COMPANY', 'PERSONAL'] as const).map((type) => (
            <Button
              key={type}
              size="sm"
              variant={draft.type === type ? 'primary' : 'secondary'}
              onClick={() => {
                set({ type })
                if (type !== 'COMPANY') {
                  setCheck(null)
                  setPasteText('')
                  setPasteMsg('')
                }
              }}
            >
              {type === 'COMPANY' ? t('invoice.profiles.typeCompany') : t('invoice.profiles.typePersonal')}
            </Button>
          ))}
        </div>
        {draft.type === 'COMPANY' && (
          <div>
            <textarea
              className="iv-paste"
              rows={2}
              value={pasteText}
              onChange={(e) => handlePasteText(e.target.value)}
              placeholder={t('invoice.profiles.pastePlaceholder')}
            />
            <div className="mt-0.5 flex items-center gap-2">
              {pasteMsg && <span className="min-w-0 flex-1 text-xs text-snb-t3">{pasteMsg}</span>}
              {pasteText.trim().length >= 10 && (
                <Button size="xs" variant="ghost" onClick={forceAiParse}>
                  {t('invoice.profiles.pasteAiButton')}
                </Button>
              )}
            </div>
          </div>
        )}
        <label className="block">
          <span className="mb-1 block text-xs text-snb-t3">
            {t('invoice.profiles.title')}
            <b className="ml-0.5" style={{ color: 'var(--iv-no-red)' }}>*</b>
          </span>
          <Input
            value={draft.title}
            onChange={(e) => set({ title: e.target.value })}
            placeholder={t(draft.type === 'COMPANY'
              ? 'invoice.profiles.titleHintCompany'
              : 'invoice.profiles.titleHintPersonal')}
          />
        </label>
        {draft.type === 'COMPANY' && (
          <div className="iv-verify">
            <div className="flex flex-wrap items-center gap-2">
              <Button
                size="sm"
                variant="secondary"
                disabled={check?.kind === 'loading' || draft.title.trim().length < 4}
                onClick={verify}
              >
                {t('invoice.profiles.verify')}
              </Button>
              {check?.kind === 'loading' && (
                <span className="text-xs text-snb-t3">{t('invoice.profiles.verifying')}</span>
              )}
              {check?.kind === 'miss' && (
                <span className="text-xs" style={{ color: 'var(--iv-seal)' }}>
                  {t('invoice.profiles.verifyMiss')}
                </span>
              )}
              {check?.kind === 'error' && (
                <span className="text-xs" style={{ color: 'var(--iv-seal)' }}>{check.msg}</span>
              )}
            </div>
            {check?.kind === 'found' && (
              <div className="mt-2.5 space-y-1.5">
                <div className="iv-verify-head">{t('invoice.profiles.verifyOfficial')}</div>
                <div className="iv-verify-row">
                  <span className="k">{t('invoice.profiles.title')}</span>
                  <span className="v">{check.o.name ?? '—'}</span>
                  <span className={nameMatches ? 'iv-verify-ok' : 'iv-verify-bad'}>
                    {t(nameMatches ? 'invoice.profiles.verifyMatch' : 'invoice.profiles.verifyDiff')}
                  </span>
                </div>
                <div className="iv-verify-row">
                  <span className="k">{t('invoice.profiles.taxNo')}</span>
                  <span className="v font-mono">{check.o.taxNo ?? '—'}</span>
                  <span className={taxMatches ? 'iv-verify-ok' : 'iv-verify-bad'}>
                    {t(taxMatches ? 'invoice.profiles.verifyMatch' : 'invoice.profiles.verifyDiff')}
                  </span>
                </div>
                {check.o.address && (
                  <div className="iv-verify-row">
                    <span className="k">{t('invoice.profiles.regAddress')}</span>
                    <span className="v">{check.o.address}</span>
                  </div>
                )}
                {check.o.bankName && (
                  <div className="iv-verify-row">
                    <span className="k">{t('invoice.profiles.bankName')}</span>
                    <span className="v">{[check.o.bankName, check.o.bankAccount].filter(Boolean).join(' · ')}</span>
                  </div>
                )}
                <Button size="xs" variant="ghost" onClick={fillFromOfficial}>
                  {t('invoice.profiles.verifyFill')}
                </Button>
              </div>
            )}
            <p className="iv-verify-note">{t('invoice.profiles.verifyNote')}</p>
          </div>
        )}
        {fields
          .filter((f) => !f.companyOnly || draft.type === 'COMPANY')
          .map((f) => (
            <label key={f.key} className="block">
              <span className="mb-1 block text-xs text-snb-t3">
                {f.label}
                {f.key === 'taxNo' && <b className="ml-0.5" style={{ color: 'var(--iv-no-red)' }}>*</b>}
              </span>
              <Input
                value={draft[f.key] ?? ''}
                onChange={(e) => set({ [f.key]: e.target.value } as Partial<ProfileDraft>)}
                placeholder={f.hint ?? ''}
              />
              {f.key === 'taxNo' && taxNoBad && (
                <span className="mt-1 block text-xs" style={{ color: 'var(--iv-seal)' }}>
                  {t('invoice.profiles.taxNoInvalid')}
                </span>
              )}
            </label>
          ))}
        <div className="flex justify-end gap-2 pt-1">
          <Button variant="secondary" onClick={onClose}>{t('invoice.profiles.cancel')}</Button>
          <Button variant="primary" disabled={saving || incomplete} onClick={save}>
            {t('invoice.profiles.save')}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
