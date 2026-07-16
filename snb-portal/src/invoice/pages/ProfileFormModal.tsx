import { useEffect, useRef, useState } from 'react'
import { Button, Modal } from '../../ui'
import { t } from '../../i18n'
import { api, type ProfileT, type RegistryOfficialT } from '../api'
import { isValidTaxNo } from '../taxno'
import { aiParsedPatch, type ParsedInvoiceInfo } from '../pasteParse'
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

/** 誊写落格顺序(AI 识别/核验带出/回填共用):按票面行序阶梯闪光 */
const PATCH_ORDER = ['title', 'taxNo', 'regAddress', 'regPhone', 'bankName', 'bankAccount'] as const
type PatchKey = (typeof PATCH_ORDER)[number]

/** 抬头表单弹窗(新增/编辑共用):ProfilesPage 管理 + ApplyPage 就地新增复用。
 *  2026-07-17 重设计(静态稿站长过目定稿):表单本体=购买方信息格,
 *  三条动线=来料区(粘贴→AI 誊写)/名称行「核验」(官方底单)/手填。
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
  const [pasteMsg, setPasteMsg] = useState<{ text: string; bad?: boolean }>({ text: '' })
  const [aiBusy, setAiBusy] = useState(false)
  const [flashed, setFlashed] = useState<Record<string, boolean>>({})

  const set = (patch: Partial<ProfileDraft>) => setDraft((d) => ({ ...d, ...patch }))
  const isCo = draft.type === 'COMPANY'

  const aiSeq = useRef(0)
  const timers = useRef<number[]>([])
  useEffect(() => () => timers.current.forEach(window.clearTimeout), [])
  const draftRef = useRef(draft)
  useEffect(() => {
    draftRef.current = draft
  }, [draft])

  /* ---------- textarea 自动长高(永不出滚动条;来料区超上限才细条内滚) ---------- */
  const pasteRef = useRef<HTMLTextAreaElement>(null)
  const addrRef = useRef<HTMLTextAreaElement>(null)
  const autoGrow = (el: HTMLTextAreaElement | null, cap?: number) => {
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, cap ?? 9999)}px`
    if (cap) el.classList.toggle('of', el.scrollHeight > cap)
  }
  useEffect(() => autoGrow(addrRef.current), [draft.regAddress, isCo])
  useEffect(() => autoGrow(pasteRef.current, 150), [pasteText, isCo])

  /* ---------- 誊写:补丁按票面行序逐格落格,90ms 阶梯 + 落格闪光 ---------- */
  const flashField = (k: string) => {
    setFlashed((f) => ({ ...f, [k]: true }))
    timers.current.push(window.setTimeout(() => setFlashed((f) => ({ ...f, [k]: false })), 950))
  }
  const applyPatch = (patch: ParsedInvoiceInfo, onlyEmpty = false) => {
    let i = 0
    for (const k of PATCH_ORDER) {
      const v = patch[k]
      if (!v) continue
      if (onlyEmpty && String(draftRef.current[k] ?? '').trim()) continue
      timers.current.push(
        window.setTimeout(() => {
          setDraft((d) => (onlyEmpty && String(d[k] ?? '').trim() ? d : { ...d, [k]: v }))
          flashField(k)
        }, i++ * 90)
      )
    }
    return i
  }

  /* ---------- 来料区:「用 AI 识别」唯一触发口(站长拍板不做自动,点了才花一次调用) ----------
     自家中转 LLM 拆字段;进行中禁按钮防连点,竞态再用序号守卫兜底;
     超长截 2000 字(后端同上限);服务端资格闸+配额照常把门 */
  const runAiParse = () => {
    const text = pasteText.trim().slice(0, 2000)
    if (text.length < 10) return
    const seq = ++aiSeq.current
    setAiBusy(true)
    setPasteMsg({ text: t('invoice.profiles.pasteAiLoading') })
    api
      .pasteAiParse(text)
      .then((r) => {
        if (seq !== aiSeq.current) return
        const patch = r.found && r.fields ? aiParsedPatch(r.fields) : {}
        const n = applyPatch(patch)
        if (n > 0) {
          setPasteMsg({ text: t('invoice.profiles.pasteAiApplied', { n: String(n) }) })
        } else {
          setPasteMsg({ text: t('invoice.profiles.pasteNone'), bad: true })
        }
      })
      .catch(() => {
        // 未达门槛/超配额/通道未配——识别失败要说清,别冒充「没识别出」
        if (seq === aiSeq.current) setPasteMsg({ text: t('invoice.profiles.pasteAiFailed'), bad: true })
      })
      .finally(() => {
        if (seq === aiSeq.current) setAiBusy(false)
      })
  }

  // 税号填了但格式不合法(18 位国标校验位/15 位老号,与后端同规则)——红格线 + 禁保存
  const taxNoBad = !!(draft.taxNo ?? '').trim() && !isValidTaxNo(draft.taxNo ?? '')

  const incomplete =
    !draft.title.trim() || (isCo && !(draft.taxNo ?? '').trim()) || taxNoBad

  /* ---------- 核验:官方底单(复核联)。查得后空字段自动带出,不拦保存 ---------- */
  const verify = async () => {
    setCheck({ kind: 'loading' })
    try {
      const r = await api.registryLookup(draft.title.trim())
      if (r.found && r.official) {
        setCheck({ kind: 'found', o: r.official })
        // 只补空:官方档案填进还空着的字段,已填内容一律不动(开票地址≠注册地址是合法场景);
        // 名称不动——它是查询键,对不上由 ✗ 提示、按回填按钮显式覆盖
        applyPatch(
          {
            taxNo: r.official.taxNo ?? undefined,
            regAddress: r.official.address ?? undefined,
            regPhone: r.official.phone ?? undefined,
            bankName: r.official.bankName ?? undefined,
            bankAccount: r.official.bankAccount ?? undefined,
          },
          true
        )
      } else {
        setCheck({ kind: 'miss' })
      }
    } catch (e) {
      setCheck({ kind: 'error', msg: String((e as Error).message) })
    }
  }

  /** 一键回填(纠错用):官方档案有值的字段全部覆盖,含名称;残缺字段保留手填 */
  const fillFromOfficial = () => {
    if (check?.kind !== 'found') return
    const o = check.o
    applyPatch({
      title: o.name ?? undefined,
      taxNo: o.taxNo ?? undefined,
      regAddress: o.address ?? undefined,
      regPhone: o.phone ?? undefined,
      bankName: o.bankName ?? undefined,
      bankAccount: o.bankAccount ?? undefined,
    })
  }

  const nameMatches = check?.kind === 'found' && draft.title.trim() === (check.o.name ?? '')
  const taxMatches =
    check?.kind === 'found' &&
    !!check.o.taxNo &&
    (draft.taxNo ?? '').trim().toUpperCase() === check.o.taxNo.toUpperCase()
  // 章预览:名称+税号都对上官方档案,保存后即真盖章
  const stampPreview = isCo && nameMatches && taxMatches

  const titleLen = draft.title.trim().length
  const verifyReady = titleLen >= 4 && check?.kind !== 'found' && check?.kind !== 'loading'

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

  const setType = (type: ProfileDraft['type']) => {
    set({ type })
    if (type !== 'COMPANY') {
      aiSeq.current++ // 在途 AI 作废,别往个人抬头里回填企业字段
      setCheck(null)
      setPasteText('')
      setPasteMsg({ text: '' })
      setAiBusy(false)
    }
  }

  const fpin = (k: PatchKey, extra = '') =>
    `iv-fpin${extra}${flashed[k] ? ' flash' : ''}`

  return (
    <Modal
      open
      onClose={onClose}
      title={
        <>
          <span className="iv-m-eyebrow">{t('invoice.profiles.eyebrow')}</span>
          {id ? t('invoice.profiles.editTitle') : t('invoice.profiles.add')}
        </>
      }
      footer={
        <>
          <span className="min-w-0 flex-1 text-xs" style={{ color: 'var(--iv-no-red)' }}>
            {taxNoBad ? t('invoice.profiles.footWarnTax') : ''}
          </span>
          <Button variant="secondary" onClick={onClose}>{t('invoice.profiles.cancel')}</Button>
          <Button variant="primary" disabled={saving || incomplete} onClick={save}>
            {t('invoice.profiles.save')}
          </Button>
        </>
      }
    >
      <div className="space-y-3.5">
        {error && <ErrorBar msg={error} />}

        {/* 联次:企业/个人 */}
        <div className="flex gap-2" role="group" aria-label={t('invoice.profiles.eyebrow')}>
          {(['COMPANY', 'PERSONAL'] as const).map((type) => (
            <button
              key={type}
              type="button"
              className="iv-type-tab"
              aria-pressed={draft.type === type}
              onClick={() => setType(type)}
            >
              {type === 'COMPANY'
                ? t('invoice.profiles.typeCompanyTab')
                : t('invoice.profiles.typePersonalTab')}
            </button>
          ))}
        </div>

        {/* 来料区(仅企业):粘贴对方发来的资料 → 点「用 AI 识别」誊写进票面 */}
        {isCo && (
          <div className="iv-tray">
            <span className="iv-tray-tag">{t('invoice.profiles.trayTag')}</span>
            <textarea
              ref={pasteRef}
              rows={2}
              value={pasteText}
              onChange={(e) => {
                setPasteText(e.target.value)
                if (e.target.value.trim().length < 10) setPasteMsg({ text: '' })
              }}
              placeholder={t('invoice.profiles.pastePlaceholder')}
            />
            <div className="iv-tray-foot">
              {pasteMsg.text && (
                <span className={`iv-tray-msg${pasteMsg.bad ? ' bad' : ''}`}>{pasteMsg.text}</span>
              )}
              {pasteText.trim().length >= 10 && (
                <button type="button" className="iv-tool lg" disabled={aiBusy} onClick={runAiParse}>
                  {t(aiBusy ? 'invoice.profiles.pasteAiBusy' : 'invoice.profiles.pasteAiButton')}
                </button>
              )}
            </div>
          </div>
        )}

        {/* 购买方信息格:表单即票面 */}
        <div className="iv-blk">
          {stampPreview && (
            <span className="iv-blk-stamp">
              <span className="tip">{t('invoice.profiles.stampTip')}</span>
              <span className="chip">{t('invoice.profiles.verifiedBadge')}</span>
            </span>
          )}
          <span className="iv-blk-side" aria-hidden="true">购买方</span>
          <div className="iv-blk-cell">
            <div className="iv-fprow">
              <span className="lb">
                {t('invoice.profiles.rowTitle')}
                <i>*</i>
              </span>
              <input
                className={fpin('title')}
                value={draft.title}
                onChange={(e) => set({ title: e.target.value })}
                placeholder={t(isCo
                  ? 'invoice.profiles.titleHintCompany'
                  : 'invoice.profiles.titleHintPersonal')}
                autoComplete="organization"
              />
              {isCo && (
                <button
                  type="button"
                  className={`iv-tool${verifyReady ? ' ready' : ''}`}
                  disabled={titleLen < 4 || check?.kind === 'loading'}
                  onClick={verify}
                >
                  {t(check?.kind === 'found'
                    ? 'invoice.profiles.verifyAgain'
                    : 'invoice.profiles.verify')}
                </button>
              )}
            </div>
            {isCo && (
              <>
                <div className="iv-fprow">
                  <span className="lb">
                    {t('invoice.profiles.rowTaxNo')}
                    <i>*</i>
                  </span>
                  <input
                    className={fpin('taxNo', ' mono') + (taxNoBad ? ' bad' : '')}
                    value={draft.taxNo ?? ''}
                    onChange={(e) => set({ taxNo: e.target.value })}
                    placeholder={t('invoice.profiles.taxHint')}
                    maxLength={20}
                  />
                </div>
                {taxNoBad && <div className="iv-fperr">{t('invoice.profiles.taxNoInvalid')}</div>}
                <div className="iv-fprow tall">
                  <span className="lb">{t('invoice.profiles.rowAddrPhone')}</span>
                  <div className="iv-fpcol">
                    <textarea
                      ref={addrRef}
                      rows={1}
                      className={fpin('regAddress')}
                      value={draft.regAddress ?? ''}
                      onChange={(e) => set({ regAddress: e.target.value })}
                      placeholder={t('invoice.profiles.regAddress')}
                    />
                    <input
                      className={fpin('regPhone', ' half')}
                      value={draft.regPhone ?? ''}
                      onChange={(e) => set({ regPhone: e.target.value })}
                      placeholder={t('invoice.profiles.regPhone')}
                    />
                  </div>
                </div>
                <div className="iv-fprow tall">
                  <span className="lb">{t('invoice.profiles.rowBankAcct')}</span>
                  <div className="iv-fpcol">
                    <input
                      className={fpin('bankName')}
                      value={draft.bankName ?? ''}
                      onChange={(e) => set({ bankName: e.target.value })}
                      placeholder={t('invoice.profiles.bankName')}
                    />
                    <input
                      className={fpin('bankAccount', ' half mono')}
                      value={draft.bankAccount ?? ''}
                      onChange={(e) => set({ bankAccount: e.target.value })}
                      placeholder={t('invoice.profiles.bankAccount')}
                    />
                  </div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* 官方底单(核验结果·复核联) */}
        {isCo && check?.kind === 'loading' && (
          <div className="iv-carbon dim">{t('invoice.profiles.verifying')}</div>
        )}
        {isCo && check?.kind === 'miss' && (
          <div className="iv-carbon miss">{t('invoice.profiles.verifyMiss')}</div>
        )}
        {isCo && check?.kind === 'error' && (
          <div className="iv-carbon miss">{check.msg}</div>
        )}
        {isCo && check?.kind === 'found' && (
          <div className="iv-carbon">
            <div className="iv-carbon-head">{t('invoice.profiles.verifyOfficial')}</div>
            <div className="iv-carbon-row">
              <span className="k">{t('invoice.profiles.rowTitle')}</span>
              <span className="v">{check.o.name ?? '—'}</span>
              <span className={nameMatches ? 'mk-ok' : 'mk-bad'}>
                {t(nameMatches ? 'invoice.profiles.verifyMatch' : 'invoice.profiles.verifyDiff')}
              </span>
            </div>
            <div className="iv-carbon-row">
              <span className="k">{t('invoice.profiles.taxNo')}</span>
              <span className="v mono">{check.o.taxNo ?? '—'}</span>
              <span className={taxMatches ? 'mk-ok' : 'mk-bad'}>
                {t(taxMatches ? 'invoice.profiles.verifyMatch' : 'invoice.profiles.verifyDiff')}
              </span>
            </div>
            {(check.o.address || check.o.bankName) && (
              <div className="iv-carbon-ref">
                {check.o.address &&
                  `${t('invoice.profiles.carbonAddr')}：${check.o.address}${check.o.phone ? ` ${check.o.phone}` : ''}`}
                {check.o.address && check.o.bankName && '　·　'}
                {check.o.bankName &&
                  `${t('invoice.profiles.carbonBank')}：${check.o.bankName}${check.o.bankAccount ? ` ${check.o.bankAccount}` : ''}`}
              </div>
            )}
            <div className="iv-carbon-foot">
              <button type="button" className="iv-tool" onClick={fillFromOfficial}>
                {t('invoice.profiles.verifyFill')}
              </button>
              <span className="hint">{t('invoice.profiles.stampHint')}</span>
            </div>
          </div>
        )}

        {isCo && <p className="iv-blk-note">{t('invoice.profiles.verifyNote')}</p>}
      </div>
    </Modal>
  )
}
