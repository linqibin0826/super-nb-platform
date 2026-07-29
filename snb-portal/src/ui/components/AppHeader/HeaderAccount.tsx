import { cx } from '../../lib/cx'

export interface HeaderAccountProps {
  /**
   * 站内余额数值（已格式化的字符串，如 `23.50`）。
   * 🚨 单位铁律：站内余额一律 `$`（名义计价 1:1），只有充值付款金额才写 `¥`。
   */
  balance: string | number
  /** 余额前缀，默认「网费」；英文站传 "Tab" 一类的网吧腔 */
  label?: string
  /** 头像取首字母用；缺省用 `?` */
  name?: string
  /** 头像链接（我的机位/账户浮卡触发器）；不传则渲染成静态 span */
  href?: string
  className?: string
}

/**
 * 顶栏已登录态（GlobalParts v3 §01）：mono 网费 `$` 金额 + 34px 圆头像。
 * 金额用安全橙——橙实底只剩状态灯与金额数字两处合法，这里是橙**字**不是橙底。
 * 数字走 mono 等宽：余额变化时字宽不跳（数据不闪红线的一部分）。
 */
export function HeaderAccount({ balance, label = '网费', name, href, className }: HeaderAccountProps) {
  const initial = (name?.trim()[0] ?? '?').toUpperCase()
  const avatarClass =
    'grid h-[34px] w-[34px] flex-none place-items-center rounded-full border border-snb-hairline-strong bg-snb-elv text-[13px] font-semibold text-snb-t1 no-underline'
  return (
    <div className={cx('flex items-center gap-3', className)}>
      <span className="whitespace-nowrap font-mono text-[13.5px] text-snb-t2">
        {label} <span className="font-semibold text-snb-safety">${balance}</span>
      </span>
      {href ? (
        <a href={href} aria-label={name} className={avatarClass}>
          {initial}
        </a>
      ) : (
        <span aria-label={name} className={avatarClass}>
          {initial}
        </span>
      )}
    </div>
  )
}
