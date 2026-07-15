import { useEffect, useState } from 'react'
import { Badge, Button, Card } from '../../ui'
import { t } from '../../i18n'
import { api, downloadPdf, type RequestT } from '../api'
import { ErrorBar, Loading } from './shared'

// BadgeTone 实际取值 'primary'|'success'|'warning'|'danger'|'gray'(无 neutral/info),按语义重映射。
const TONE: Record<RequestT['status'], 'gray' | 'primary' | 'success' | 'danger'> = {
  PENDING: 'gray',
  INVOICING: 'primary',
  ISSUED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'gray',
}

/** 我的申请:状态/费额/驳回原因;PENDING 可撤回,ISSUED 可下载。 */
export function RequestsPage() {
  const [rows, setRows] = useState<RequestT[] | null>(null)
  const [error, setError] = useState('')

  const load = () => api.requests().then(setRows).catch((e) => setError(String(e.message)))
  useEffect(() => {
    load()
  }, [])

  const cancel = async (id: string) => {
    if (!window.confirm(t('invoice.requests.confirmCancel'))) return
    try {
      await api.cancelRequest(id)
      load()
    } catch (e) {
      setError(String((e as Error).message))
    }
  }

  if (error) return <ErrorBar msg={error} />
  if (!rows) return <Loading />
  if (rows.length === 0) return <Card className="p-8 text-center text-snb-t2">{t('invoice.requests.empty')}</Card>

  return (
    <div className="space-y-3">
      {rows.map((r) => (
        <Card key={r.id} className="space-y-2 p-4">
          <div className="flex items-center justify-between">
            <span className="font-mono text-sm">{r.requestNo}</span>
            <Badge tone={TONE[r.status]}>{t(`invoice.requests.statuses.${r.status}`)}</Badge>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-snb-t2">{r.profileTitle}</span>
            <span className="font-mono">
              ¥{r.amount.toFixed(2)}
              <span className="ml-2 text-snb-t2">
                {t('invoice.requests.fee')} {r.fee === 0 ? t('invoice.apply.free') : `¥${r.fee.toFixed(2)}`}
              </span>
            </span>
          </div>
          <div className="text-xs text-snb-t2">{new Date(r.createdAt).toLocaleString('zh-CN')}</div>
          {r.status === 'REJECTED' && r.rejectReason && (
            <div className="text-sm text-snb-t2">
              {t('invoice.requests.rejectReason')}: {r.rejectReason}
            </div>
          )}
          <div className="flex gap-2">
            {r.status === 'PENDING' && (
              <Button size="sm" onClick={() => cancel(r.id)}>{t('invoice.requests.cancel')}</Button>
            )}
            {r.status === 'ISSUED' && (
              <Button
                size="sm"
                variant="primary"
                onClick={() => downloadPdf(`/requests/${r.id}/pdf`, `${r.requestNo}.pdf`)}
              >
                {t('invoice.requests.download')}
              </Button>
            )}
          </div>
        </Card>
      ))}
    </div>
  )
}
