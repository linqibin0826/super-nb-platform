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
  // 港风霓虹大气：远处招牌光晕。冷暖对撞是核心——暖橙与青玉不可只留其一
  hero: 'radial-gradient(680px 420px at 10% -8%, rgba(255, 107, 53, 0.16) 0px, transparent 62%), radial-gradient(600px 380px at 92% 2%, rgba(0, 229, 199, 0.11) 0px, transparent 60%)',
  dusk: 'radial-gradient(820px 460px at 62% 104%, rgba(255, 61, 219, 0.09) 0%, transparent 64%), radial-gradient(680px 420px at 12% 96%, rgba(255, 107, 53, 0.13) 0%, transparent 62%)',
  mesh: 'radial-gradient(at 40% 20%, rgba(255, 107, 53, 0.12) 0px, transparent 50%), radial-gradient(at 80% 0%, rgba(0, 229, 199, 0.09) 0px, transparent 50%), radial-gradient(at 0% 60%, rgba(255, 61, 219, 0.06) 0px, transparent 50%)',
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
