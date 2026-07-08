import { cx } from '../../lib/cx'

export type AmbientVariant = 'hero' | 'dusk' | 'mesh'

export interface AmbientBackgroundProps {
  /** hero=顶部暖光(studio)；dusk=页脚落日余晖(learn 暮色)；mesh=浅色网格光(fork) */
  variant?: AmbientVariant
  /** false 时用 absolute 定位（容器内局部氛围），默认 fixed 全屏 */
  fixed?: boolean
  className?: string
}

const recipes: Record<AmbientVariant, string> = {
  hero: 'radial-gradient(at 20% 0%, rgba(204, 120, 92, 0.1) 0px, transparent 55%), radial-gradient(at 85% 8%, rgba(181, 99, 74, 0.06) 0px, transparent 50%)',
  dusk: 'radial-gradient(ellipse 120% 42% at 50% 108%, rgba(204, 120, 92, 0.15) 0%, rgba(224, 96, 76, 0.05) 45%, transparent 72%), radial-gradient(ellipse 90% 34% at 50% -12%, rgba(226, 168, 143, 0.06) 0%, transparent 70%)',
  mesh: 'radial-gradient(at 40% 20%, rgba(204, 120, 92, 0.12) 0px, transparent 50%), radial-gradient(at 80% 0%, rgba(204, 120, 92, 0.08) 0px, transparent 50%), radial-gradient(at 0% 50%, rgba(204, 120, 92, 0.08) 0px, transparent 50%)',
}

/** 氛围光层：品牌签名元素。暗色主题的亮度靠它而非抬高底色（learn 军规） */
export function AmbientBackground({ variant = 'hero', fixed = true, className }: AmbientBackgroundProps) {
  return (
    <div
      aria-hidden="true"
      className={cx('pointer-events-none inset-0 z-0', fixed ? 'fixed' : 'absolute', className)}
      style={{ background: recipes[variant] }}
    />
  )
}
