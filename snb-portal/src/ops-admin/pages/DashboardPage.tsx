import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Button, Card, CardBody, CardHeader, StatCard } from '../../ui'
import { api, OpsApiError, type DashboardResult, type SubscriptionRow } from '../api'
import { ErrorBar, Loading, PageHead, REFUND_LABELS } from './shared'

/** 外链面板(有确切 URL 的才放,别造假链接;卡台三家 URL 站长给了再加) */
const PANELS: Array<{ label: string; href: string }> = [
  { label: 'Proxy-Cheap 面板', href: 'https://app.proxy-cheap.com/' },
]

function BillingList({ rows }: { rows: SubscriptionRow[] }) {
  if (rows.length === 0) return <p className="text-sm text-snb-t3">30 天内没有要扣款的订阅。</p>
  return (
    <ul className="space-y-2">
      {rows.map((r) => (
        <li key={r.id} className="flex flex-wrap items-center gap-2 text-sm">
          <span className="font-mono text-snb-t1">{r.nextBillingAt}</span>
          <Link className="text-snb-terra hover:underline" to={`/admin/accounts/${r.accountId}`}>
            {r.email}
          </Link>
          <span className="text-snb-t2">
            {r.service}
            {r.tier ? `·${r.tier}` : ''}
            {r.priceUsd ? `·$${r.priceUsd}` : ''}
          </span>
          {r.cardPlatform && (
            <span className="text-snb-t3">
              {r.cardPlatform}
              {r.cardLast4 ? `·${r.cardLast4}` : ''}
            </span>
          )}
        </li>
      ))}
    </ul>
  )
}

function RefundList({ rows }: { rows: SubscriptionRow[] }) {
  if (rows.length === 0) return <p className="text-sm text-snb-t3">没有到期该催的退款。</p>
  return (
    <ul className="space-y-2">
      {rows.map((r) => (
        <li key={r.id} className="flex flex-wrap items-center gap-2 text-sm">
          <span className="font-mono text-snb-t1">{r.refundFollowUpAt}</span>
          <Link className="text-snb-terra hover:underline" to={`/admin/accounts/${r.accountId}`}>
            {r.email}
          </Link>
          <span className="text-snb-t2">
            {r.service}·{REFUND_LABELS[r.refundStatus]}
            {r.refundAmountUsd ? `·$${r.refundAmountUsd}` : ''}
          </span>
          {r.refundNotes && <span className="text-snb-t3">{r.refundNotes}</span>}
        </li>
      ))}
    </ul>
  )
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardResult | null>(null)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    api
      .dashboard()
      .then(setData)
      .catch((e) => {
        if (e instanceof OpsApiError && e.status === 403) setForbidden(true)
        else setError(String(e.message))
      })
  }, [])

  const head = (
    <PageHead title="资产运维台">
      <Link to="/admin/accounts">
        <Button variant="secondary">账号台账</Button>
      </Link>
      {PANELS.map((p) => (
        <a key={p.href} href={p.href} target="_blank" rel="noreferrer">
          <Button variant="ghost">{p.label} ↗</Button>
        </a>
      ))}
    </PageHead>
  )

  if (forbidden)
    return (
      <>
        {head}
        <Alert tone="warning">需要管理员身份。请先在主站以管理员账号登录。</Alert>
      </>
    )
  if (error)
    return (
      <>
        {head}
        <ErrorBar msg={error} />
      </>
    )
  if (!data)
    return (
      <>
        {head}
        <Loading />
      </>
    )

  return (
    <>
      {head}
      <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <StatCard label="30 天内扣款" value={String(data.upcomingBilling.length)} />
        <StatCard label="退款该催了" value={String(data.refundFollowUps.length)} />
        <StatCard label="封号未结案" value={String(data.bannedOpenCount)} />
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader>未来 30 天扣款(按日)</CardHeader>
          <CardBody>
            <BillingList rows={data.upcomingBilling} />
          </CardBody>
        </Card>
        <Card>
          <CardHeader>退款跟进</CardHeader>
          <CardBody>
            <RefundList rows={data.refundFollowUps} />
          </CardBody>
        </Card>
      </div>
    </>
  )
}
