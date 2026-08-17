import type { ReactNode, SelectHTMLAttributes } from 'react'
import { Alert, Badge, Skeleton, type BadgeTone } from '../../ui'
import type { AccountStatus, RefundStatus, SubStatus } from '../api'

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

/** 页面级错误条 */
export function ErrorBar({ msg }: { msg: string }) {
  return <Alert tone="danger">出错了:{msg}</Alert>
}

/** 页头:标题 + 右侧操作区 */
export function PageHead({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
      <h1 className="text-xl font-semibold text-snb-t1">{title}</h1>
      <div className="flex items-center gap-2">{children}</div>
    </div>
  )
}

/** 原生下拉(与 Input 大致同皮肤;MVP 不引机房人格件) */
export function FieldSelect({
  label,
  className,
  ...rest
}: { label?: string } & SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className={className}>
      {label && <label className="mb-1.5 block text-sm font-medium text-snb-t2">{label}</label>}
      <select
        className="w-full rounded-lg border border-snb-hairline-strong bg-transparent px-3 py-2 text-sm text-snb-t1 focus:outline-none focus:ring-2 focus:ring-snb-terra/40"
        {...rest}
      />
    </div>
  )
}

const ACCOUNT_TONES: Record<AccountStatus, BadgeTone> = {
  ACTIVE: 'success',
  BANNED: 'danger',
  UNVERIFIED: 'warning',
  ABANDONED: 'gray',
}
const ACCOUNT_LABELS: Record<AccountStatus, string> = {
  ACTIVE: '可用',
  BANNED: '已封',
  UNVERIFIED: '未验',
  ABANDONED: '已弃',
}

/** 账号状态徽章 */
export function AccountStatusBadge({ status }: { status: AccountStatus }) {
  return <Badge tone={ACCOUNT_TONES[status]}>{ACCOUNT_LABELS[status]}</Badge>
}

const SUB_TONES: Record<SubStatus, BadgeTone> = {
  FREE: 'gray',
  ACTIVE: 'success',
  EXPIRED: 'warning',
  CANCELED: 'gray',
  BANNED: 'danger',
}
export const SUB_LABELS: Record<SubStatus, string> = {
  FREE: '未付费',
  ACTIVE: '生效中',
  EXPIRED: '已到期',
  CANCELED: '已取消',
  BANNED: '已封',
}

/** 订阅状态徽章 */
export function SubStatusBadge({ status }: { status: SubStatus }) {
  return <Badge tone={SUB_TONES[status]}>{SUB_LABELS[status]}</Badge>
}

export const REFUND_LABELS: Record<RefundStatus, string> = {
  NONE: '无需退款',
  PENDING: '待申诉',
  APPEALING: '申诉中',
  REFUNDED: '已到账',
  REJECTED: '被拒',
}
