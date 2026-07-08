import { forwardRef, type SelectHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface TicketSelectOption {
  value: string
  label: string
}

export interface TicketSelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  options: TicketSelectOption[]
}

/** 规格票下拉：无框下划线 select（studio 生图规格票同款），hover/focus 下划线变色。
 *
 * 原生 select 不设宽度时按"最长的那个 option"撑开自身宽度（浏览器防止切换选项时布局跳动
 * 的默认行为），但这里要的是"看起来像纯文字"的效果，宽度应贴合当前选中值，而非最长选项——
 * 否则文字后面会留一截多余下划线，且不同 select 之间宽度观感不一致。
 * 修法：用一个不可见的镜像文本（相同字号/内边距、白名单空格不折行）在正常流中撑出容器宽度，
 * select 本身绝对定位铺满该容器——绝对定位元素不参与祖先的固有尺寸计算，天然摆脱"按最长
 * option 撑宽"的原生行为，只贴合当前选中值。
 */
export const TicketSelect = forwardRef<HTMLSelectElement, TicketSelectProps>(function TicketSelect(
  { options, className, value, ...rest },
  ref
) {
  const currentLabel = options.find((o) => o.value === value)?.label ?? ''
  return (
    <span className={cx('relative inline-block', className)}>
      <span aria-hidden="true" className="invisible block whitespace-pre py-1 pl-0 pr-5 text-sm font-medium">
        {currentLabel || ' '}
      </span>
      <select
        ref={ref}
        value={value}
        className="absolute inset-0 w-full cursor-pointer appearance-none rounded-none border-0 border-b border-snb-hairline-strong bg-transparent py-1 pl-0 pr-5 text-sm font-medium text-snb-t1 transition-colors duration-200 hover:border-snb-t3 focus:border-primary-500 focus:outline-none disabled:cursor-not-allowed disabled:opacity-60"
        {...rest}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      <svg
        className="pointer-events-none absolute right-0.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-snb-t3"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden="true"
      >
        <path d="m6 9 6 6 6-6" />
      </svg>
    </span>
  )
})
