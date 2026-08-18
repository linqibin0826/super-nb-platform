import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Button, Chip, Input, cx } from '../../ui'
import { api, OpsApiError, type AccountRow, type SubscriptionRow } from '../api'
import { AccountStatusBadge, Dday, ErrorBar, Loading, MONO, PageHead, SUB_LABELS, serviceShort } from './shared'

const OWNER_FILTERS = [
  { value: '', label: '全部' },
  { value: '林琪斌', label: '林琪斌' },
  { value: '张爱博', label: '张爱博' },
  { value: '__none__', label: '未分配' },
]

/** 订阅摘要:服务·档位·状态,生效中的补扣款倒计时 */
function SubSummary({ subs }: { subs: SubscriptionRow[] }) {
  if (subs.length === 0) return <span className="text-snb-t3">—</span>
  return (
    <span className="flex flex-wrap items-center gap-x-3 gap-y-1">
      {subs.map((s) => (
        <span key={s.id} className="inline-flex items-center gap-1.5 whitespace-nowrap text-xs">
          <span
            className={cx(
              'font-medium',
              s.status === 'BANNED' ? 'text-snb-danger' : s.status === 'ACTIVE' ? 'text-snb-t1' : 'text-snb-t3'
            )}
          >
            {serviceShort(s.service)}
            {s.tier ? `·${s.tier}` : ''}
          </span>
          <span className={cx(s.status === 'BANNED' ? 'text-snb-danger' : 'text-snb-t3')}>{SUB_LABELS[s.status]}</span>
          {s.status === 'ACTIVE' && s.nextBillingAt && <Dday date={s.nextBillingAt} />}
        </span>
      ))}
    </span>
  )
}

const TH = ({ children, className }: { children?: string; className?: string }) => (
  <th className={cx('px-4 py-3 text-left text-xs font-medium tracking-[0.14em] text-snb-t3', className)}>{children}</th>
)

export function AccountListPage() {
  const navigate = useNavigate()
  const [accounts, setAccounts] = useState<AccountRow[] | null>(null)
  const [subs, setSubs] = useState<SubscriptionRow[]>([])
  const [keyword, setKeyword] = useState('')
  const [owner, setOwner] = useState('')
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    Promise.all([api.accounts.list(), api.subs.list()])
      .then(([a, s]) => {
        setAccounts(a)
        setSubs(s)
      })
      .catch((e) => {
        if (e instanceof OpsApiError && e.status === 403) setForbidden(true)
        else setError(String(e.message))
      })
  }, [])

  const filtered = useMemo(() => {
    if (!accounts) return []
    const kw = keyword.trim().toLowerCase()
    return accounts.filter((a) => {
      if (owner === '__none__' ? a.owner : owner && a.owner !== owner) return false
      if (!kw) return true
      return [a.email, a.owner, a.notes, a.source, a.country]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(kw))
    })
  }, [accounts, keyword, owner])

  const head = (
    <PageHead title="账号台账" sub={accounts ? `在册 ${accounts.length} 个邮箱账号` : undefined}>
      <Link to="/admin/accounts/new">
        <Button>新建账号</Button>
      </Link>
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
  if (!accounts)
    return (
      <>
        {head}
        <Loading />
      </>
    )

  const subsByAccount = new Map<string, SubscriptionRow[]>()
  for (const s of subs) {
    const list = subsByAccount.get(s.accountId) ?? []
    list.push(s)
    subsByAccount.set(s.accountId, list)
  }

  return (
    <>
      {head}
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <div className="max-w-sm flex-1">
          <Input placeholder="按邮箱/负责人/备注过滤…" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        </div>
        <div className="flex items-center" role="group" aria-label="按负责人筛选">
          {OWNER_FILTERS.map((f) => (
            <Chip key={f.value} active={owner === f.value} onClick={() => setOwner(f.value)}>
              {f.label}
            </Chip>
          ))}
        </div>
      </div>
      <div className="overflow-x-auto rounded-2xl border border-snb-hairline bg-snb-panel shadow-card">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-snb-hairline-strong">
              <TH>邮箱</TH>
              <TH className="hidden md:table-cell">地区</TH>
              <TH>负责人</TH>
              <TH>订阅</TH>
              <TH>状态</TH>
              <TH className="w-8" />
            </tr>
          </thead>
          <tbody className="divide-y divide-snb-hairline">
            {filtered.map((a) => (
              <tr
                key={a.id}
                onClick={() => navigate(`/admin/accounts/${a.id}`)}
                className="cursor-pointer transition-colors duration-quick ease-snb hover:bg-black/[0.02] dark:hover:bg-white/[0.04]"
              >
                <td className="px-4 py-3">
                  <Link
                    to={`/admin/accounts/${a.id}`}
                    onClick={(e) => e.stopPropagation()}
                    className={cx(
                      'text-snb-t1 underline-offset-4 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
                      MONO
                    )}
                  >
                    {a.email}
                  </Link>
                </td>
                <td className="hidden px-4 py-3 text-snb-t2 md:table-cell">{a.country ?? '—'}</td>
                <td className="px-4 py-3 text-snb-t2">{a.owner ?? <span className="text-snb-t3">未分配</span>}</td>
                <td className="px-4 py-3">
                  <SubSummary subs={subsByAccount.get(a.id) ?? []} />
                </td>
                <td className="px-4 py-3">
                  <AccountStatusBadge status={a.status} />
                </td>
                <td className="px-2 py-3 text-snb-t3" aria-hidden="true">
                  ›
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filtered.length === 0 && (
          <p className="px-4 py-8 text-center text-sm text-snb-t3">没有匹配的账号,换个关键词或筛选试试。</p>
        )}
      </div>
      <p className={cx('mt-3 text-xs text-snb-t3', MONO)}>
        {filtered.length} / {accounts.length}
      </p>
    </>
  )
}
