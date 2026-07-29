import { forwardRef, type ButtonHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'overlay' | 'hero'
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
}

// 动效只许四档：常规 120ms（duration-quick）+ 全站唯一缓动 ease-snb
// （🪦 duration-200 ease-out 是浏览器默认档，GlobalParts v3 起不许出现）
// 焦点环走 --snb-focus：深色是半透安全橙（原值不动），浅色是实色熔铁橙
// （半透橙压纸只有 2.09:1，白天档必须实色才够 3:1）
const base =
  'inline-flex items-center justify-center gap-2 font-medium transition-all duration-quick ease-snb focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus disabled:cursor-not-allowed'

// 主按钮四态（GlobalParts v3 §03，与 lib/cta.ts 的链接版逐字同源）：
// 常态 cta 底 / cta-fg 字 + 底边 2 → hover 抬 1px 底边 3（120ms）→ active 沉 1px
// 底边 1（55ms）→ 禁用 elv 底 / t3 字 / 无底边。禁用一律不吃 opacity——
// 半透明的按钮在暗底上会糊成一片，禁用靠换底换字表达。
// 🚨 双档不是简单换值：**深夜是纸白底沥青字、白天是墨块底纸白字**——
// 「浅色里最响的一块只能是最黑的一块」。因此 hover 在深色是提亮（→#FFFFFF）、
// 在浅色是加深（→#0F0E0C）：白天里「更用力」等于更黑。四态数值全在 --snb-cta-* 里翻。
const ctaFourStates =
  'bg-snb-cta font-semibold text-snb-cta-fg shadow-edge-2 hover:-translate-y-px hover:bg-snb-cta-hover hover:shadow-edge-3 active:translate-y-px active:bg-snb-cta-press active:shadow-edge-1 active:duration-press disabled:translate-y-0 disabled:bg-snb-elv disabled:text-snb-t3 disabled:shadow-none'

const variants: Record<ButtonVariant, string> = {
  // 签名主键（🪦 霓虹橙填充与偏移硬阴影随霓虹退役；
  // 🚨「橙底白字」整条退役——实测 2.61:1，橙实底只剩状态灯与金额数字两处合法）
  primary: ctaFourStates,
  // 次按钮=描边款（v3 改：🪦 elv 实底退役，改透明底 hairline-strong 边 + 主字色）
  secondary:
    'border border-snb-hairline-strong bg-transparent text-snb-t1 hover:border-snb-hairline-heavy hover:bg-snb-panel active:translate-y-px active:duration-press disabled:border-snb-hairline disabled:bg-transparent disabled:text-snb-t3 disabled:translate-y-0',
  ghost:
    'bg-transparent text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1 active:translate-y-px active:duration-press disabled:bg-transparent disabled:text-snb-t3 disabled:opacity-60 disabled:translate-y-0',
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

// 尺寸：md = CTA 全站唯一数值（高 44 / px 20 / 圆角 8 / 14px），lg 是它的放大档；
// ⚠️ xs/sm 是图片浮键与密集工具条用的小钮，视觉可小于 44——热区由调用方用透明
// 内边距撑（可点物 ≥44×44 的纪律在热区，不在视觉）
const sizes: Record<ButtonSize, string> = {
  xs: 'rounded-lg px-2.5 py-1 text-xs',
  sm: 'rounded-lg px-3 py-1.5 text-xs',
  md: 'h-11 rounded-[8px] px-5 text-sm',
  lg: 'h-12 rounded-[8px] px-6 text-[15px]',
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  { variant = 'primary', size = 'md', className, ...rest },
  ref
) {
  return <button ref={ref} className={cx(base, variants[variant], sizes[size], className)} {...rest} />
})
