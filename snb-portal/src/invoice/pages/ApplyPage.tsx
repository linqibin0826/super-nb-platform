import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, Card } from '../../ui'
import { t } from '../../i18n'
import { api, InvoiceApiError, InvoiceAuthError, type OverviewT, type ProfileT } from '../api'
import { feeCents, fmtYuan, fmtYuanGrouped, rmbUpper, selectedTotalCents } from '../fee'
import { ErrorBar, Loading, PageHead } from './shared'
import { loginUrl } from '../../auth/apiFetch'

const today = () => new Date().toLocaleDateString('sv')

/** 申请开票(填开联):左=订单账页,右=实时成票的发票申请单。
 *  勾选订单→票面合计/大写实时更新;跨过免收线盖「免」章(挂载即动画,一次性)。 */
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
  const grandCents = overview.orders.reduce((s, o) => s + Math.round(o.amount * 100), 0)

  const warn = feeShort
    ? t('invoice.apply.feeShort', { fee: fmtYuan(fee) })
    : totalCents > 0 && belowMin
      ? t('invoice.apply.belowMin', { diff: fmtYuan(minCents - totalCents) })
      : ''

  return (
    <>
      {head}
      {error && <div className="mb-4"><ErrorBar msg={error} /></div>}
      <div className="grid items-start gap-7 lg:grid-cols-[minmax(0,1fr)_432px]">
        {/* 左:订单账页 */}
        <div>
          {overview.orders.length === 0 ? (
            <div className="rounded-xl border-[1.5px] border-dashed border-snb-hairline-strong p-16 text-center text-sm text-snb-t3">
              {t('invoice.apply.empty')}
            </div>
          ) : (
            <Card className="overflow-hidden p-0">
              <div className="flex items-center gap-3 border-b border-snb-hairline-strong px-4 py-3.5 iv-ledger-head">
                <label className="flex cursor-pointer items-center gap-2.5 text-sm font-semibold">
                  <input
                    type="checkbox"
                    checked={allOn}
                    onChange={(e) =>
                      setSelected(e.target.checked ? new Set(overview.orders.map((o) => o.orderId)) : new Set())
                    }
                  />
                  {t('invoice.apply.selectAll')}
                </label>
                <span className="ml-auto text-[13px] text-snb-t3">
                  {t('invoice.apply.headMeta', {
                    n: String(overview.orders.length),
                    total: '¥' + fmtYuanGrouped(grandCents),
                  })}
                </span>
              </div>
              <div>
                {overview.orders.map((o) => (
                  <label key={o.orderId} className={`iv-ord ${selected.has(o.orderId) ? 'on' : ''}`}>
                    <input type="checkbox" checked={selected.has(o.orderId)} onChange={() => toggle(o.orderId)} />
                    <span className="w-[88px] flex-none text-[13px] tabular-nums text-snb-t3">
                      {new Date(o.completedAt).toLocaleDateString('sv')}
                    </span>
                    <span className="min-w-0 flex-1 truncate font-mono text-[13px] text-snb-t2">{o.orderNo}</span>
                    <span className="flex-none text-right font-mono text-[15px] font-semibold tabular-nums">
                      ¥{fmtYuanGrouped(Math.round(o.amount * 100))}
                    </span>
                  </label>
                ))}
              </div>
              <div className="flex flex-wrap gap-1.5 border-t border-snb-hairline px-4 py-3 text-[13px] text-snb-t3">
                <span>
                  {t('invoice.apply.balance')} <b className="font-semibold text-snb-t2">¥{overview.balance.toFixed(2)}</b>
                </span>
                <span>·</span>
                <span>{t('invoice.apply.balanceNote')}</span>
              </div>
            </Card>
          )}
        </div>

        {/* 右:发票申请单(实时成票) */}
        <div className="iv-fapiao lg:sticky lg:top-24">
          <div className="iv-fp-title">{t('invoice.apply.docTitle')}</div>
          <div className="iv-fp-title-rule" />
          <div className="iv-fp-no">
            <span>
              {t('invoice.apply.noLabel')} <b>{t('invoice.apply.noPending')}</b>
            </span>
            <span>{today()}</span>
          </div>

          <div className="iv-fp-grid">
            <div className="iv-fp-row">
              <div className="iv-fp-side">{t('invoice.apply.buyerSide')}</div>
              <div className="iv-fp-cell">
                <div className="iv-fp-field">
                  <span className="lb">{t('invoice.apply.nameLabel')}</span>
                  {profiles.length === 0 ? (
                    <Link to="/profiles" className="text-[13px] underline underline-offset-4 hover:text-snb-t1">
                      {t('invoice.apply.noProfile')}
                    </Link>
                  ) : (
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
                  )}
                </div>
                <div className="iv-fp-field">
                  <span className="lb">{t('invoice.apply.taxLabel')}</span>
                  <span className={`vl font-mono ${profile?.taxNo ? '' : 'text-snb-t3'}`}>
                    {profile?.taxNo || t('invoice.profiles.noTax')}
                  </span>
                </div>
              </div>
            </div>

            <div className="iv-fp-row">
              <div className="iv-fp-side">{t('invoice.apply.itemSide')}</div>
              <div className="iv-fp-cell">
                <table className="iv-fp-items">
                  <thead>
                    <tr>
                      <th>{t('invoice.apply.thName')}</th>
                      <th className="!text-right">{t('invoice.apply.thCount')}</th>
                      <th className="!text-right">{t('invoice.apply.thAmt')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td>{t('invoice.apply.itemName')}</td>
                      <td className="r">{t('invoice.apply.countUnit', { n: String(selected.size) })}</td>
                      <td className="r">{totalCents ? '¥' + fmtYuanGrouped(totalCents) : '—'}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

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
                  {isFree ? t('invoice.apply.feeFreeNote') : t('invoice.apply.feeNote')}
                </div>
              </div>
            </div>

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
      </div>
    </>
  )
}
