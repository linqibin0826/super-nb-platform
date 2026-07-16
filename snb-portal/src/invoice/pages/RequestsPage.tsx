import { useEffect, useState } from 'react'
import { Button } from '../../ui'
import { t } from '../../i18n'
import { api, downloadPdf, type RequestT } from '../api'
import {
  ErrorBar,
  Loading,
  PageHead,
  SealAccepted,
  SealCancelled,
  SealIssued,
  SealRejected,
  SealWait,
} from './shared'
import { fmtYuanGrouped, rmbUpper } from '../fee'

const day = (iso: string) => new Date(iso).toLocaleDateString('sv')

/** 状态章:每张存根右侧盖一枚,状态即印章 */
function Seal({ r }: { r: RequestT }) {
  switch (r.status) {
    case 'ISSUED':
      return <SealIssued issuedAt={r.issuedAt} />
    case 'INVOICING':
      return <SealAccepted />
    case 'PENDING':
      return <SealWait />
    case 'REJECTED':
      return <SealRejected />
    case 'CANCELLED':
      return <SealCancelled />
  }
}

/** 手续费一行:按状态措辞(待受理=受理时扣;免收;其余中性) */
function feeLine(r: RequestT): string {
  if (r.fee === 0) return t('invoice.requests.feeFreeLine')
  const fee = '¥' + fmtYuanGrouped(Math.round(r.fee * 100))
  if (r.status === 'PENDING') return t('invoice.requests.feeOnAccept', { fee })
  return t('invoice.requests.feeCharged', { fee })
}

/** 我的申请(存根联):骑缝存根卡 + 状态印章;PENDING 可撤回,ISSUED 可下载。 */
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

  const head = (
    <PageHead eyebrow={t('invoice.requests.eyebrow')} title={t('invoice.tabs.requests')} sub={t('invoice.requests.sub')} />
  )

  if (error) return <>{head}<ErrorBar msg={error} /></>
  if (!rows) return <>{head}<Loading /></>
  if (rows.length === 0) {
    return (
      <>
        {head}
        <div className="rounded-xl border-[1.5px] border-dashed border-snb-hairline-strong p-16 text-center text-sm text-snb-t3">
          {t('invoice.requests.empty')}
        </div>
      </>
    )
  }

  return (
    <>
      {head}
      <div className="flex flex-col gap-4">
        {rows.map((r) => (
          <div key={r.id} className={`iv-stub ${r.status === 'CANCELLED' ? 'dim' : ''}`}>
            {/* 存根区(骑缝) */}
            <div className="iv-stub-l">
              <div className="iv-stub-tag">{t('invoice.requests.stubTag')}</div>
              <div className="font-mono text-[13px] font-semibold tracking-tight">{r.requestNo}</div>
              <div className="mt-0.5 text-[12.5px] text-snb-t3">
                {t('invoice.requests.appliedAt', { d: day(r.createdAt) })}
                {r.issuedAt && <> · {t('invoice.requests.issuedAtLine', { d: day(r.issuedAt) })}</>}
              </div>
              <div className="mt-1.5 truncate text-[13.5px] text-snb-t2">{r.profileTitle}</div>
              <div className="iv-stub-cap">{rmbUpper(Math.round(r.amount * 100))}</div>
            </div>

            {/* 状态区 */}
            <div className="iv-stub-r">
              <div className="flex-none">
                <div className="font-mono text-[22px] font-bold tabular-nums">
                  ¥{fmtYuanGrouped(Math.round(r.amount * 100))}
                </div>
                <div className="mt-0.5 text-[12.5px] text-snb-t3">{feeLine(r)}</div>
              </div>
              <div className="min-w-0 flex-1 text-[13px] leading-relaxed text-snb-t3">
                {r.status === 'INVOICING' && t('invoice.requests.invoicingNote')}
                {r.status === 'PENDING' && t('invoice.requests.pendingNote')}
                {r.status === 'REJECTED' && r.rejectReason && (
                  <span style={{ color: 'var(--iv-seal)' }}>
                    {t('invoice.requests.rejectReason')}：{r.rejectReason}
                  </span>
                )}
                {r.remark && r.status !== 'REJECTED' && (
                  <span className="block truncate">{r.remark}</span>
                )}
              </div>
              <div className="iv-seal-wrap">
                <Seal r={r} />
              </div>
              <div className="z-[3] flex flex-none flex-col items-end gap-2">
                {r.status === 'ISSUED' && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => downloadPdf(`/requests/${r.id}/pdf`, `${r.requestNo}.pdf`)}
                  >
                    {t('invoice.requests.download')}
                  </Button>
                )}
                {r.status === 'PENDING' && (
                  <Button size="sm" variant="ghost" onClick={() => cancel(r.id)}>
                    {t('invoice.requests.cancel')}
                  </Button>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </>
  )
}
