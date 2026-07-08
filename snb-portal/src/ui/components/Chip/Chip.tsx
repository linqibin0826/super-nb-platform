import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface ChipProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  active?: boolean
}

/** 筛选胶囊（studio 灵感库类目 chips 同款） */
export const Chip = forwardRef<HTMLButtonElement, ChipProps>(function Chip(
  { active = false, className, ...rest },
  ref
) {
  return (
    <button
      ref={ref}
      className={cx(
        'rounded-full border px-3 py-1 text-sm transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50',
        active
          ? 'border-primary-500 bg-primary-500 text-white shadow-sm'
          : 'border-snb-hairline-strong bg-snb-panel/60 text-snb-t2 hover:border-primary-500/60 hover:text-primary-500',
        className
      )}
      {...rest}
    />
  )
})
