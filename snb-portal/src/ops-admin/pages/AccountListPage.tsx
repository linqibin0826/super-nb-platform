import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Badge, Button, Input, Table } from '../../ui'
import { api, OpsApiError, type AccountRow, type SubscriptionRow } from '../api'
import { AccountStatusBadge, ErrorBar, FieldSelect, Loading, PageHead, SUB_LABELS } from './shared'

/** 该邮箱的订阅摘要徽章串,如 CHATGPT·PRO·生效中 */
function SubSummary({ subs }: { subs: SubscriptionRow[] }) {
  if (subs.length === 0) return <span className="text-snb-t3">—</span>
  return (
    <span className="flex flex-wrap gap-1">
      {subs.map((s) => (
        <Badge key={s.id} tone={s.status === 'BANNED' ? 'danger' : s.status === 'ACTIVE' ? 'success' : 'gray'}>
          {s.service}
          {s.tier ? `·${s.tier}` : ''}·{SUB_LABELS[s.status]}
        </Badge>
      ))}
    </span>
  )
}

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
    <PageHead title="账号台账">
      <Link to="/admin">
        <Button variant="ghost">← 看板</Button>
      </Link>
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
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <div className="max-w-sm flex-1">
          <Input placeholder="按邮箱/负责人/备注过滤…" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        </div>
        <FieldSelect
          className="w-40"
          value={owner}
          onChange={(e) => setOwner(e.target.value)}
          aria-label="按负责人筛选"
        >
          <option value="">全部负责人</option>
          <option value="林琪斌">林琪斌</option>
          <option value="张爱博">张爱博</option>
          <option value="__none__">未分配</option>
        </FieldSelect>
      </div>
      <Table
        columns={[
          { key: 'email', title: '邮箱' },
          { key: 'provider', title: '服务商' },
          { key: 'status', title: '邮箱状态' },
          { key: 'owner', title: '负责人' },
          { key: 'subs', title: '订阅' },
          { key: 'ops', title: '' },
        ]}
        rows={filtered.map((a) => ({
          email: (
            <button
              type="button"
              className="text-snb-terra hover:underline"
              onClick={() => navigate(`/admin/accounts/${a.id}`)}
            >
              {a.email}
            </button>
          ),
          provider: a.provider ?? '—',
          status: <AccountStatusBadge status={a.status} />,
          owner: a.owner ?? '—',
          subs: <SubSummary subs={subsByAccount.get(a.id) ?? []} />,
          ops: (
            <Link className="text-sm text-snb-t3 hover:text-snb-t1" to={`/admin/accounts/${a.id}`}>
              编辑
            </Link>
          ),
        }))}
        rowKey={(_row, i) => filtered[i].id}
      />
      <p className="mt-3 text-sm text-snb-t3">
        共 {filtered.length} / {accounts.length} 个账号
      </p>
    </>
  )
}
