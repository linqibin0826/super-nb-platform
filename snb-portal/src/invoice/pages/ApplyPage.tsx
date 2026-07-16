import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button } from '../../ui'
import { t } from '../../i18n'
import { api, InvoiceApiError, InvoiceAuthError, type OverviewT, type ProfileT } from '../api'
import { feeCents, fmtYuan, fmtYuanGrouped, rmbUpper, selectedTotalCents } from '../fee'
import { ErrorBar, Loading, PageHead } from './shared'
import { EMPTY_DRAFT, ProfileFormModal } from './ProfileFormModal'
import { loginUrl } from '../../auth/apiFetch'

const today = () => new Date().toLocaleDateString('sv')

/** 申请开票(填开联):整页一张居中票面,订单即明细行——
 *  在票上勾明细,合计/大写实时更新;跨过免收线盖「免」章(挂载即动画)。 */
export function ApplyPage() {
  const [overview, setOverview] = useState<OverviewT | null>(null)
  const [profiles, setProfiles] = useState<ProfileT[] | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [profileId, setProfileId] = useState('')
  const [remark, setRemark] = useState('')
  const [error, setError] = useState('')
  const [needLogin, setNeedLogin] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [doneNo, setDoneNo] = useState('')
  const [adding, setAdding] = useState(false)

  useEffect(() => {
    Promise.all([api.orders(), api.profiles()])
      .then(([ov, ps]) => {
        setOverview(ov)
        setProfiles(ps)
        setSelected(new Set(ov.orders.map((o) => o.orderId))) // 默认全选
        if (ps.length > 0) setProfileId(ps[0].id)
      })
      .catch((e) => (e instanceof InvoiceAuthError ? setNeedLogin(true) : setError(String(e.message))))
  }, [])

  const totalCents = useMemo(
    () => (overview ? selectedTotalCents(overview.orders, selected) : 0),
    [overview, selected],
  )
  const fee = overview ? feeCents(totalCents, overview) : 0
  const minCents = overview ? Math.round(overview.minTotal * 100) : 0
  const freeCentsAt = overview ? Math.round(overview.freeThreshold * 100) : 0
  const balanceCents = overview ? Math.round(overview.balance * 100) : 0
  const belowMin = totalCents < minCents
  const feeShort = fee > 0 && balanceCents < fee
  const isFree = totalCents >= freeCentsAt && totalCents > 0

  const head = (
    <PageHead eyebrow={t('invoice.apply.eyebrow')} title={t('invoice.tabs.apply')} sub={t('invoice.apply.intro')} />
  )

  if (needLogin) {
    return (
      <>
        {head}
        <div className="rounded-xl border-[1.5px] border-dashed border-snb-hairline-strong p-14 text-center text-sm text-snb-t2">
          <a className="underline underline-offset-4 hover:text-snb-t1" href={loginUrl()}>
            {t('invoice.nav.login')}
          </a>
        </div>
      </>
    )
  }
  if (error && !overview) return <>{head}<ErrorBar msg={error} /></>
  if (!overview || !profiles) return <>{head}<Loading /></>

  // 提交成功:票据回执
  if (doneNo) {
    return (
      <>
        {head}
        <div className="iv-fapiao mx-auto mt-8 max-w-md px-8 py-9 text-center">
          <div className="iv-fp-title">{t('invoice.apply.doneTitle')}</div>
          <div className="iv-fp-title-rule" />
          <div className="mt-5 font-mono text-lg font-semibold" style={{ color: 'var(--iv-no-red)' }}>
            {doneNo}
          </div>
          <p className="mt-3 text-sm text-snb-t2">{t('invoice.apply.processNote')}</p>
          <Link to="/requests" className="mt-6 inline-block">
            <Button variant="primary">{t('invoice.apply.goRequests')}</Button>
          </Link>
        </div>
      </>
    )
  }

  const toggle = (id: string) => {
    const next = new Set(selected)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    setSelected(next)
  }

  const submit = async () => {
    setSubmitting(true)
    setError('')
    try {
      const result = await api.createRequest([...selected], profileId, remark.trim())
      setDoneNo(result.requestNo)
    } catch (e) {
      if (e instanceof InvoiceApiError && e.status === 409) {
        setError(t('invoice.apply.pendingExists'))
      } else {
        setError(String((e as Error).message))
      }
    } finally {
      setSubmitting(false)
    }
  }

  const profile = profiles.find((p) => p.id === profileId)
  const allOn = overview.orders.length > 0 && selected.size === overview.orders.length

  const warn =
    totalCents > 0 && belowMin
      ? t('invoice.apply.belowMin', { diff: fmtYuan(minCents - totalCents) })
      : profiles.length === 0
        ? t('invoice.apply.needProfile')
        : feeShort
          ? t('invoice.apply.feeShort', { fee: fmtYuan(fee) })
          : ''

  return (
    <>
      {head}
      {error && <div className="mx-auto mb-4 max-w-3xl"><ErrorBar msg={error} /></div>}
      <div className="iv-fapiao mx-auto max-w-3xl">
        <div className="iv-fp-title">{t('invoice.apply.docTitle')}</div>
        <div className="iv-fp-title-rule" />
        <div className="iv-fp-no">
          <span>
            {t('invoice.apply.noLabel')} <b>{t('invoice.apply.noPending')}</b>
          </span>
          <span>{today()}</span>
        </div>

        <div className="iv-fp-grid">
          {/* 购买方 */}
          <div className="iv-fp-row">
            <div className="iv-fp-side">{t('invoice.apply.buyerSide')}</div>
            <div className="iv-fp-cell">
              {profiles.length === 0 ? (
                <button type="button" className="iv-fp-addpf" onClick={() => setAdding(true)}>
                  <b>＋ {t('invoice.apply.addProfileCta')}</b>
                  <span>{t('invoice.apply.addProfileHint')}</span>
                </button>
              ) : (
                <>
                  <div className="iv-fp-field">
                    <span className="lb">{t('invoice.apply.nameLabel')}</span>
                    <select
                      className="iv-fp-select"
                      value={profileId}
                      onChange={(e) => setProfileId(e.target.value)}
                      aria-label={t('invoice.apply.profile')}
                    >
                      {profiles.map((p) => (
                        <option key={p.id} value={p.id}>{p.title}</option>
                      ))}
                    </select>
                    <Button size="xs" variant="ghost" title={t('invoice.profiles.add')} onClick={() => setAdding(true)}>
                      ＋
                    </Button>
                  </div>
                  <div className="iv-fp-field">
                    <span className="lb">{t('invoice.apply.taxLabel')}</span>
                    <span className={`vl font-mono ${profile?.taxNo ? '' : 'dim'}`}>
                      {profile ? profile.taxNo || t('invoice.profiles.noTax') : '—'}
                    </span>
                    {profile?.verifiedAt && (
                      <span className="iv-stamp-verified" title={t('invoice.profiles.verifiedTip')}>
                        {t('invoice.profiles.verifiedBadge')}
                      </span>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>

          {/* 项目明细:订单即商品行,表头勾选=全选 */}
          <div className="iv-fp-row">
            <div className="iv-fp-side">{t('invoice.apply.itemSide')}</div>
            <div className="iv-fp-cell !p-0">
              {overview.orders.length === 0 ? (
                <div className="px-4 py-10 text-center text-[13px] text-snb-t3">{t('invoice.apply.empty')}</div>
              ) : (
                <>
                  <div className="iv-fp-ord-head">
                    <input
                      type="checkbox"
                      checked={allOn}
                      title={t('invoice.apply.selectAll')}
                      aria-label={t('invoice.apply.selectAll')}
                      onChange={(e) =>
                        setSelected(e.target.checked ? new Set(overview.orders.map((o) => o.orderId)) : new Set())
                      }
                    />
                    <span className="sel">{t('invoice.apply.selectAll')}</span>
                    <span className="nm">{t('invoice.apply.itemName')}</span>
                    <span className="meta">
                      {t('invoice.apply.ordMeta', {
                        n: String(selected.size),
                        total: '¥' + fmtYuanGrouped(totalCents),
                      })}
                    </span>
                  </div>
                  <div className="iv-fp-orders">
                    {overview.orders.map((o) => (
                      <label key={o.orderId} className={`iv-fp-ord ${selected.has(o.orderId) ? '' : 'off'}`}>
                        <input
                          type="checkbox"
                          checked={selected.has(o.orderId)}
                          onChange={() => toggle(o.orderId)}
                        />
                        <span className="d">{new Date(o.completedAt).toLocaleDateString('sv')}</span>
                        <span className="no">{o.orderNo}</span>
                        <span className="amt">¥{fmtYuanGrouped(Math.round(o.amount * 100))}</span>
                      </label>
                    ))}
                  </div>
                </>
              )}
            </div>
          </div>

          {/* 合计 */}
          <div className="iv-fp-row">
            <div className="iv-fp-side">{t('invoice.apply.sumSide')}</div>
            <div className="iv-fp-cell">
              {totalCents > 0 ? (
                <div className="iv-fp-cap">
                  <span className="iv-fp-cap-sm">{t('invoice.apply.capitalPrefix')}</span>
                  {rmbUpper(totalCents)}
                </div>
              ) : (
                <div className="iv-fp-cap dim">
                  <span className="iv-fp-cap-sm">{t('invoice.apply.capitalPrefix')}</span>
                  {t('invoice.apply.capitalEmpty')}
                </div>
              )}
              <div className="iv-fp-num">¥{fmtYuanGrouped(totalCents)}</div>
            </div>
          </div>

          {/* 费用 */}
          <div className="iv-fp-row">
            <div className="iv-fp-side">{t('invoice.apply.feeSide')}</div>
            <div className="iv-fp-cell">
              <div className="iv-fee-line">
                <span>{t('invoice.apply.feeLabel')}</span>
                {isFree ? (
                  <>
                    <span className="strike font-mono">
                      ¥{fmtYuanGrouped(Math.round(totalCents * overview.feeRate))}
                    </span>
                    <b className="iv-fee-free font-mono">¥0.00</b>
                    <span className="iv-stamp-mian pop">免</span>
                  </>
                ) : (
                  <b className="font-mono">{totalCents >= minCents ? '¥' + fmtYuanGrouped(fee) : '—'}</b>
                )}
              </div>
              <div className="iv-fee-note">
                {isFree
                  ? t('invoice.apply.feeFreeNote')
                  : t('invoice.apply.feeNoteWithBal', { balance: '¥' + fmtYuanGrouped(balanceCents) })}
              </div>
            </div>
          </div>

          {/* 备注 */}
          <div className="iv-fp-row">
            <div className="iv-fp-side">{t('invoice.apply.remarkSide')}</div>
            <div className="iv-fp-cell">
              <textarea
                className="iv-fp-remark"
                value={remark}
                onChange={(e) => setRemark(e.target.value)}
                maxLength={200}
                placeholder={t('invoice.apply.remark')}
              />
            </div>
          </div>
        </div>

        <div className="mt-3.5 flex items-center gap-3">
          <div className="iv-fp-hint">{t('invoice.apply.previewHint')}</div>
          <Button
            variant="primary"
            disabled={submitting || belowMin || feeShort || selected.size === 0 || !profileId}
            onClick={submit}
          >
            {t('invoice.apply.submit')}
          </Button>
        </div>
        <div className="iv-warn">{warn}</div>
      </div>

      {adding && (
        <ProfileFormModal
          id={null}
          initial={{ ...EMPTY_DRAFT }}
          onClose={() => setAdding(false)}
          onSaved={async (id) => {
            setAdding(false)
            try {
              const ps = await api.profiles()
              setProfiles(ps)
              setProfileId(id)
            } catch {
              // 抬头已建好但刷新失败:提示用户,别让 profileId 空着导致提交按钮一直灰
              setError(t('invoice.apply.refreshFailed'))
            }
          }}
        />
      )}
    </>
  )
}
