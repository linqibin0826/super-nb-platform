import type { ReactNode, SelectHTMLAttributes } from 'react'
import { Alert, Badge, Skeleton, cx, type BadgeTone } from '../../ui'
import type { AccountStatus, RefundStatus, SubStatus } from '../api'

/** 账本声线:所有数据值(邮箱/日期/IP/卡号/金额)一律等宽 + 等距数字 */
export const MONO = 'font-mono tabular-nums'

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

/** 页头:标题 + 可选副行 + 右侧操作区 */
export function PageHead({ title, sub, children }: { title: string; sub?: ReactNode; children?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-xl font-semibold text-snb-t1">{title}</h1>
        {sub && <p className={cx('mt-1 text-xs text-snb-t3', MONO)}>{sub}</p>}
      </div>
      <div className="flex items-center gap-2">{children}</div>
    </div>
  )
}

/** 分组眉题:字距拉开的小标签 + 延伸线,给表单/卡片内部划段落 */
export function SectionLabel({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cx('flex items-center gap-3', className)}>
      <span className="flex-none text-xs font-semibold tracking-[0.2em] text-snb-t3">{children}</span>
      <span className="h-px flex-1 border-t border-snb-hairline" aria-hidden="true" />
    </div>
  )
}

/** 距今天数(按本地日历日,忽略时分):负=已过期 */
export function daysUntil(dateStr: string): number {
  const [y, m, d] = dateStr.slice(0, 10).split('-').map(Number)
  const target = new Date(y, m - 1, d).getTime()
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  return Math.round((target - today) / 86_400_000)
}

/** 扣款/跟进倒计时:≤7 天亮橙(钱要出去了),其余弱字;过期读作逾期 */
export function Dday({ date }: { date: string }) {
  const n = daysUntil(date)
  const label = n === 0 ? '今天' : n < 0 ? `逾期${-n}天` : `D-${n}`
  return (
    <span className={cx('text-xs', MONO, n <= 7 ? 'font-semibold text-snb-safety' : 'text-snb-t3')}>{label}</span>
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
        className="w-full rounded-lg border border-snb-hairline-strong bg-transparent px-3 py-2 text-sm text-snb-t1 transition-colors duration-quick ease-snb hover:border-snb-hairline-heavy focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus"
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

/** 服务名的账本缩写:列表/票据行里省地方 */
export function serviceShort(service: 'CHATGPT' | 'CLAUDE'): string {
  return service === 'CHATGPT' ? 'GPT' : 'Claude'
}

/** 订阅状态徽章 */
export function SubStatusBadge({ status }: { status: SubStatus }) {
  return <Badge tone={SUB_TONES[status]}>{SUB_LABELS[status]}</Badge>
}

/** 常开地区(站长 2026-08-18 拍板);非标存量值由表单兜底 option 显示,不会被吞 */
export const REGIONS = ['美国', '日本', '菲律宾', '马来西亚', '加拿大', '新加坡']

export const REFUND_LABELS: Record<RefundStatus, string> = {
  NONE: '无需退款',
  PENDING: '待申诉',
  APPEALING: '申诉中',
  REFUNDED: '已到账',
  REJECTED: '被拒',
}
