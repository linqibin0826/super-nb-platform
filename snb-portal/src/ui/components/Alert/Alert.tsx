import type { HTMLAttributes, ReactNode } from 'react'
import { cx } from '../../lib/cx'

export type AlertTone = 'tip' | 'warning' | 'danger' | 'info'
export interface AlertProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  tone?: AlertTone
  title?: ReactNode
}

// 🪦 彩色左边条退役（spec §3.10：「碎卡片各带描边与底色 / 彩色左条」是反模式）。
// 语气只落在标题色上，容器是一块统一面板；tone 同时挂到 data-tone 供消费方/测试选择。
// 语气标题走 *-ink 槽：本色 safety/danger 压染色底与 well 余量不足 4.5:1
const titleTones: Record<AlertTone, string> = {
  tip: 'text-snb-t1',
  warning: 'text-snb-safety-ink',
  danger: 'text-snb-danger-ink',
  info: 'text-snb-t1',
}

/** 提示块：统一面板 + 语气标题 */
export function Alert({ tone = 'tip', title, className, children, ...rest }: AlertProps) {
  return (
    <div
      data-tone={tone}
      role={tone === 'danger' ? 'alert' : undefined}
      className={cx('rounded-2xl bg-snb-well px-[18px] py-4 text-sm leading-relaxed text-snb-t2', className)}
      {...rest}
    >
      {title != null && <p className={cx('mb-1 font-semibold', titleTones[tone])}>{title}</p>}
      {children}
    </div>
  )
}
