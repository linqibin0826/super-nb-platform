import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'overlay' | 'hero'
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
}

const base =
  'inline-flex items-center justify-center gap-2 font-medium transition-all duration-200 ease-out focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 disabled:cursor-not-allowed disabled:opacity-50'

const variants: Record<ButtonVariant, string> = {
  // 签名主键：扁平赤陶（填充压深到 600，白字对比 ≈AA）+ 偏移硬阴影按压（hover 下沉 1px、active 2px）
  primary:
    'bg-primary-600 text-white shadow-[0_3px_0_#97503C] hover:bg-primary-700 hover:translate-y-px hover:shadow-[0_2px_0_#7A4231] active:translate-y-0.5 active:shadow-[0_1px_0_#7A4231] disabled:translate-y-0 disabled:shadow-[0_3px_0_#97503C]',
  secondary:
    'border border-snb-hairline-strong bg-snb-elv text-snb-t2 shadow-sm hover:bg-snb-panel hover:text-snb-t1',
  ghost: 'bg-transparent text-snb-t2 hover:bg-snb-t1/5 hover:text-snb-t1',
  // 压在图片上的玻璃键（赞/藏/下载/复制浮层）：暗玻璃 + 内描边，不用硬阴影——阴影在图片上不读数
  overlay:
    'bg-black/40 text-white ring-1 ring-inset ring-white/25 backdrop-blur-sm hover:bg-black/60',
  // 电影页 CTA：白底黑字（零品牌色）
  hero: 'bg-white text-black hover:bg-[#E5E7EB] active:translate-y-px',
}

const sizes: Record<ButtonSize, string> = {
  xs: 'rounded-lg px-2.5 py-1 text-xs',
  sm: 'rounded-lg px-3 py-1.5 text-xs',
  md: 'rounded-xl px-4 py-2.5 text-sm',
  lg: 'rounded-2xl px-6 py-3 text-base',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', className, ...rest },
  ref
) {
  return <button ref={ref} className={cx(base, variants[variant], sizes[size], className)} {...rest} />
})
