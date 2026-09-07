import { cx } from '../../lib/cx'

export interface HeaderAccountProps {
  /**
   * 站内余额数值（已格式化的字符串，如 `23.50`）。
   * 🚨 单位铁律：站内余额一律 `$`（名义计价 1:1），只有充值付款金额才写 `¥`。
   */
  balance: string | number
  /** 余额前缀，默认「余额」 */
  label?: string
  /** 头像取首字母用；缺省用 `?` */
  name?: string
  /** 头像链接（控制台/账户浮卡触发器）；不传则渲染成静态 span */
  href?: string
  className?: string
}

/** 顶栏已登录态：「余额 $金额」+ 34px 圆头像。金额用强调色**字**；数字 tabular-nums 字宽不跳。 */
export function HeaderAccount({ balance, label = '余额', name, href, className }: HeaderAccountProps) {
  const initial = (name?.trim()[0] ?? '?').toUpperCase()
  const avatarClass =
    'grid h-[34px] w-[34px] flex-none place-items-center rounded-full bg-snb-elv text-[13px] font-semibold text-snb-t2 no-underline'
  return (
    <div className={cx('flex items-center gap-3', className)}>
      <span className="whitespace-nowrap text-[13px] tabular-nums text-snb-t2">
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
