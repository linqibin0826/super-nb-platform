import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Alert, Card, CardBody, StatusLamp, cx } from '../../ui'
import { api, OpsApiError, type DashboardResult, type SubscriptionRow } from '../api'
import { Dday, ErrorBar, Loading, MONO, PageHead, REFUND_LABELS, SectionLabel, daysUntil, serviceShort } from './shared'

const RAIL_DAYS = 30

/** 金额字符串求和(priceUsd/refundAmountUsd 是后端原样字符串);解析不了的跳过 */
function sumUsd(rows: SubscriptionRow[], pick: (r: SubscriptionRow) => string | null): number {
  return rows.reduce((acc, r) => {
    const n = Number(pick(r))
    return Number.isFinite(n) ? acc + n : acc
  }, 0)
}

const fmtUsd = (n: number) => (Number.isInteger(n) ? String(n) : n.toFixed(2))

/** 看板数字牌:label 弱字、数值等宽大字;money=橙(钱的授权范围) */
function StatTile({ label, value, unit, note, money }: {
  label: string
  value: string
  unit?: string
  note?: string
  money?: boolean
}) {
  return (
    <div className="rounded-2xl border border-snb-hairline bg-snb-panel p-5 shadow-card">
      <p className="text-xs tracking-[0.14em] text-snb-t3">{label}</p>
      <p className={cx('mt-2 text-[28px] leading-none', MONO, money ? 'text-snb-safety' : 'text-snb-t1')}>
        {value}
        {unit && <span className="ml-1 text-sm text-snb-t3">{unit}</span>}
      </p>
      {note && <p className="mt-2 text-xs text-snb-t3">{note}</p>}
    </div>
  )
}

interface RailTick {
  date: string // YYYY-MM-DD
  days: number
  totalUsd: number
  count: number
}

/** 缴费轨:未来 30 天的真实时间轴,每笔续费按天距落点;≤7 天亮橙。
 *  这是看板的签名件——刻度间距 = 真实天数,一眼读出「钱什么时候出去」。 */
function BillingRail({ rows }: { rows: SubscriptionRow[] }) {
  const ticks = useMemo<RailTick[]>(() => {
    const byDate = new Map<string, SubscriptionRow[]>()
    for (const r of rows) {
      if (!r.nextBillingAt) continue
      const list = byDate.get(r.nextBillingAt) ?? []
      list.push(r)
      byDate.set(r.nextBillingAt, list)
    }
    return [...byDate.entries()]
      .map(([date, list]) => ({ date, days: daysUntil(date), totalUsd: sumUsd(list, (r) => r.priceUsd), count: list.length }))
      .filter((t) => t.days >= 0 && t.days <= RAIL_DAYS)
      .sort((a, b) => a.days - b.days)
  }, [rows])

  return (
    <Card>
      <CardBody>
        <SectionLabel>缴费轨 · 今天 → +{RAIL_DAYS} 天</SectionLabel>
        {ticks.length === 0 ? (
          <p className="mt-4 text-sm text-snb-t3">{RAIL_DAYS} 天内没有要扣款的订阅,轨上是空的。</p>
        ) : (
          <div className="mx-4 mt-2 sm:mx-8">
            <div className="relative h-[96px]">
              {/* 轨道基线 + 两端立柱:基线钉在 42px=刻度列(标签16+间距4+立杆18+圆点8/2)的圆点圆心 */}
              <div className="absolute inset-x-0 top-[42px] border-t border-snb-hairline-strong" aria-hidden="true" />
              <div className="absolute left-0 top-[36px] h-3 w-px bg-snb-t2" aria-hidden="true" />
              <div className="absolute right-0 top-[36px] h-3 w-px bg-snb-t2" aria-hidden="true" />
              {ticks.map((t) => {
                const urgent = t.days <= 7
                return (
                  <div
                    key={t.date}
                    className="absolute top-0 h-full"
                    style={{ left: `${(t.days / RAIL_DAYS) * 100}%` }}
                    title={`${t.date} · ${t.count} 笔 · $${fmtUsd(t.totalUsd)}`}
                  >
                    <div className="flex -translate-x-1/2 flex-col items-center whitespace-nowrap">
                      <span
                        className={cx(
                          'h-4 text-xs leading-4',
                          MONO,
                          urgent ? 'font-semibold text-snb-safety' : 'text-snb-t3'
                        )}
                      >
                        {t.days === 0 ? '今天' : `D-${t.days}`}
                      </span>
                      <span
                        aria-hidden="true"
                        className={cx('mt-1 h-[18px] w-px', urgent ? 'bg-snb-safety' : 'bg-snb-lamp-off')}
                      />
                      <span
                        aria-hidden="true"
                        className={cx('h-2 w-2 rounded-full', urgent ? 'bg-snb-safety' : 'bg-snb-lamp-off')}
                      />
                      <span className={cx('mt-1.5 text-[11px] text-snb-t2', MONO)}>{t.date.slice(5)}</span>
                      <span className={cx('text-[11px]', MONO, urgent ? 'text-snb-safety' : 'text-snb-t3')}>
                        ${fmtUsd(t.totalUsd)}
                        {t.count > 1 ? `·${t.count}笔` : ''}
                      </span>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
        )}
      </CardBody>
    </Card>
  )
}

/** 扣款清单:账本行——日期 | 倒计时 | 邮箱 | 服务·卡 | 金额右对齐 */
function BillingLedger({ rows }: { rows: SubscriptionRow[] }) {
  if (rows.length === 0) return <p className="text-sm text-snb-t3">30 天内没有要扣款的订阅。</p>
  return (
    <ul className="divide-y divide-snb-hairline">
      {rows.map((r) => (
        <li
          key={r.id}
          className="grid grid-cols-[auto_minmax(40px,auto)_1fr_auto] items-baseline gap-x-3 py-2.5 text-sm"
        >
          <span className={cx('text-snb-t2', MONO)}>{r.nextBillingAt?.slice(5)}</span>
          {r.nextBillingAt ? <Dday date={r.nextBillingAt} /> : <span />}
          <Link
            className={cx('min-w-0 truncate text-snb-t1 underline-offset-4 hover:underline', MONO)}
            to={`/admin/accounts/${r.accountId}`}
          >
            {r.email}
          </Link>
          <span className={cx('text-right text-snb-t1', MONO)}>{r.priceUsd ? `$${r.priceUsd}` : ''}</span>
          <span className="col-span-2 col-start-3 mt-0.5 text-xs text-snb-t3">
            {serviceShort(r.service)}
            {r.tier ? `·${r.tier}` : ''}
            {r.cardPlatform ? ` · ${r.cardPlatform}${r.cardLast4 ? `·${r.cardLast4}` : ''}` : ''}
          </span>
        </li>
      ))}
    </ul>
  )
}

/** 退款跟进:灯 + 跟进日 + 逾期提醒;申诉中=亮灯(正在发生),待申诉=空心待开 */
function RefundLedger({ rows }: { rows: SubscriptionRow[] }) {
  if (rows.length === 0) return <p className="text-sm text-snb-t3">没有到期该催的退款。</p>
  return (
    <ul className="divide-y divide-snb-hairline">
      {rows.map((r) => {
        const overdue = r.refundFollowUpAt ? daysUntil(r.refundFollowUpAt) : null
        return (
          <li key={r.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 py-2.5 text-sm">
            <StatusLamp state={r.refundStatus === 'APPEALING' ? 'live' : 'pending'} />
            <span className={cx('text-snb-t2', MONO)}>{r.refundFollowUpAt}</span>
            {overdue !== null && overdue < 0 && (
              <span className={cx('text-xs font-semibold text-snb-safety', MONO)}>逾期{-overdue}天</span>
            )}
            <Link
              className={cx('truncate text-snb-t1 underline-offset-4 hover:underline', MONO)}
              to={`/admin/accounts/${r.accountId}`}
            >
              {r.email}
            </Link>
            <span className="text-xs text-snb-t3">
              {serviceShort(r.service)}·{REFUND_LABELS[r.refundStatus]}
              {r.refundAmountUsd ? (
                <>
                  {' · '}
                  <span className={MONO}>${r.refundAmountUsd}</span>
                </>
              ) : null}
            </span>
          </li>
        )
      })}
    </ul>
  )
}

export function DashboardPage() {
  const [data, setData] = useState<DashboardResult | null>(null)
  const [subs, setSubs] = useState<SubscriptionRow[] | null>(null)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    Promise.all([api.dashboard(), api.subs.list()])
      .then(([d, s]) => {
        setData(d)
        setSubs(s)
      })
      .catch((e) => {
        if (e instanceof OpsApiError && e.status === 403) setForbidden(true)
        else setError(String(e.message))
      })
  }, [])

  const stats = useMemo(() => {
    if (!subs) return null
    const active = subs.filter((s) => s.status === 'ACTIVE')
    const chasing = subs.filter((s) => s.refundStatus === 'PENDING' || s.refundStatus === 'APPEALING')
    return {
      monthlyUsd: sumUsd(active, (r) => r.priceUsd),
      activeCount: active.length,
      chasingUsd: sumUsd(chasing, (r) => r.refundAmountUsd),
      chasingCount: chasing.length,
    }
  }, [subs])

  const today = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const head = (
    <PageHead
      title="机房后勤簿"
      sub={`${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())} · 账号 / 订阅 / 缴费 / 退款`}
    />
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
  if (!data || !stats)
    return (
      <>
        {head}
        <Loading />
      </>
    )

  return (
    <>
      {head}
      <div className="mb-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatTile
          label="月固定支出"
          value={`$${fmtUsd(stats.monthlyUsd)}`}
          note={`${stats.activeCount} 笔生效订阅`}
          money
        />
        <StatTile label="30 天内扣款" value={String(data.upcomingBilling.length)} unit="笔" />
        <StatTile
          label="退款在途"
          value={`$${fmtUsd(stats.chasingUsd)}`}
          note={stats.chasingCount > 0 ? `${stats.chasingCount} 笔待申诉/申诉中` : '没有在追的钱'}
        />
        <StatTile label="封号未结案" value={String(data.bannedOpenCount)} unit="件" />
      </div>
      <div className="mb-5">
        <BillingRail rows={data.upcomingBilling} />
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <Card>
          <CardBody>
            <SectionLabel className="mb-2">扣款清单</SectionLabel>
            <BillingLedger rows={data.upcomingBilling} />
          </CardBody>
        </Card>
        <Card>
          <CardBody>
            <SectionLabel className="mb-2">退款跟进</SectionLabel>
            <RefundLedger rows={data.refundFollowUps} />
          </CardBody>
        </Card>
      </div>
    </>
  )
}
