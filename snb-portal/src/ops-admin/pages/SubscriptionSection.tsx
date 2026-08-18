import { useCallback, useEffect, useState } from 'react'
import { Button, Card, CardBody, CardHeader, Input, Textarea, cx } from '../../ui'
import {
  api,
  type RefundStatus,
  type SubService,
  type SubStatus,
  type SubTier,
  type SubscriptionInput,
  type SubscriptionRow,
} from '../api'
import {
  Dday,
  ErrorBar,
  FieldSelect,
  MONO,
  REFUND_LABELS,
  REGIONS,
  SUB_LABELS,
  SectionLabel,
  SubStatusBadge,
  serviceShort,
} from './shared'

const SERVICES: SubService[] = ['CHATGPT', 'CLAUDE']
const TIERS: SubTier[] = ['FREE', 'PLUS', 'PRO', 'MAX', 'TEAM']
const SUB_STATUSES: SubStatus[] = ['FREE', 'ACTIVE', 'EXPIRED', 'CANCELED', 'BANNED']
const REFUND_STATUSES: RefundStatus[] = ['NONE', 'PENDING', 'APPEALING', 'REFUNDED', 'REJECTED']
const CARD_PLATFORMS = ['mekelove', 'cgwcard', 'yikahk', 'other']

/** 表单里的订阅字段(全部字符串态;datetime-local ↔ ISO Instant 的转换在提交/装载时做) */
interface SubForm {
  service: SubService
  tier: string
  region: string
  status: SubStatus
  cardPlatform: string
  cardLast4: string
  registerIp: string
  currentIp: string
  registeredAt: string // datetime-local 值
  startedAt: string
  nextBillingAt: string
  priceUsd: string
  sub2apiAccountId: string
  sub2apiAccountName: string
  bannedAt: string // datetime-local 值
  bannedWhilePaid: '' | 'true' | 'false'
  refundStatus: RefundStatus
  refundAmountUsd: string
  appealedAt: string
  refundResolvedAt: string
  refundFollowUpAt: string
  refundNotes: string
  notes: string
}

const EMPTY_SUB: SubForm = {
  service: 'CHATGPT',
  tier: '',
  region: '',
  status: 'FREE',
  cardPlatform: '',
  cardLast4: '',
  registerIp: '',
  currentIp: '',
  registeredAt: '',
  startedAt: '',
  nextBillingAt: '',
  priceUsd: '',
  sub2apiAccountId: '',
  sub2apiAccountName: '',
  bannedAt: '',
  bannedWhilePaid: '',
  refundStatus: 'NONE',
  refundAmountUsd: '',
  appealedAt: '',
  refundResolvedAt: '',
  refundFollowUpAt: '',
  refundNotes: '',
  notes: '',
}

const nn = (v: string) => (v.trim() === '' ? null : v.trim())
/** datetime-local("2026-08-13T10:30") → ISO Instant;空 → null */
const toIso = (local: string) => (local ? new Date(local).toISOString() : null)
/** ISO Instant → datetime-local 值(本地时区,分钟精度);空 → '' */
function toLocalInput(iso: string | null): string {
  if (!iso) return ''
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function rowToForm(r: SubscriptionRow): SubForm {
  return {
    service: r.service,
    tier: r.tier ?? '',
    region: r.region ?? '',
    status: r.status,
    cardPlatform: r.cardPlatform ?? '',
    cardLast4: r.cardLast4 ?? '',
    registerIp: r.registerIp ?? '',
    currentIp: r.currentIp ?? '',
    registeredAt: toLocalInput(r.registeredAt),
    startedAt: r.startedAt ?? '',
    nextBillingAt: r.nextBillingAt ?? '',
    priceUsd: r.priceUsd ?? '',
    sub2apiAccountId: r.sub2apiAccountId ?? '',
    sub2apiAccountName: r.sub2apiAccountName ?? '',
    bannedAt: toLocalInput(r.bannedAt),
    bannedWhilePaid: r.bannedWhilePaid === null ? '' : r.bannedWhilePaid ? 'true' : 'false',
    refundStatus: r.refundStatus,
    refundAmountUsd: r.refundAmountUsd ?? '',
    appealedAt: r.appealedAt ?? '',
    refundResolvedAt: r.refundResolvedAt ?? '',
    refundFollowUpAt: r.refundFollowUpAt ?? '',
    refundNotes: r.refundNotes ?? '',
    notes: r.notes ?? '',
  }
}

function formToInput(accountId: string, f: SubForm): SubscriptionInput {
  return {
    accountId,
    service: f.service,
    tier: (nn(f.tier) as SubTier | null),
    region: nn(f.region),
    cardPlatform: nn(f.cardPlatform),
    cardLast4: nn(f.cardLast4),
    registerIp: nn(f.registerIp),
    currentIp: nn(f.currentIp),
    registeredAt: toIso(f.registeredAt),
    startedAt: nn(f.startedAt),
    nextBillingAt: nn(f.nextBillingAt),
    priceUsd: nn(f.priceUsd),
    status: f.status,
    sub2apiAccountId: nn(f.sub2apiAccountId),
    sub2apiAccountName: nn(f.sub2apiAccountName),
    bannedAt: toIso(f.bannedAt),
    bannedWhilePaid: f.bannedWhilePaid === '' ? null : f.bannedWhilePaid === 'true',
    refundStatus: f.refundStatus,
    refundAmountUsd: nn(f.refundAmountUsd),
    appealedAt: nn(f.appealedAt),
    refundResolvedAt: nn(f.refundResolvedAt),
    refundFollowUpAt: nn(f.refundFollowUpAt),
    refundNotes: nn(f.refundNotes),
    notes: nn(f.notes),
  }
}

function TextField({
  label,
  value,
  onChange,
  type,
  placeholder,
}: {
  label: string
  value: string
  onChange: (v: string) => void
  type?: string
  placeholder?: string
}) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-snb-t2">{label}</label>
      <Input type={type} value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} />
    </div>
  )
}

/** 单条订阅编辑表单(新建/编辑共用) */
function SubEditor({
  initial,
  onSave,
  onCancel,
  saving,
}: {
  initial: SubForm
  onSave: (f: SubForm) => void
  onCancel: () => void
  saving: boolean
}) {
  const [f, setF] = useState<SubForm>(initial)
  const set = (patch: Partial<SubForm>) => setF((v) => ({ ...v, ...patch }))
  const bannedMissing = f.status === 'BANNED' && f.bannedAt === ''

  return (
    <div className="space-y-5 rounded-xl border border-snb-hairline-strong bg-snb-well/40 p-4">
      <div>
        <SectionLabel className="mb-3">基本</SectionLabel>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <FieldSelect label="服务" value={f.service} onChange={(e) => set({ service: e.target.value as SubService })}>
            {SERVICES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </FieldSelect>
          <FieldSelect label="档位" value={f.tier} onChange={(e) => set({ tier: e.target.value })}>
            <option value="">—</option>
            {TIERS.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </FieldSelect>
          <FieldSelect label="订阅区域" value={f.region} onChange={(e) => set({ region: e.target.value })}>
            <option value="">—</option>
            {REGIONS.map((r) => (
              <option key={r} value={r}>
                {r}
              </option>
            ))}
            {f.region && !REGIONS.includes(f.region) && <option value={f.region}>{f.region}</option>}
          </FieldSelect>
          <FieldSelect label="状态" value={f.status} onChange={(e) => set({ status: e.target.value as SubStatus })}>
            {SUB_STATUSES.map((s) => (
              <option key={s} value={s}>
                {SUB_LABELS[s]}
              </option>
            ))}
          </FieldSelect>
        </div>
      </div>
      <div>
        <SectionLabel className="mb-3">注册与开通</SectionLabel>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <FieldSelect
            label="开通卡台"
            value={f.cardPlatform}
            onChange={(e) => set({ cardPlatform: e.target.value })}
          >
            <option value="">—</option>
            {CARD_PLATFORMS.map((c) => (
              <option key={c} value={c}>
                {c}
              </option>
            ))}
          </FieldSelect>
          <TextField label="卡号后四位" value={f.cardLast4} onChange={(v) => set({ cardLast4: v })} />
          <TextField label="注册IP" value={f.registerIp} onChange={(v) => set({ registerIp: v })} />
          <TextField label="现用IP" value={f.currentIp} onChange={(v) => set({ currentIp: v })} />
          <TextField
            label="注册时间"
            type="datetime-local"
            value={f.registeredAt}
            onChange={(v) => set({ registeredAt: v })}
          />
          <TextField label="开始日期" type="date" value={f.startedAt} onChange={(v) => set({ startedAt: v })} />
          <TextField
            label="下次扣款日"
            type="date"
            value={f.nextBillingAt}
            onChange={(v) => set({ nextBillingAt: v })}
          />
          <TextField label="月价($)" value={f.priceUsd} onChange={(v) => set({ priceUsd: v })} placeholder="200.00" />
          <TextField
            label="号池账号ID"
            value={f.sub2apiAccountId}
            onChange={(v) => set({ sub2apiAccountId: v })}
          />
          <TextField
            label="号池账号名"
            value={f.sub2apiAccountName}
            onChange={(v) => set({ sub2apiAccountName: v })}
          />
        </div>
      </div>
      <div>
        <SectionLabel className="mb-3">封号</SectionLabel>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <TextField
            label="封号时间"
            type="datetime-local"
            value={f.bannedAt}
            onChange={(v) => set({ bannedAt: v })}
          />
          <FieldSelect
            label="封号时是否付费中"
            value={f.bannedWhilePaid}
            onChange={(e) => set({ bannedWhilePaid: e.target.value as SubForm['bannedWhilePaid'] })}
          >
            <option value="">未知</option>
            <option value="true">是</option>
            <option value="false">否</option>
          </FieldSelect>
        </div>
        {bannedMissing && <p className="mt-1 text-sm text-snb-danger">状态为「已封」时封号时间必填。</p>}
      </div>
      <div>
        <SectionLabel className="mb-3">退款追踪</SectionLabel>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <FieldSelect
            label="退款状态"
            value={f.refundStatus}
            onChange={(e) => set({ refundStatus: e.target.value as RefundStatus })}
          >
            {REFUND_STATUSES.map((s) => (
              <option key={s} value={s}>
                {REFUND_LABELS[s]}
              </option>
            ))}
          </FieldSelect>
          <TextField label="退款金额($)" value={f.refundAmountUsd} onChange={(v) => set({ refundAmountUsd: v })} />
          <TextField label="申诉日期" type="date" value={f.appealedAt} onChange={(v) => set({ appealedAt: v })} />
          <TextField
            label="到账/结案日期"
            type="date"
            value={f.refundResolvedAt}
            onChange={(v) => set({ refundResolvedAt: v })}
          />
          <TextField
            label="下次跟进日"
            type="date"
            value={f.refundFollowUpAt}
            onChange={(v) => set({ refundFollowUpAt: v })}
          />
          <div className="col-span-2 sm:col-span-3">
            <label className="mb-1.5 block text-sm font-medium text-snb-t2">退款备注(路由/工单线程)</label>
            <Input value={f.refundNotes} onChange={(e) => set({ refundNotes: e.target.value })} />
          </div>
        </div>
      </div>
      <div>
        <label className="mb-1.5 block text-sm font-medium text-snb-t2">备注</label>
        <Textarea value={f.notes} onChange={(e) => set({ notes: e.target.value })} rows={2} />
      </div>
      <div className="flex gap-2">
        <Button onClick={() => onSave(f)} disabled={saving || bannedMissing}>
          {saving ? '保存中…' : '保存订阅'}
        </Button>
        <Button variant="ghost" onClick={onCancel}>
          取消
        </Button>
      </div>
    </div>
  )
}

/** 账号详情页下方的订阅区:列出该邮箱开通的服务,可加/改/删 */
export function SubscriptionSection({ accountId }: { accountId: string }) {
  const [rows, setRows] = useState<SubscriptionRow[] | null>(null)
  const [error, setError] = useState('')
  const [editingId, setEditingId] = useState<string | 'new' | null>(null)
  const [saving, setSaving] = useState(false)

  const reload = useCallback(() => {
    api.subs
      .list(accountId)
      .then(setRows)
      .catch((e) => setError(String(e.message)))
  }, [accountId])

  useEffect(reload, [reload])

  const save = async (f: SubForm) => {
    setSaving(true)
    setError('')
    try {
      if (editingId === 'new') await api.subs.create(formToInput(accountId, f))
      else if (editingId) await api.subs.update(editingId, formToInput(accountId, f))
      setEditingId(null)
      reload()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  const remove = async (id: string) => {
    if (!window.confirm('确认删除这条订阅记录?')) return
    setError('')
    try {
      await api.subs.remove(id)
      reload()
    } catch (e) {
      setError(String((e as Error).message))
    }
  }

  return (
    <Card className="mt-5">
      <CardHeader>
        <div className="flex items-center justify-between">
          <span>服务开通 / 订阅</span>
          {editingId === null && <Button onClick={() => setEditingId('new')}>加一个服务</Button>}
        </div>
      </CardHeader>
      <CardBody>
        {error && (
          <div className="mb-3">
            <ErrorBar msg={error} />
          </div>
        )}
        {rows === null ? (
          <p className="text-sm text-snb-t3">载入中…</p>
        ) : rows.length === 0 && editingId !== 'new' ? (
          <p className="text-sm text-snb-t3">这个邮箱还没开通任何服务。点「加一个服务」记录 ChatGPT / Claude 的开通。</p>
        ) : (
          <ul className="space-y-3">
            {rows.map((r) => (
              <li key={r.id}>
                {editingId === r.id ? (
                  <SubEditor initial={rowToForm(r)} onSave={save} onCancel={() => setEditingId(null)} saving={saving} />
                ) : (
                  <div className="flex flex-wrap items-center gap-x-4 gap-y-2 rounded-xl border border-snb-hairline px-4 py-3 text-sm">
                    {/* 票据头:服务·档位 + 区域 */}
                    <div className="min-w-[104px]">
                      <p className={cx('font-semibold text-snb-t1', MONO)}>
                        {serviceShort(r.service)}
                        {r.tier ? ` · ${r.tier}` : ''}
                      </p>
                      <p className="text-xs text-snb-t3">{r.region ?? '—'}</p>
                    </div>
                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-snb-t2">
                      {r.nextBillingAt && (
                        <span>
                          扣款 <span className={MONO}>{r.nextBillingAt}</span> <Dday date={r.nextBillingAt} />
                        </span>
                      )}
                      {r.priceUsd && <span className={MONO}>${r.priceUsd}/月</span>}
                      {r.cardPlatform && (
                        <span className={MONO}>
                          {r.cardPlatform}
                          {r.cardLast4 ? `·${r.cardLast4}` : ''}
                        </span>
                      )}
                      {r.sub2apiAccountName && <span className="text-snb-t3">池·{r.sub2apiAccountName}</span>}
                      {r.bannedAt && (
                        <span className="text-snb-danger">
                          封于 <span className={MONO}>{r.bannedAt.slice(0, 10)}</span>
                        </span>
                      )}
                      {r.refundStatus !== 'NONE' && (
                        <span className="text-snb-danger">退款·{REFUND_LABELS[r.refundStatus]}</span>
                      )}
                    </div>
                    <span className="ml-auto flex items-center gap-2">
                      <SubStatusBadge status={r.status} />
                      <Button size="sm" variant="ghost" onClick={() => setEditingId(r.id)}>
                        编辑
                      </Button>
                      <Button size="sm" variant="ghost" onClick={() => remove(r.id)}>
                        删除
                      </Button>
                    </span>
                  </div>
                )}
              </li>
            ))}
            {editingId === 'new' && (
              <li>
                <SubEditor initial={EMPTY_SUB} onSave={save} onCancel={() => setEditingId(null)} saving={saving} />
              </li>
            )}
          </ul>
        )}
      </CardBody>
    </Card>
  )
}
