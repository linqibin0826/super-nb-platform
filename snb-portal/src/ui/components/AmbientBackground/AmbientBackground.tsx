import { cx } from '../../lib/cx'

export type AmbientVariant = 'hero' | 'dusk' | 'mesh'

export interface AmbientBackgroundProps {
  /** hero=左侧三团（带侧栏的壳）；dusk=底部一团；mesh=左上一团（无侧栏页面慎用：环境光只垫在有玻璃的壳底下） */
  variant?: AmbientVariant
  /** false 时用 absolute 定位（容器内局部），默认 fixed 全屏底层 */
  fixed?: boolean
  className?: string
}

/* 环境光层（苹果式 v3，spec §3.5 ③）：玻璃需要有东西可折射。钉在左侧，只从侧栏与顶栏透出，
   内容区实白面板不受影响。这是底层环境光不是元素发光，与零发光纪律不冲突。
   两档同配方、只差透明度（深色由 .dark 下的 CSS 变量 --snb-ambient-alpha 缩放）。
   🪦 07-27 的「顶灯提亮」--snb-ambient-lift 配方随网吧退役（那两个槽位保名给行悬停用）。 */
const recipes: Record<AmbientVariant, string> = {
  hero: [
    'radial-gradient(520px 440px at 0% 6%, rgb(var(--snb-brand) / calc(.55 * var(--snb-ambient-alpha, 1))), transparent 66%)',
    'radial-gradient(480px 520px at 16% 52%, rgb(96 132 210 / calc(.42 * var(--snb-ambient-alpha, 1))), transparent 66%)',
    'radial-gradient(560px 460px at 4% 100%, rgb(232 160 120 / calc(.48 * var(--snb-ambient-alpha, 1))), transparent 66%)',
    'radial-gradient(300px 300px at 22% 30%, rgb(255 255 255 / calc(.8 * var(--snb-ambient-white, 1))), transparent 70%)',
  ].join(', '),
  dusk: 'radial-gradient(820px 460px at 50% 104%, rgb(var(--snb-brand) / calc(.35 * var(--snb-ambient-alpha, 1))), transparent 64%)',
  mesh: 'radial-gradient(680px 420px at 12% -8%, rgb(var(--snb-brand) / calc(.40 * var(--snb-ambient-alpha, 1))), transparent 62%)',
}

/** 环境光层：放在壳的最底层（-z-10），玻璃侧栏 / 顶栏透它 */
export function AmbientBackground({ variant = 'hero', fixed = true, className }: AmbientBackgroundProps) {
  return (
    <div
      aria-hidden="true"
      className={cx('pointer-events-none inset-0 -z-10', fixed ? 'fixed' : 'absolute', className)}
      style={{ background: recipes[variant] }}
    />
  )
}
