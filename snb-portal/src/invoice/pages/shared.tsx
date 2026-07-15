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
