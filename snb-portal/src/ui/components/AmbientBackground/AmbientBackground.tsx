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
   v2 的氛围只有一种：顶灯提亮（与 fork PublicShell/开卡台同配方），
   背景无彩色——彩色只属于安全橙那一小撮状态灯。三个 variant 键保留（API 不动），
   差异只剩灯位：hero=顶部中央 / dusk=底部余晖 / mesh=双侧低亮。

   🚨 双档：提亮色走 --snb-ambient-lift（深 #161A20 提亮沥青 / 浅 #FBF9F5 纸面受光）。
   同一手法反向——深色是「底上打一盏灯」，浅色是「纸上落一片光」；两档都只有 ~1.1 的
   亮度差，绝不许变成模糊光球（那是发光，零发光红线）。写死 hex 会让白天档在纸上
   糊出一团深色油渍。 */
const recipes: Record<AmbientVariant, string> = {
  hero: 'radial-gradient(940px 640px at 50% 22%, var(--snb-ambient-lift) 0%, transparent 66%)',
  dusk: 'radial-gradient(820px 460px at 50% 104%, var(--snb-ambient-lift) 0%, transparent 64%)',
  mesh: 'radial-gradient(680px 420px at 12% -8%, var(--snb-ambient-lift-soft) 0%, transparent 62%), radial-gradient(600px 380px at 92% 2%, var(--snb-ambient-lift-soft) 0%, transparent 60%)',
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
