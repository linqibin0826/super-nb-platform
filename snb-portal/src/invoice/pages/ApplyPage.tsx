import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, TicketSelect, Textarea } from '../../ui'
import { t } from '../../i18n'
import { api, InvoiceApiError, InvoiceAuthError, type OverviewT, type ProfileT } from '../api'
import { feeCents, fmtYuan, selectedTotalCents } from '../fee'
import { ErrorBar, Loading } from './shared'
import { loginUrl } from '../../auth/apiFetch'

/** 申请开票:默认全选可开票订单,实时算合计/手续费,选抬头提交。 */
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
  const balanceCents = overview ? Math.round(overview.balance * 100) : 0
  const belowMin = totalCents < minCents
  const feeShort = fee > 0 && balanceCents < fee

  if (needLogin) {
    return (
      <Alert tone="info">
        <a className="underline" href={loginUrl()}>{t('invoice.nav.login')}</a>
      </Alert>
    )
  }
  if (error) return <ErrorBar msg={error} />
  if (!overview || !profiles) return <Loading />
  if (doneNo) return <Alert tone="tip">{t('invoice.apply.submitted', { no: doneNo })}</Alert>

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

  return (
    <div className="space-y-4">
      <p className="text-sm text-snb-t2">{t('invoice.apply.intro')}</p>
      {overview.orders.length === 0 ? (
        <Card className="p-8 text-center text-snb-t2">{t('invoice.apply.empty')}</Card>
      ) : (
        <Card className="divide-y divide-snb-line p-0">
          <label className="flex items-center gap-3 px-4 py-2 text-sm text-snb-t2">
            <input
              type="checkbox"
              checked={selected.size === overview.orders.length}
              onChange={(e) =>
                setSelected(e.target.checked ? new Set(overview.orders.map((o) => o.orderId)) : new Set())
              }
            />
            {t('invoice.apply.selectAll')}
          </label>
          {overview.orders.map((o) => (
            <label key={o.orderId} className="flex items-center gap-3 px-4 py-3">
              <input type="checkbox" checked={selected.has(o.orderId)} onChange={() => toggle(o.orderId)} />
              <span className="flex-1 font-mono text-sm">{o.orderNo}</span>
              <span className="text-sm text-snb-t2">{new Date(o.completedAt).toLocaleDateString('zh-CN')}</span>
              <span className="w-24 text-right font-mono">¥{o.amount.toFixed(2)}</span>
            </label>
          ))}
        </Card>
      )}

      <Card className="space-y-3 p-4">
        <div className="flex justify-between text-sm">
          <span>{t('invoice.apply.total')}</span>
          <span className="font-mono">¥{fmtYuan(totalCents)}</span>
        </div>
        <div className="flex justify-between text-sm">
          <span>{t('invoice.apply.fee')} (5%)</span>
          <span className="font-mono">{fee === 0 ? t('invoice.apply.free') : `¥${fmtYuan(fee)}`}</span>
        </div>
        <div className="flex justify-between text-sm text-snb-t2">
          <span>{t('invoice.apply.balance')}</span>
          <span className="font-mono">¥{overview.balance.toFixed(2)}</span>
        </div>
        {belowMin && totalCents > 0 && (
          <Alert tone="warning">{t('invoice.apply.belowMin', { diff: fmtYuan(minCents - totalCents) })}</Alert>
        )}
        {feeShort && <Alert tone="warning">{t('invoice.apply.feeShort', { fee: fmtYuan(fee) })}</Alert>}

        {profiles.length === 0 ? (
          <Alert tone="info">{t('invoice.apply.noProfile')}</Alert>
        ) : (
          <TicketSelect
            value={profileId}
            onChange={(e) => setProfileId(e.target.value)}
            options={profiles.map((p) => ({ value: p.id, label: p.title }))}
            aria-label={t('invoice.apply.profile')}
          />
        )}
        <Textarea
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          maxLength={200}
          placeholder={t('invoice.apply.remark')}
        />
        <Button
          variant="primary"
          disabled={submitting || belowMin || feeShort || selected.size === 0 || !profileId}
          onClick={submit}
        >
          {t('invoice.apply.submit')}
        </Button>
      </Card>
    </div>
  )
}
