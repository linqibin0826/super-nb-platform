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
  // 签名主键：纸白平钮 + 沥青字（零发光 v2，fork .btn-primary 同款——
  // 🪦 霓虹橙填充与偏移硬阴影按压随霓虹退役；主按钮靠明度赢，不靠颜色）
  primary:
    'bg-paper font-semibold text-asphalt hover:bg-[#E2DDD3] active:bg-[#D6D0C4]',
  secondary:
    'border border-snb-hairline-strong bg-snb-elv text-snb-t2 shadow-sm hover:bg-snb-panel hover:text-snb-t1',
  ghost: 'bg-transparent text-snb-t2 hover:bg-snb-t1/5 hover:text-snb-t1',
  // 压在图片上的浮键（赞/藏/下载/复制）：实心暗底 + 内描边（🪦 blur 随零玻璃退役，
  // 底加深一档补回图片上的辨识度），不用硬阴影——阴影在图片上不读数
  overlay:
    'bg-black/55 text-white ring-1 ring-inset ring-white/25 hover:bg-black/75',
  // 暗景大 CTA：与 primary 同族纸白（🪦 bg-core 已随管芯退役——那个键删了以后
  // 这里曾静默失效成透明钮，换键值时最典型的坑）
  hero: 'bg-paper font-semibold text-asphalt hover:bg-[#E2DDD3] active:translate-y-px',
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
