import { cx } from '../../lib/cx'

export type AmbientVariant = 'hero' | 'dusk' | 'mesh'

export interface AmbientBackgroundProps {
  /** hero=顶部暖光(studio)；dusk=页脚落日余晖(learn 暮色)；mesh=浅色网格光(fork) */
  variant?: AmbientVariant
  /** false 时用 absolute 定位（容器内局部氛围），默认 fixed 全屏 */
  fixed?: boolean
  className?: string
}

/* 🪦 港风霓虹大气（暖橙×青玉×品红三色 rgba 光晕）随零发光 v2 退役——
   rgb 写法躲过了第一轮 hex 扫描，靠消费面清点才抓出来。
   v2 的氛围只有一种：沥青顶灯提亮（#161A20，与 fork PublicShell/开卡台同配方），
   背景无彩色——彩色只属于安全橙那一小撮状态灯。三个 variant 键保留（API 不动），
   差异只剩灯位：hero=顶部中央 / dusk=底部余晖 / mesh=双侧低亮。 */
const recipes: Record<AmbientVariant, string> = {
  hero: 'radial-gradient(940px 640px at 50% 22%, #161A20 0%, transparent 66%)',
  dusk: 'radial-gradient(820px 460px at 50% 104%, #161A20 0%, transparent 64%)',
  mesh: 'radial-gradient(680px 420px at 12% -8%, #14171D 0%, transparent 62%), radial-gradient(600px 380px at 92% 2%, #14171D 0%, transparent 60%)',
}

/** 氛围光层：暗色的亮度靠它而非抬高底色（learn 军规） */
export function AmbientBackground({ variant = 'hero', fixed = true, className }: AmbientBackgroundProps) {
  return (
    <div
      aria-hidden="true"
      className={cx('pointer-events-none inset-0 z-0', fixed ? 'fixed' : 'absolute', className)}
      style={{ background: recipes[variant] }}
    />
  )
}
