import { useEffect, useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Card, CardBody, Input, Textarea, cx } from '../../ui'
import { api, type AccountInput, type AccountRow, type AccountStatus } from '../api'
import { AccountStatusBadge, ErrorBar, FieldSelect, Loading, MONO, PageHead, REGIONS, SectionLabel } from './shared'
import { SubscriptionSection } from './SubscriptionSection'

const PROVIDERS = ['gmail', 'mail.com', 'outlook', 'icloud', 'qq', 'other']
/** 负责人只有这两位(站长 2026-08-18 拍板);将来加人在此追加 */
const OWNERS = ['林琪斌', '张爱博']
const STATUSES: Array<{ value: AccountStatus; label: string }> = [
  { value: 'ACTIVE', label: '可用' },
  { value: 'BANNED', label: '已封' },
  { value: 'UNVERIFIED', label: '未验' },
  { value: 'ABANDONED', label: '已弃' },
]

interface FormState {
  email: string
  provider: string
  recoveryEmail: string
  regYear: string
  country: string
  owner: string
  status: AccountStatus
  source: string
  notes: string
}

const EMPTY: FormState = {
  email: '',
  provider: 'gmail',
  recoveryEmail: '',
  regYear: '',
  country: '',
  owner: '',
  status: 'UNVERIFIED',
  source: '',
  notes: '',
}

const nn = (v: string) => (v.trim() === '' ? null : v.trim())

/** 密码字段(编辑模式):已设置/未设置 + 显示(按需解密) + 修改(留空提交=不改,这是后端契约) */
function SecretField({
  label,
  has,
  revealed,
  editing,
  value,
  onToggleReveal,
  onToggleEdit,
  onChange,
}: {
  label: string
  has: boolean
  revealed: string | null
  editing: boolean
  value: string
  onToggleReveal: () => void
  onToggleEdit: () => void
  onChange: (v: string) => void
}) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-snb-t2">{label}</label>
      {editing ? (
        <div className="flex items-center gap-2">
          <Input value={value} onChange={(e) => onChange(e.target.value)} placeholder="留空=不改原密码" />
          <Button variant="ghost" onClick={onToggleEdit}>
            取消
          </Button>
        </div>
      ) : (
        <div className="flex items-center gap-2">
          {revealed !== null ? (
            <Input readOnly value={revealed} className={MONO} />
          ) : (
            <span className={cx('text-sm text-snb-t2', has && MONO)}>{has ? '已设置 ••••••' : '未设置'}</span>
          )}
          {has && (
            <Button variant="ghost" onClick={onToggleReveal}>
              {revealed !== null ? '隐藏' : '显示'}
            </Button>
          )}
          <Button variant="ghost" onClick={onToggleEdit}>
            修改
          </Button>
        </div>
      )}
    </div>
  )
}

function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-snb-t2">{label}</label>
      {children}
      {hint && <p className="mt-1 text-xs text-snb-t3">{hint}</p>}
    </div>
  )
}

/** 读态档案卡的键值行:标签弱字定宽,值走账本声线 */
function DossierRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="grid grid-cols-[6.5rem_1fr] items-baseline gap-3 py-[7px]">
      <dt className="text-xs text-snb-t3">{label}</dt>
      <dd className="min-w-0 text-sm text-snb-t1">{children}</dd>
    </div>
  )
}

const Empty = () => <span className="text-snb-t3">—</span>

/** 档案读态:查信息不该面对一墙输入框——身份一行读完,凭据/归属是无框键值对 */
function DossierView({
  form,
  hasPassword,
  hasRecoveryPassword,
  createdAt,
  secret,
  onToggleReveal,
  onEdit,
}: {
  form: FormState
  hasPassword: boolean
  hasRecoveryPassword: boolean
  createdAt: string | null
  secret: { password: string | null; recoveryPassword: string | null } | null
  onToggleReveal: () => void
  onEdit: () => void
}) {
  const specLine = [
    form.provider || null,
    form.regYear ? `${form.regYear} 年注册` : null,
    form.country || null,
    form.owner || '未分配',
  ]
    .filter(Boolean)
    .join(' · ')

  const secretVal = (has: boolean, plain: string | null) => {
    if (!has) return <span className="text-snb-t3">未设置</span>
    if (plain !== null) return <span className={MONO}>{plain}</span>
    return <span className={cx('text-snb-t2', MONO)}>••••••</span>
  }

  return (
    <Card>
      <CardBody>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex min-w-0 flex-wrap items-center gap-3">
            <span className={cx('truncate text-lg text-snb-t1', MONO)}>{form.email}</span>
            <AccountStatusBadge status={form.status} />
          </div>
          <Button variant="secondary" size="sm" onClick={onEdit}>
            编辑档案
          </Button>
        </div>
        <p className="mt-1.5 text-sm text-snb-t2">{specLine}</p>

        <div className="mt-5 grid gap-x-10 lg:grid-cols-2">
          <dl className="divide-y divide-snb-hairline">
            <DossierRow label="邮箱密码">
              <span className="flex items-center gap-3">
                {secretVal(hasPassword, secret ? secret.password : null)}
                {(hasPassword || hasRecoveryPassword) && (
                  <button
                    type="button"
                    onClick={onToggleReveal}
                    className="text-xs text-snb-t2 underline-offset-4 hover:text-snb-t1 hover:underline focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus"
                  >
                    {secret !== null ? '隐藏' : '显示'}
                  </button>
                )}
              </span>
            </DossierRow>
            <DossierRow label="辅助邮箱">
              {form.recoveryEmail ? <span className={MONO}>{form.recoveryEmail}</span> : <Empty />}
            </DossierRow>
            <DossierRow label="辅助邮箱密码">
              {secretVal(hasRecoveryPassword, secret ? secret.recoveryPassword : null)}
            </DossierRow>
          </dl>
          <dl className="divide-y divide-snb-hairline">
            <DossierRow label="货源">{form.source || <Empty />}</DossierRow>
            <DossierRow label="建档">
              {createdAt ? <span className={MONO}>{createdAt.slice(0, 10)}</span> : <Empty />}
            </DossierRow>
          </dl>
        </div>

        {form.notes && (
          <div className="mt-4 border-l-2 border-snb-hairline-strong pl-3 text-sm leading-relaxed text-snb-t2">
            {form.notes}
          </div>
        )}
      </CardBody>
    </Card>
  )
}

export function AccountFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const { id } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState<FormState>(EMPTY)
  const [account, setAccount] = useState<AccountRow | null>(null)
  const [loading, setLoading] = useState(mode === 'edit')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  // 档案页读优先:编辑是动作不是常态。新建模式恒为表单。
  const [editing, setEditing] = useState(mode === 'create')
  // 进编辑时快照,取消时滚回——否则改一半点取消,读态会显示没保存的值
  const [snapshot, setSnapshot] = useState<FormState | null>(null)
  // 新建模式:两个明文输入;编辑模式:走 SecretField 的显示/修改交互
  const [password, setPassword] = useState('')
  const [recoveryPassword, setRecoveryPassword] = useState('')
  const [pwEditing, setPwEditing] = useState(false)
  const [rpwEditing, setRpwEditing] = useState(false)
  const [secret, setSecret] = useState<{ password: string | null; recoveryPassword: string | null } | null>(null)

  useEffect(() => {
    if (mode !== 'edit' || !id) return
    api.accounts
      .list()
      .then((all) => {
        const found = all.find((a) => a.id === id)
        if (!found) {
          setError(`账号不存在: ${id}`)
        } else {
          setAccount(found)
          setForm({
            email: found.email,
            provider: found.provider ?? '',
            recoveryEmail: found.recoveryEmail ?? '',
            regYear: found.regYear ?? '',
            country: found.country ?? '',
            owner: found.owner ?? '',
            status: found.status,
            source: found.source ?? '',
            notes: found.notes ?? '',
          })
        }
      })
      .catch((e) => setError(String(e.message)))
      .finally(() => setLoading(false))
  }, [mode, id])

  const set = (patch: Partial<FormState>) => setForm((f) => ({ ...f, ...patch }))

  const buildInput = (): AccountInput => ({
    email: form.email.trim(),
    provider: nn(form.provider),
    recoveryEmail: nn(form.recoveryEmail),
    regYear: nn(form.regYear),
    country: nn(form.country),
    owner: nn(form.owner),
    status: form.status,
    source: nn(form.source),
    notes: nn(form.notes),
    // 新建:输入即密码(空=不设);编辑:仅当点了「修改」且非空才覆盖,否则 null=不改
    password: mode === 'create' ? nn(password) : pwEditing ? nn(password) : null,
    recoveryPassword: mode === 'create' ? nn(recoveryPassword) : rpwEditing ? nn(recoveryPassword) : null,
  })

  const submit = async () => {
    setSaving(true)
    setError('')
    setSaved(false)
    try {
      if (mode === 'create') {
        const { id: newId } = await api.accounts.create(buildInput())
        navigate(`/admin/accounts/${newId}`)
      } else if (id) {
        await api.accounts.update(id, buildInput())
        setSaved(true)
        // 读态展示靠 form + 这两个布尔,保存后就地对齐,不再拉一遍列表
        setAccount((a) =>
          a
            ? {
                ...a,
                hasPassword: pwEditing && password.trim() !== '' ? true : a.hasPassword,
                hasRecoveryPassword: rpwEditing && recoveryPassword.trim() !== '' ? true : a.hasRecoveryPassword,
              }
            : a
        )
        setPwEditing(false)
        setRpwEditing(false)
        setSecret(null)
        setEditing(false)
      }
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  const remove = async () => {
    if (!id || !window.confirm('确认删除该账号?名下有订阅时会被拒绝。')) return
    setError('')
    try {
      await api.accounts.remove(id)
      navigate('/admin/accounts')
    } catch (e) {
      setError(String((e as Error).message))
    }
  }

  const toggleReveal = async () => {
    if (secret !== null) {
      setSecret(null)
      return
    }
    if (!id) return
    try {
      setSecret(await api.accounts.secret(id))
    } catch (e) {
      setError(String((e as Error).message))
    }
  }

  const head = (
    <PageHead
      title={mode === 'create' ? '新建账号' : '账号档案'}
      sub={mode === 'edit' ? form.email || undefined : undefined}
    >
      <Link to="/admin/accounts">
        <Button variant="ghost">← 返回台账</Button>
      </Link>
      {mode === 'edit' && (
        <Button variant="ghost" className="text-snb-danger hover:text-snb-danger" onClick={remove}>
          删除账号
        </Button>
      )}
    </PageHead>
  )

  if (loading)
    return (
      <>
        {head}
        <Loading />
      </>
    )

  return (
    <>
      {head}
      {error && (
        <div className="mb-4">
          <ErrorBar msg={error} />
        </div>
      )}
      {saved && !editing && (
        <div className="mb-4">
          <Alert tone="tip">已保存。</Alert>
        </div>
      )}
      {!editing && mode === 'edit' ? (
        <DossierView
          form={form}
          hasPassword={account?.hasPassword ?? false}
          hasRecoveryPassword={account?.hasRecoveryPassword ?? false}
          createdAt={account?.createdAt ?? null}
          secret={secret}
          onToggleReveal={toggleReveal}
          onEdit={() => {
            setSaved(false)
            setSnapshot(form)
            setEditing(true)
          }}
        />
      ) : (
        <Card>
          <CardBody>
            <SectionLabel className="mb-4">身份</SectionLabel>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="邮箱 *">
                <Input
                  className={MONO}
                  value={form.email}
                  onChange={(e) => set({ email: e.target.value })}
                  placeholder="xx@gmail.com"
                />
              </Field>
              <FieldSelect label="邮箱服务商" value={form.provider} onChange={(e) => set({ provider: e.target.value })}>
                <option value="">—</option>
                {PROVIDERS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </FieldSelect>
              <Field label="注册年份">
                <Input
                  className={MONO}
                  value={form.regYear}
                  onChange={(e) => set({ regYear: e.target.value })}
                  placeholder="2024"
                />
              </Field>
              <FieldSelect label="国家/地区" value={form.country} onChange={(e) => set({ country: e.target.value })}>
                <option value="">—</option>
                {REGIONS.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
                {form.country && !REGIONS.includes(form.country) && (
                  <option value={form.country}>{form.country}</option>
                )}
              </FieldSelect>
              <FieldSelect label="负责人" value={form.owner} onChange={(e) => set({ owner: e.target.value })}>
                <option value="">未分配</option>
                {OWNERS.map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
                {/* 库里已有的非标值不至于被下拉悄悄改掉 */}
                {form.owner && !OWNERS.includes(form.owner) && <option value={form.owner}>{form.owner}</option>}
              </FieldSelect>
              <div>
                <FieldSelect
                  label="邮箱状态"
                  value={form.status}
                  onChange={(e) => set({ status: e.target.value as AccountStatus })}
                >
                  {STATUSES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </FieldSelect>
                <p className="mt-1 text-xs text-snb-t3">
                  指邮箱账号本身的死活;ChatGPT/Claude 服务被封记在下方订阅行,不改这里。
                </p>
              </div>
            </div>

            <SectionLabel className="mb-4 mt-7">凭据</SectionLabel>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {mode === 'create' ? (
                <Field label="邮箱密码">
                  <Input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="可留空" />
                </Field>
              ) : (
                <SecretField
                  label="邮箱密码"
                  has={account?.hasPassword ?? false}
                  revealed={secret ? secret.password : null}
                  editing={pwEditing}
                  value={password}
                  onToggleReveal={toggleReveal}
                  onToggleEdit={() => {
                    setPwEditing((v) => !v)
                    setPassword('')
                  }}
                  onChange={setPassword}
                />
              )}
              <Field label="辅助邮箱">
                <Input
                  className={MONO}
                  value={form.recoveryEmail}
                  onChange={(e) => set({ recoveryEmail: e.target.value })}
                />
              </Field>
              {mode === 'create' ? (
                <Field label="辅助邮箱密码">
                  <Input
                    value={recoveryPassword}
                    onChange={(e) => setRecoveryPassword(e.target.value)}
                    placeholder="可留空"
                  />
                </Field>
              ) : (
                <SecretField
                  label="辅助邮箱密码"
                  has={account?.hasRecoveryPassword ?? false}
                  revealed={secret ? secret.recoveryPassword : null}
                  editing={rpwEditing}
                  value={recoveryPassword}
                  onToggleReveal={toggleReveal}
                  onToggleEdit={() => {
                    setRpwEditing((v) => !v)
                    setRecoveryPassword('')
                  }}
                  onChange={setRecoveryPassword}
                />
              )}
            </div>

            <SectionLabel className="mb-4 mt-7">归属</SectionLabel>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="货源">
                <Input value={form.source} onChange={(e) => set({ source: e.target.value })} placeholder="松哥店铺…" />
              </Field>
              <div className="sm:col-span-2">
                <Field label="备注">
                  <Textarea value={form.notes} onChange={(e) => set({ notes: e.target.value })} rows={2} />
                </Field>
              </div>
            </div>

            <div className="mt-6 flex gap-2">
              <Button onClick={submit} disabled={saving || !form.email.trim()}>
                {saving ? '保存中…' : mode === 'create' ? '创建账号' : '保存修改'}
              </Button>
              {mode === 'edit' && (
                <Button
                  variant="ghost"
                  onClick={() => {
                    if (snapshot) setForm(snapshot)
                    setEditing(false)
                    setPwEditing(false)
                    setRpwEditing(false)
                  }}
                >
                  取消
                </Button>
              )}
            </div>
          </CardBody>
        </Card>
      )}
      {mode === 'edit' && id && <SubscriptionSection accountId={id} />}
    </>
  )
}
