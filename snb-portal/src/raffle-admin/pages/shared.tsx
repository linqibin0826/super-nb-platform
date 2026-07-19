import type { ReactNode, SelectHTMLAttributes } from 'react'
import { Alert, Skeleton } from '../../ui'
import { t } from '../../i18n'
import type { CampaignStatus } from '../api'

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

/** 状态指示灯:机房面板的活体信号——进行中常亮微脉动,已开奖常亮不动,已作废熄灭 */
export function Lamp({ status }: { status: CampaignStatus }) {
  return <span className={`rf-lamp rf-lamp-${status}`} aria-hidden="true" />
}

/** 跳线柱:奖品清单里 payload 是否已接线(已生成=lit,待生成=空位) */
export function Jack({ lit }: { lit: boolean }) {
  return <span className={`rf-jack ${lit ? 'rf-jack-lit' : ''}`} aria-hidden="true" />
}

/** 读数井:mono 数值嵌浅凹槽,机房仪表盘既视感 */
export function Well({ children }: { children: ReactNode }) {
  return <span className="rf-well font-mono text-[12.5px] text-snb-t2">{children}</span>
}

/** 盒式下拉:与 Input 同皮肤(rf-select),可选 label 与紧凑尺寸;选项用 children 传 <option> */
export function RfSelect({
  label,
  size,
  className,
  ...rest
}: { label?: string; size?: 'sm' } & Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'>) {
  return (
    <div className={className}>
      {label && <label className="mb-1.5 block text-sm font-medium text-snb-t2">{label}</label>}
      <select className={`rf-select ${size === 'sm' ? 'rf-select-sm' : ''}`} {...rest} />
    </div>
  )
}

/** 面板分区:铭牌体标签 + 顶部分隔线,表单/清单里成组归拢字段用 */
export function Cluster({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="rf-cluster">
      <div className="rf-plate-label mb-3">{label}</div>
      {children}
    </div>
  )
}

/** 页头:小标 + 大标题(可选行内状态灯) + 副标 */
export function PageHead({
  eyebrow,
  title,
  sub,
  status,
}: {
  eyebrow: string
  title: string
  sub?: string
  status?: ReactNode
}) {
  return (
    <div className="mb-7">
      <div className="text-xs font-semibold uppercase tracking-wide text-snb-t3">{eyebrow}</div>
      <div className="flex items-center gap-2.5">
        <h1 className="font-display text-3xl font-semibold tracking-wide">{title}</h1>
        {status}
      </div>
      {sub && <p className="mt-1.5 text-sm text-snb-t3">{sub}</p>}
    </div>
  )
}
