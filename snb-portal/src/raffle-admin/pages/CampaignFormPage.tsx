import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Alert, Button, Card, Input, TicketSelect } from '../../ui'
import { t } from '../../i18n'
import {
  api,
  RaffleApiError,
  type CampaignDetailT,
  type CampaignScalarsT,
  type GateType,
  type PrizeSkeletonT,
  type WeightMode,
} from '../api'
import { ErrorBar, Loading, PageHead } from './shared'

function toLocalInput(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function toIso(local: string): string {
  return local ? new Date(local).toISOString() : ''
}

const BLANK: CampaignScalarsT = {
  name: '',
  entryOpenAt: new Date().toISOString(),
  entryCloseAt: '',
  drawAt: '',
  gateType: 'RECHARGE',
  gateAmount: 30,
  gateFrom: '2026-06-01T00:00:00.000Z',
  minAccountAgeDays: null,
  weightMode: 'EQUAL',
}

export function CampaignFormPage({ mode }: { mode: 'create' | 'edit' }) {
  const { id } = useParams()
  const [searchParams] = useSearchParams()
  const cloneFrom = searchParams.get('cloneFrom')
  const navigate = useNavigate()

  const [scalars, setScalars] = useState<CampaignScalarsT>(BLANK)
  const [prizeSkeleton, setPrizeSkeleton] = useState<PrizeSkeletonT[]>([])
  const [detail, setDetail] = useState<CampaignDetailT | null>(null)
  const [loading, setLoading] = useState(mode === 'edit')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    if (mode === 'edit' && id) {
      api
        .detail(id)
        .then((d) => {
          setDetail(d)
          setScalars({
            name: d.name,
            entryOpenAt: d.entryOpenAt,
            entryCloseAt: d.entryCloseAt,
            drawAt: d.drawAt,
            gateType: d.gateType,
            gateAmount: d.gateAmount,
            gateFrom: d.gateFrom,
            minAccountAgeDays: d.minAccountAgeDays,
            weightMode: d.weightMode,
          })
          setLoading(false)
        })
        .catch((e) => {
          if (e instanceof RaffleApiError && e.status === 403) setForbidden(true)
          else setError(String(e.message))
          setLoading(false)
        })
    } else if (mode === 'create' && cloneFrom) {
      // 克隆=纯前端操作:拿源期详情预填草稿,时间清空待填,奖品只留骨架(tier/展示名/kind/
      // 排序),payload 不带过来(旧码/口令已消耗或已过期,必须重新生成)。
      api
        .detail(cloneFrom)
        .then((d) => {
          setScalars({
            name: `${d.name}(克隆)`,
            entryOpenAt: '',
            entryCloseAt: '',
            drawAt: '',
            gateType: d.gateType,
            gateAmount: d.gateAmount,
            gateFrom: d.gateFrom,
            minAccountAgeDays: d.minAccountAgeDays,
            weightMode: d.weightMode,
          })
          setPrizeSkeleton(
            d.prizes.map((p) => ({ tier: p.tier, displayName: p.displayName, kind: p.kind, sortOrder: p.sortOrder })),
          )
        })
        .catch((e) => setError(String((e as Error).message)))
    }
  }, [mode, id, cloneFrom])

  const editable = mode === 'create' || detail?.status === 'active'

  const save = async () => {
    setSaving(true)
    setError('')
    try {
      if (mode === 'create') {
        const created = await api.create({ ...scalars, prizes: prizeSkeleton })
        navigate(`/admin/campaigns/${created.id}`, { replace: true })
      } else if (id) {
        const updated = await api.update(id, scalars)
        setDetail(updated)
      }
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  const cancelCampaign = async () => {
    if (!id || !confirm(t('raffle.admin.confirmCancel'))) return
    setSaving(true)
    try {
      await api.cancel(id)
      setDetail(await api.detail(id))
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setSaving(false)
    }
  }

  if (forbidden) return <Alert tone="warning">{t('raffle.admin.forbidden')}</Alert>
  if (loading) return <Loading />

  return (
    <>
      <PageHead
        eyebrow={t('raffle.admin.eyebrow')}
        title={mode === 'create' ? t('raffle.admin.newCampaign') : scalars.name}
        sub={detail ? t(`raffle.admin.statuses.${detail.status}`) : undefined}
      />
      {error && (
        <div className="mb-4">
          <ErrorBar msg={error} />
        </div>
      )}
      <Card className="space-y-4 p-6">
        <Input
          label={t('raffle.admin.fields.name')}
          value={scalars.name}
          disabled={!editable}
          onChange={(e) => setScalars({ ...scalars, name: e.target.value })}
        />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Input
            type="datetime-local"
            label={t('raffle.admin.fields.entryOpenAt')}
            disabled={!editable}
            value={scalars.entryOpenAt ? toLocalInput(scalars.entryOpenAt) : ''}
            onChange={(e) => setScalars({ ...scalars, entryOpenAt: toIso(e.target.value) })}
          />
          <Input
            type="datetime-local"
            label={t('raffle.admin.fields.entryCloseAt')}
            disabled={!editable}
            value={scalars.entryCloseAt ? toLocalInput(scalars.entryCloseAt) : ''}
            onChange={(e) => setScalars({ ...scalars, entryCloseAt: toIso(e.target.value) })}
          />
          <Input
            type="datetime-local"
            label={t('raffle.admin.fields.drawAt')}
            disabled={!editable}
            value={scalars.drawAt ? toLocalInput(scalars.drawAt) : ''}
            onChange={(e) => setScalars({ ...scalars, drawAt: toIso(e.target.value) })}
          />
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <TicketSelect
            value={scalars.gateType}
            disabled={!editable}
            options={[
              { value: 'RECHARGE', label: t('raffle.admin.gateTypes.RECHARGE') },
              { value: 'SPEND', label: t('raffle.admin.gateTypes.SPEND') },
            ]}
            onChange={(e) => setScalars({ ...scalars, gateType: e.target.value as GateType })}
          />
          <Input
            type="number"
            label={t('raffle.admin.fields.gateAmount')}
            disabled={!editable}
            value={scalars.gateAmount}
            onChange={(e) => setScalars({ ...scalars, gateAmount: Number(e.target.value) })}
          />
          <Input
            type="datetime-local"
            label={t('raffle.admin.fields.gateFrom')}
            disabled={!editable}
            value={scalars.gateFrom ? toLocalInput(scalars.gateFrom) : ''}
            onChange={(e) => setScalars({ ...scalars, gateFrom: toIso(e.target.value) })}
          />
        </div>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Input
            type="number"
            label={t('raffle.admin.fields.minAccountAgeDays')}
            hint={t('raffle.admin.fields.minAccountAgeDaysHint')}
            disabled={!editable}
            value={scalars.minAccountAgeDays ?? ''}
            onChange={(e) =>
              setScalars({
                ...scalars,
                minAccountAgeDays: e.target.value === '' ? null : Number(e.target.value),
              })
            }
          />
          <TicketSelect
            value={scalars.weightMode}
            disabled={!editable}
            options={[
              { value: 'EQUAL', label: t('raffle.admin.weightModes.EQUAL') },
              { value: 'WEIGHTED', label: t('raffle.admin.weightModes.WEIGHTED') },
            ]}
            onChange={(e) => setScalars({ ...scalars, weightMode: e.target.value as WeightMode })}
          />
        </div>
        <div className="flex items-center gap-3 border-t border-snb-hairline pt-4">
          {editable && (
            <Button variant="primary" disabled={saving} onClick={save}>
              {mode === 'create' ? t('raffle.admin.create') : t('raffle.admin.saveChanges')}
            </Button>
          )}
          {mode === 'edit' && detail?.status !== 'cancelled' && (
            <Button variant="secondary" disabled={saving} onClick={cancelCampaign}>
              {t('raffle.admin.cancelCampaign')}
            </Button>
          )}
        </div>
      </Card>

      {mode === 'create' && prizeSkeleton.length > 0 && (
        <Card className="mt-6 space-y-2 p-6">
          <h2 className="font-display text-lg font-semibold">{t('raffle.admin.clonedPrizesTitle')}</h2>
          <p className="text-sm text-snb-t3">{t('raffle.admin.clonedPrizesHint')}</p>
          <ul className="space-y-1 text-[13px]">
            {prizeSkeleton.map((p, i) => (
              <li key={i}>
                {p.tier} · {p.displayName} · {t(`raffle.admin.kinds.${p.kind}`)}
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  )
}
