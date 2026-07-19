import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Alert, Badge, Button, Card } from '../../ui'
import { t } from '../../i18n'
import { api, RaffleApiError, type CampaignStatus, type CampaignSummaryT } from '../api'
import { ErrorBar, Loading, PageHead } from './shared'

const STATUS_TONE: Record<CampaignStatus, 'primary' | 'success' | 'gray'> = {
  active: 'primary',
  drawn: 'success',
  cancelled: 'gray',
}

function fmt(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}

export function CampaignListPage() {
  const navigate = useNavigate()
  const [rows, setRows] = useState<CampaignSummaryT[] | null>(null)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)

  const load = () => {
    setError('')
    api
      .list()
      .then(setRows)
      .catch((e) => {
        if (e instanceof RaffleApiError && e.status === 403) setForbidden(true)
        else setError(String(e.message))
      })
  }
  useEffect(load, [])

  const head = (
    <div className="mb-7 flex items-center justify-between">
      <PageHead eyebrow={t('raffle.admin.eyebrow')} title={t('raffle.admin.title')} sub={t('raffle.admin.sub')} />
      <Link to="/admin/campaigns/new">
        <Button variant="primary">{t('raffle.admin.newCampaign')}</Button>
      </Link>
    </div>
  )

  if (forbidden)
    return (
      <>
        {head}
        <Alert tone="warning">{t('raffle.admin.forbidden')}</Alert>
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
      {!rows ? (
        <Loading />
      ) : rows.length === 0 ? (
        <div className="rounded-xl border-[1.5px] border-dashed border-snb-hairline-strong p-16 text-center text-sm text-snb-t3">
          {t('raffle.admin.empty')}
        </div>
      ) : (
        <Card className="overflow-hidden p-0">
          {rows.map((c) => (
            <Link
              key={c.id}
              to={`/admin/campaigns/${c.id}`}
              className="flex items-center gap-4 border-b border-snb-hairline px-5 py-3.5 text-[13.5px] transition-colors last:border-b-0 hover:bg-snb-t1/5"
            >
              <Badge tone={STATUS_TONE[c.status]}>{t(`raffle.admin.statuses.${c.status}`)}</Badge>
              <span className="min-w-0 flex-1 truncate font-semibold">{c.name}</span>
              <span className="w-[110px] flex-none text-right font-mono tabular-nums text-snb-t3">
                {t('raffle.admin.gate', { type: t(`raffle.admin.gateTypes.${c.gateType}`), amount: c.gateAmount })}
              </span>
              <span className="w-[160px] flex-none text-right text-snb-t3 max-md:hidden">{fmt(c.drawAt)}</span>
              <span className="w-[80px] flex-none text-right tabular-nums text-snb-t3">
                {t('raffle.admin.prizeCount', { n: c.prizeCount })}
              </span>
              <Button
                size="xs"
                variant="ghost"
                onClick={(e) => {
                  e.preventDefault() // 整行是 <Link>,按钮点击不能触发外层导航
                  navigate(`/admin/campaigns/new?cloneFrom=${c.id}`)
                }}
              >
                {t('raffle.admin.cloneFrom')}
              </Button>
            </Link>
          ))}
        </Card>
      )}
    </>
  )
}
