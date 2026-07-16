import { useEffect, useState } from 'react'
import { Alert, Button, Card, Input } from '../../ui'
import { t } from '../../i18n'
import { api, downloadPdf, invoiceUpload, InvoiceApiError, type AdminDetailT, type AdminRowT } from '../api'
import { ErrorBar, Loading, PageHead } from './shared'
import { fmtYuanGrouped, rmbUpper } from '../fee'

const STATUSES = ['PENDING', 'INVOICING', 'ISSUED', 'REJECTED', 'CANCELLED', ''] as const

/** 发票管理(柜台,站长自用):队列行→点开=mini 票面+操作;非 admin 由后端 403 把门。 */
export function AdminPage() {
  const [status, setStatus] = useState<string>('PENDING')
  const [page, setPage] = useState(1)
  const [rows, setRows] = useState<AdminRowT[] | null>(null)
  const [total, setTotal] = useState(0)
  const [error, setError] = useState('')
  const [forbidden, setForbidden] = useState(false)
  const [openId, setOpenId] = useState<string | null>(null)
  const [detail, setDetail] = useState<AdminDetailT | null>(null)
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

  const toggleRow = (id: string) => {
    if (openId === id) {
      setOpenId(null)
      setDetail(null)
      return
    }
    setOpenId(id)
    setDetail(null)
    setReason('')
    setRefundFee(true)
    api.adminDetail(id).then(setDetail).catch((e) => setError(String(e.message)))
  }

  const act = async (fn: () => Promise<unknown>) => {
    setBusy(true)
    setError('')
    try {
      await fn()
      setOpenId(null)
      setDetail(null)
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

  const head = (
    <PageHead eyebrow={t('invoice.admin.eyebrow')} title={t('invoice.admin.title')} sub={t('invoice.admin.sub')} />
  )

  if (forbidden) return <>{head}<Alert tone="warning">{t('invoice.admin.forbidden')}</Alert></>

  return (
    <>
      {head}
      <div className="mb-4 flex flex-wrap gap-2">
        {STATUSES.map((s) => (
          <Button
            key={s || 'all'}
            size="sm"
            variant={status === s ? 'primary' : 'secondary'}
            onClick={() => {
              setStatus(s)
              setPage(1)
              setOpenId(null)
              setDetail(null)
            }}
          >
            {s === '' ? t('invoice.admin.all') : t(`invoice.requests.statuses.${s}`)}
          </Button>
        ))}
      </div>
      {error && <div className="mb-4"><ErrorBar msg={error} /></div>}
      {!rows ? (
        <Loading />
      ) : rows.length === 0 ? (
        <div className="rounded-xl border-[1.5px] border-dashed border-snb-hairline-strong p-16 text-center text-sm text-snb-t3">
          {t('invoice.requests.empty')}
        </div>
      ) : (
        <Card className="overflow-hidden p-0">
          {rows.map((r) => (
            <div key={r.id}>
              <button
                type="button"
                onClick={() => toggleRow(r.id)}
                className="flex w-full items-center gap-4 border-b border-snb-hairline px-5 py-3.5 text-left text-[13.5px] transition-colors hover:bg-snb-t1/5"
              >
                <span className="w-[210px] flex-none font-mono font-semibold max-md:w-auto max-md:flex-1">
                  {r.requestNo}
                </span>
                <span className="min-w-0 flex-1 truncate text-snb-t2 max-md:hidden">{r.email}</span>
                <span className="w-[110px] flex-none text-right font-mono font-semibold tabular-nums">
                  ¥{fmtYuanGrouped(Math.round(r.amount * 100))}
                </span>
                <span className="w-[90px] flex-none text-right tabular-nums text-snb-t3 max-md:hidden">
                  {r.fee === 0
                    ? t('invoice.admin.freeShort')
                    : t('invoice.admin.feeCol', { fee: fmtYuanGrouped(Math.round(r.fee * 100)) })}
                </span>
                <span className={`iv-st-pill iv-st-${r.status}`}>{t(`invoice.requests.statuses.${r.status}`)}</span>
              </button>

              {openId === r.id && (
                <div className="iv-adm-detail border-b border-snb-hairline p-5">
                  {!detail ? (
                    <div className="py-6 text-center text-sm text-snb-t3">{t('invoice.common.loading')}</div>
                  ) : (
                    <div className="grid items-start gap-5 md:grid-cols-[minmax(0,1fr)_300px]">
                      <div className="iv-mini-fapiao">
                        <div className="iv-fp-field">
                          <span className="lb">{t('invoice.admin.userLabel')}</span>
                          <span className="vl font-mono text-[12.5px]">{detail.email} · uid {detail.userId}</span>
                        </div>
                        <div className="iv-fp-field">
                          <span className="lb">{t('invoice.apply.buyerSide')}</span>
                          <span className="vl">
                            {detail.profileType === 'COMPANY'
                              ? t('invoice.profiles.typeCompany')
                              : t('invoice.profiles.typePersonal')}
                            {' · '}
                            {detail.profileTitle}
                          </span>
                        </div>
                        <div className="iv-fp-field">
                          <span className="lb">{t('invoice.apply.taxLabel')}</span>
                          <span className={`vl font-mono ${detail.profileTaxNo ? '' : 'dim'}`}>
                            {detail.profileTaxNo || '—'}
                          </span>
                        </div>
                        {(detail.profileRegAddress || detail.profileBankName) && (
                          <div className="iv-fp-field">
                            <span className="lb">{t('invoice.admin.extraLabel')}</span>
                            <span className="vl text-[12px] text-snb-t3">
                              {[detail.profileRegAddress, detail.profileRegPhone, detail.profileBankName, detail.profileBankAccount]
                                .filter(Boolean)
                                .join(' · ')}
                            </span>
                          </div>
                        )}
                        <div className="iv-fp-field">
                          <span className="lb">{t('invoice.apply.sumSide')}</span>
                          <span className="vl font-mono font-bold">
                            ¥{fmtYuanGrouped(Math.round(detail.amount * 100))}
                            <span className="iv-doc ml-2 font-normal">（{rmbUpper(Math.round(detail.amount * 100))}）</span>
                          </span>
                        </div>
                        <div className="iv-fp-field">
                          <span className="lb">{t('invoice.apply.feeSide')}</span>
                          <span className="vl">
                            {detail.fee === 0
                              ? t('invoice.requests.feeFreeLine')
                              : `¥${fmtYuanGrouped(Math.round(detail.fee * 100))}${detail.feeChargedAt ? t('invoice.admin.feeChargedSuffix') : ''}`}
                          </span>
                        </div>
                        {detail.remark && (
                          <div className="iv-fp-field">
                            <span className="lb">{t('invoice.apply.remarkSide')}</span>
                            <span className="vl text-snb-t3">{detail.remark}</span>
                          </div>
                        )}
                        {detail.rejectReason && (
                          <div className="iv-fp-field">
                            <span className="lb">{t('invoice.requests.rejectReason')}</span>
                            <span className="vl" style={{ color: 'var(--iv-seal)' }}>{detail.rejectReason}</span>
                          </div>
                        )}
                        <table className="mt-2 w-full border-t border-dashed border-snb-hairline-strong text-[12.5px]">
                          <tbody>
                            {detail.orders.map((o) => (
                              <tr key={o.orderId}>
                                <td className="py-1 font-mono">{o.orderNo}</td>
                                <td className="py-1 text-snb-t3">{new Date(o.completedAt).toLocaleDateString('sv')}</td>
                                <td className="py-1 text-right font-mono tabular-nums">
                                  ¥{fmtYuanGrouped(Math.round(o.amount * 100))}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>

                      <div className="flex flex-col gap-2.5">
                        {detail.status === 'PENDING' && (
                          <Button variant="primary" disabled={busy} onClick={() => act(() => api.adminCharge(detail.id))}>
                            {detail.fee === 0 ? t('invoice.admin.chargeFree') : t('invoice.admin.charge')}
                          </Button>
                        )}
                        {(detail.status === 'INVOICING' || detail.status === 'ISSUED') && (
                          <>
                            <label className="block cursor-pointer rounded-lg border-[1.5px] border-dashed border-snb-hairline-strong px-4 py-3 text-center text-[13px] text-snb-t2 transition-colors hover:border-primary-500 hover:text-snb-t1">
                              <input
                                type="file"
                                accept="application/pdf"
                                className="hidden"
                                onChange={(e) => {
                                  const f = e.target.files?.[0]
                                  e.target.value = '' // 复位:上传失败后重选同一文件仍能触发 change
                                  if (f) uploadPdf(detail.id, f)
                                }}
                              />
                              <b>{t('invoice.admin.upload')}</b>
                              <span className="mt-0.5 block text-[11.5px] text-snb-t3">{t('invoice.admin.uploadHint')}</span>
                            </label>
                            <Button
                              variant="secondary"
                              onClick={() => downloadPdf(`/admin/requests/${detail.id}/pdf`, `${detail.requestNo}.pdf`)}
                            >
                              {t('invoice.admin.download')}
                            </Button>
                          </>
                        )}
                        {(detail.status === 'PENDING' || detail.status === 'INVOICING') && (
                          <div className="flex flex-col gap-2 border-t border-snb-hairline pt-2.5">
                            <Input
                              value={reason}
                              onChange={(e) => setReason(e.target.value)}
                              placeholder={t('invoice.admin.rejectReason')}
                            />
                            {detail.status === 'INVOICING' && (
                              <label className="flex cursor-pointer items-center gap-1.5 text-xs text-snb-t2">
                                <input
                                  type="checkbox"
                                  checked={refundFee}
                                  onChange={(e) => setRefundFee(e.target.checked)}
                                />
                                {t('invoice.admin.refundFee')}
                              </label>
                            )}
                            <Button
                              variant="secondary"
                              className="!text-[color:var(--iv-seal)] hover:!border-[color:var(--iv-seal)]"
                              disabled={busy || !reason.trim()}
                              onClick={() => act(() => api.adminReject(detail.id, reason.trim(), refundFee))}
                            >
                              {t('invoice.admin.reject')}
                            </Button>
                          </div>
                        )}
                        <div className="text-[11.5px] leading-relaxed text-snb-t3">{t('invoice.admin.opsNote')}</div>
                      </div>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </Card>
      )}
      {total > 0 && (
        <div className="mt-4 flex items-center gap-3 text-sm text-snb-t2">
          <Button size="sm" variant="secondary" disabled={page <= 1} onClick={() => setPage(page - 1)}>←</Button>
          <span>{page} / {Math.max(1, Math.ceil(total / 20))}</span>
          <Button size="sm" variant="secondary" disabled={page * 20 >= total} onClick={() => setPage(page + 1)}>→</Button>
        </div>
      )}
    </>
  )
}
