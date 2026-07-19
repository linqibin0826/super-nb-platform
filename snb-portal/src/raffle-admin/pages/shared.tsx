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
  return <Alert tone="danger">{t('raffle.common.error', { msg })}</Alert>
}

/** 页头:小标 + 大标题 + 副标 */
export function PageHead({ eyebrow, title, sub }: { eyebrow: string; title: string; sub?: string }) {
  return (
    <div className="mb-7">
      <div className="text-xs font-semibold uppercase tracking-wide text-snb-t3">{eyebrow}</div>
      <h1 className="font-display text-3xl font-semibold tracking-wide">{title}</h1>
      {sub && <p className="mt-1.5 text-sm text-snb-t3">{sub}</p>}
    </div>
  )
}
