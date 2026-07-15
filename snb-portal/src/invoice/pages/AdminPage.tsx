import { useEffect, useState } from 'react'
import { Alert, Badge, Button, Card, Input } from '../../ui'
import { t } from '../../i18n'
import { api, downloadPdf, invoiceUpload, InvoiceApiError, type AdminDetailT, type AdminRowT } from '../api'
import { ErrorBar, Loading } from './shared'

const STATUSES = ['', 'PENDING', 'INVOICING', 'ISSUED', 'REJECTED', 'CANCELLED'] as const

/** 发票管理(站长自用):非 admin 一律 403 由后端把门,前端只渲染错误提示。 */
export function AdminPage() {
  const [status, setStatus] = useState<string>('PENDING')
  const [page, setPage] = useState(1)
  const [rows, setRows] = useState<AdminRowT[] | null>(null)
  const [total, setTotal] = useState(0)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)
  const [open, setOpen] = useState<AdminDetailT | null>(null)
  const [reason, setReason] = useState('')
  const [refundFee, setRefundFee] = useState(true)
  const [busy, setBusy] = useState(false)

  const load = () => {
    setError('')
    api.adminPage(status, page)
      .then((result) => {
        setRows(result.items)
        setTotal(result.total)
      })
      .catch((e) => {
        if (e instanceof InvoiceApiError && e.status === 403) setForbidden(true)
        else setError(String(e.message))
      })
  }
  useEffect(load, [status, page])

  const act = async (fn: () => Promise<unknown>) => {
    setBusy(true)
    setError('')
    try {
      await fn()
      setOpen(null)
      load()
    } catch (e) {
      setError(String((e as Error).message))
    } finally {
      setBusy(false)
    }
  }

  const uploadPdf = (id: string, file: File) => {
    const form = new FormData()
    form.append('file', file, file.name)
    return act(() => invoiceUpload(`/admin/requests/${id}/pdf`, form))
  }

  if (forbidden) return <Alert tone="warning">{t('invoice.admin.forbidden')}</Alert>

  return (
    <div className="space-y-3">
      <h1 className="text-lg font-medium">{t('invoice.admin.title')}</h1>
      <div className="flex flex-wrap gap-2">
        {STATUSES.map((s) => (
          <Button
            key={s || 'all'}
            size="sm"
            variant={status === s ? 'primary' : undefined}
            onClick={() => {
              setStatus(s)
              setPage(1)
            }}
          >
            {s === '' ? t('invoice.admin.all') : t(`invoice.requests.statuses.${s}`)}
          </Button>
        ))}
      </div>
      {error && <ErrorBar msg={error} />}
      {!rows ? (
        <Loading />
      ) : (
        <>
          {rows.map((r) => (
            <Card key={r.id} className="space-y-2 p-4">
              <div className="flex items-center gap-3">
                <span className="font-mono text-sm">{r.requestNo}</span>
                <Badge tone="gray">{t(`invoice.requests.statuses.${r.status}`)}</Badge>
                <span className="flex-1 truncate text-sm text-snb-t2">{r.email}</span>
                <span className="font-mono">¥{r.amount.toFixed(2)}</span>
                <span className="font-mono text-sm text-snb-t2">fee ¥{r.fee.toFixed(2)}</span>
                <Button size="sm" onClick={() => api.adminDetail(r.id).then(setOpen).catch((e) => setError(String(e.message)))}>
                  {t('invoice.admin.detail')}
                </Button>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                {r.status === 'PENDING' && (
                  <Button size="sm" variant="primary" disabled={busy}
                          onClick={() => act(() => api.adminCharge(r.id))}>
                    {r.fee === 0 ? t('invoice.admin.chargeFree') : t('invoice.admin.charge')}
                  </Button>
                )}
                {(r.status === 'INVOICING' || r.status === 'ISSUED') && (
                  <label className="inline-flex cursor-pointer items-center gap-1 text-sm text-snb-t2">
                    <input
                      type="file"
                      accept="application/pdf"
                      className="hidden"
                      onChange={(e) => e.target.files?.[0] && uploadPdf(r.id, e.target.files[0])}
                    />
                    <span className="rounded border border-snb-line px-2 py-1">{t('invoice.admin.upload')}</span>
                  </label>
                )}
                {(r.status === 'INVOICING' || r.status === 'ISSUED') && (
                  <Button size="sm" onClick={() => downloadPdf(`/admin/requests/${r.id}/pdf`, `${r.requestNo}.pdf`)}>
                    {t('invoice.admin.download')}
                  </Button>
                )}
                {(r.status === 'PENDING' || r.status === 'INVOICING') && (
                  <span className="inline-flex items-center gap-2">
                    <Input value={reason} onChange={(e) => setReason(e.target.value)}
                           placeholder={t('invoice.admin.rejectReason')} />
                    {r.status === 'INVOICING' && (
                      <label className="flex items-center gap-1 text-xs text-snb-t2">
                        <input type="checkbox" checked={refundFee}
                               onChange={(e) => setRefundFee(e.target.checked)} />
                        {t('invoice.admin.refundFee')}
                      </label>
                    )}
                    <Button size="sm" disabled={busy || !reason.trim()}
                            onClick={() => act(() => api.adminReject(r.id, reason.trim(), refundFee))}>
                      {t('invoice.admin.reject')}
                    </Button>
                  </span>
                )}
              </div>
            </Card>
          ))}
          <div className="flex items-center gap-3 text-sm text-snb-t2">
            <Button size="sm" disabled={page <= 1} onClick={() => setPage(page - 1)}>←</Button>
            <span>{page} / {Math.max(1, Math.ceil(total / 20))}</span>
            <Button size="sm" disabled={page * 20 >= total} onClick={() => setPage(page + 1)}>→</Button>
          </div>
        </>
      )}

      {open && (
        <Card className="space-y-2 p-4">
          <div className="flex justify-between">
            <span className="font-mono">{open.requestNo}</span>
            <Button size="sm" onClick={() => setOpen(null)}>×</Button>
          </div>
          <div className="text-sm">{open.email} · uid {open.userId}</div>
          <div className="text-sm">
            {open.profileType === 'COMPANY' ? t('invoice.profiles.typeCompany') : t('invoice.profiles.typePersonal')}
            {' · '}{open.profileTitle}
            {open.profileTaxNo && <span className="font-mono"> · {open.profileTaxNo}</span>}
          </div>
          {(open.profileRegAddress || open.profileBankName) && (
            <div className="text-xs text-snb-t2">
              {[open.profileRegAddress, open.profileRegPhone, open.profileBankName, open.profileBankAccount]
                .filter(Boolean).join(' · ')}
            </div>
          )}
          {open.remark && <div className="text-sm text-snb-t2">备注: {open.remark}</div>}
          <table className="w-full text-sm">
            <tbody>
              {open.orders.map((o) => (
                <tr key={o.orderId} className="border-t border-snb-line">
                  <td className="py-1 font-mono">{o.orderNo}</td>
                  <td className="py-1 text-snb-t2">{new Date(o.completedAt).toLocaleDateString('zh-CN')}</td>
                  <td className="py-1 text-right font-mono">¥{o.amount.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  )
}
