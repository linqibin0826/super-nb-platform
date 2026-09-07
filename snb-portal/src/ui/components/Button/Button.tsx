import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'overlay' | 'hero'
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
}

// 动效：小位移走 200ms + ease-snb-quick；按压 100ms 缩到 .97（pointer-down 即反馈）
// 焦点环走 --snb-focus：深色是半透安全橙（原值不动），浅色是实色熔铁橙
// （半透橙压纸只有 2.09:1，白天档必须实色才够 3:1）
const base =
  'inline-flex items-center justify-center gap-2 font-medium transition-[background-color,color,border-color,transform,box-shadow] duration-quick ease-snb-quick focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus disabled:cursor-not-allowed'

// 主按钮四态（与 lib/cta.ts 的链接版逐字同源）：强调色填充 / hover 加深 / press 再加深 + 缩 .97 /
// 禁用 elv 底 t3 字。🪦 键帽底边（shadow-edge-*）与 1px 抬沉随网吧退役。
const ctaFourStates =
  'bg-snb-cta font-semibold text-snb-cta-fg hover:bg-snb-cta-hover active:bg-snb-cta-press active:scale-[0.97] active:duration-press disabled:bg-snb-elv disabled:text-snb-t3 disabled:scale-100'

const variants: Record<ButtonVariant, string> = {
  // 签名主键（🪦 霓虹橙填充与偏移硬阴影随霓虹退役）：
  // 主按钮 = 强调色填充 + `--snb-cta-fg`（浅白字 5.0:1 / 深黑字 8.3:1）；
  // 🪦 旧霓虹橙底白字 2.61:1 的口径随 v2 退役
  primary: ctaFourStates,
  // 次按钮=白胶囊：panel 底 + hairline-strong 描边 + 卡片投影（apple.com 的 secondary）
  secondary:
    'border border-snb-hairline-strong bg-snb-panel text-snb-t1 shadow-card hover:border-snb-hairline-heavy active:scale-[0.97] active:duration-press disabled:border-snb-hairline disabled:bg-transparent disabled:text-snb-t3 disabled:shadow-none disabled:scale-100',
  ghost:
    'bg-transparent text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1 active:scale-[0.97] active:duration-press disabled:bg-transparent disabled:text-snb-t3 disabled:opacity-60 disabled:scale-100',
  // 压在图片上的浮键（赞/藏/下载/复制）：实心暗底 + 内描边（🪦 blur 随零玻璃退役，
  // 底加深一档补回图片上的辨识度），不用硬阴影——阴影在图片上不读数。
  // 🚨 **两档都保持暗底纸白字，不许跟着主题翻**：图片可读性设施与主题无关，
  // 白天把它翻成浅底 = 压在浅色图上直接瞎（浅色化最经典的翻车点）
  overlay:
    'bg-black/55 text-white ring-1 ring-inset ring-white/25 hover:bg-black/75 disabled:opacity-50',
  // 暗景大 CTA：与 primary 同一组四态（🪦 bg-core 已随管芯退役——那个键删了以后
  // 这里曾静默失效成透明钮，换键值时最典型的坑）
  hero: ctaFourStates,
}

// 尺寸：md = CTA 全站唯一数值（高 44 / px 22 / 胶囊 / 15px）；xs/sm 是图片浮键与密集工具条用的小钮
const sizes: Record<ButtonSize, string> = {
  xs: 'rounded-full px-2.5 py-1 text-xs',
  sm: 'rounded-full px-3 py-1.5 text-xs',
  md: 'h-11 rounded-full px-[22px] text-[15px]',
  lg: 'h-12 rounded-full px-6 text-base',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', className, ...rest },
  ref
) {
  return <button ref={ref} className={cx(base, variants[variant], sizes[size], className)} {...rest} />
})
