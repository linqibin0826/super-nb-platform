import { useId } from 'react'
import { Alert, Skeleton } from '../../ui'
import { t } from '../../i18n'

/** 页面级加载骨架 */
export function Loading() {
  return (
    <div className="space-y-3">
      <Skeleton className="h-8 w-1/3" />
      <Skeleton className="h-24 w-full" />
      <Skeleton className="h-24 w-full" />
    </div>
  )
}

/** 页面级错误条(带消息插值) */
export function ErrorBar({ msg }: { msg: string }) {
  return <Alert tone="danger">{t('invoice.common.error', { msg })}</Alert>
}

/** 页头:仿宋红眉题(票据联次) + 宋体大标题 + 副标 */
export function PageHead({ eyebrow, title, sub }: { eyebrow: string; title: string; sub?: string }) {
  return (
    <div className="mb-7">
      <div className="iv-eyebrow">{eyebrow}</div>
      <h1 className="font-display text-3xl font-semibold tracking-wide">{title}</h1>
      {sub && <p className="mt-1.5 text-sm text-snb-t3">{sub}</p>}
    </div>
  )
}

const sealDate = (iso: string) => new Date(iso).toLocaleDateString('sv').replaceAll('-', '.')

/** 已开票:红色椭圆章(曲线刊头+五角星+状态+日期入章) */
export function SealIssued({ issuedAt }: { issuedAt: string | null }) {
  const arcId = useId()
  return (
    <svg className="iv-seal-red" width="118" height="82" viewBox="0 0 220 150" role="img"
         aria-label={t('invoice.requests.statuses.ISSUED')}>
      <ellipse cx="110" cy="75" rx="102" ry="66" fill="none" stroke="currentColor" strokeWidth="3.5" />
      <path id={arcId} d="M 22 82 A 90 58 0 0 1 198 82" fill="none" />
      <text fontSize="15" letterSpacing="3.5" fill="currentColor" fontFamily="Georgia,serif">
        <textPath href={`#${arcId}`} startOffset="50%" textAnchor="middle">SUPER·NB·API·SERVICE</textPath>
      </text>
      <polygon fill="currentColor"
        points="110,52 113.5,62.5 124.5,62.5 115.6,69 119,79.5 110,73 101,79.5 104.4,69 95.5,62.5 106.5,62.5" />
      <text x="110" y="106" textAnchor="middle" fontSize="26" letterSpacing="8" fill="currentColor"
        fontFamily="'FangSong','STFangsong','仿宋',serif" fontWeight="700">
        {t('invoice.requests.statuses.ISSUED')}
      </text>
      {issuedAt && (
        <text x="110" y="126" textAnchor="middle" fontSize="12" letterSpacing="2" fill="currentColor"
          fontFamily="ui-monospace,monospace">{sealDate(issuedAt)}</text>
      )}
    </svg>
  )
}

/** 开票中:蓝色圆形受理章(银行章式样) */
export function SealAccepted() {
  return (
    <svg className="iv-seal-blue" width="96" height="96" viewBox="0 0 160 160" role="img"
         aria-label={t('invoice.requests.statuses.INVOICING')}>
      <circle cx="80" cy="80" r="72" fill="none" stroke="currentColor" strokeWidth="3" />
      <circle cx="80" cy="80" r="58" fill="none" stroke="currentColor" strokeWidth="1.2" />
      <text x="80" y="82" textAnchor="middle" fontSize="21" letterSpacing="5" fill="currentColor"
        fontFamily="'FangSong','STFangsong','仿宋',serif" fontWeight="700">
        {t('invoice.requests.sealAccepted')}
      </text>
      <text x="80" y="104" textAnchor="middle" fontSize="12" letterSpacing="1" fill="currentColor"
        fontFamily="'FangSong','STFangsong','仿宋',serif">
        {t('invoice.requests.statuses.INVOICING')}
      </text>
    </svg>
  )
}

/** 待受理:虚线未盖章位 */
export function SealWait() {
  return (
    <div className="iv-seal-wait">
      {t('invoice.requests.statuses.PENDING')}
      <br />
      {t('invoice.requests.sealWaitNote')}
    </div>
  )
}

/** 已驳回:斜盖红框章 */
export function SealRejected() {
  return <div className="iv-seal-rect">{t('invoice.requests.statuses.REJECTED')}</div>
}

/** 已撤回:灰章淡化 */
export function SealCancelled() {
  return <div className="iv-seal-gray">{t('invoice.requests.statuses.CANCELLED')}</div>
}
