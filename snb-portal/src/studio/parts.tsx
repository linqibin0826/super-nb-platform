// 全站公用件 v3 —— studio 的接线层（2026-07-29 双档补全时**去重完毕**）。
//
// 【历史】本文件曾是 v3 的 studio 本地实现：那时 src/ui 还是没同步的 vendor 副本，
// 上游 super-nb-ui 已落地 v3 而副本还披着旧皮，只能在这儿先手写一份等收口。
// 收口已经做完（vendor 整包重同步 + tailwind-preset 拉齐），本文件因此收缩成
// **只做适配、不再复制任何数值**——数值全在 vendor 里，双档翻转也全在那儿。
//
// 🚨 别再往这里写颜色。手写一份 = 白天档漏翻一处，而且不报错。
// ⚠️ 保留本文件（而不是让各处直接 import '../ui'）的理由：
//    ① 现有 import 路径不用动；② 空态/错误态的 studio 调用签名与 vendor 不同
//    （这里是扁平的 body/actionLabel/onAction），转接一层比改五个调用点稳。
import { EmptyState as UiEmptyState, ErrorState as UiErrorState } from '../ui'

// 链接版三条配方：直接用 vendor 的（值与 Button 的 primary/secondary/ghost 同源）。
// 三条都不含横向内边距与宽度，调用方按场景补 px-5 / w-full。
export { ctaAnchorClass, secondaryAnchorClass, ghostAnchorClass } from '../ui'

// 状态灯唯一版：vendor 件签名兼容（<StatusLamp state="pending" />）。
// 苹果式 v3：live = 绿在线点 + 透明归零扩散环（--snb-live），灭态走 --snb-lamp-off，两档各自翻。
export { StatusLamp } from '../ui'
export type { StatusLampState as LampState } from '../ui'

// 动效四档 + 两把缓动（v3 §00 / 苹果式 v3）：小位移与悬停走 quick + ease-snb-quick
const MOTION = 'transition-[background-color,color,border-color,transform,box-shadow] duration-quick ease-snb-quick'
const FOCUS = 'focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus'

/** 主按钮四态（`<button>` 版，与 vendor `Button variant="primary" size="md"` 逐字同源）。
 *  苹果式 v3：胶囊 / 高 44 / px 22 / 15px semibold / 强调色填充；hover 加深、press 缩 .97。
 *  🪦 键帽底边（shadow-edge-*）与 1px 抬沉随网吧退役；四态值全在 --snb-cta-* 里翻，本文件一个颜色都不写死。 */
export const ctaClass =
  `inline-flex h-11 items-center justify-center gap-2 rounded-full border-0 px-[22px] text-[15px] font-semibold ${MOTION} ${FOCUS} ` +
  'bg-snb-cta text-snb-cta-fg ' +
  'hover:bg-snb-cta-hover ' +
  'active:bg-snb-cta-press active:scale-[0.97] active:duration-press ' +
  'disabled:cursor-not-allowed disabled:bg-snb-elv disabled:text-snb-t3 disabled:scale-100 ' +
  'motion-reduce:transform-none'

/** 次按钮：白胶囊——panel 底 + hairline-strong 描边 + 卡片投影（apple.com 的 secondary）。 */
export const secondaryClass =
  `inline-flex h-11 items-center justify-center gap-2 rounded-full border border-snb-hairline-strong px-[22px] text-[15px] font-medium ${MOTION} ${FOCUS} ` +
  'bg-snb-panel text-snb-t1 shadow-card hover:border-snb-hairline-heavy ' +
  'active:scale-[0.97] active:duration-press ' +
  'disabled:cursor-not-allowed disabled:border-snb-hairline disabled:bg-transparent disabled:text-snb-t3 disabled:shadow-none disabled:scale-100 ' +
  'motion-reduce:transform-none'

/** 幽灵：无边 + 次字色；hover 提主字色 + 6% 主字色底。 */
export const ghostClass =
  `inline-flex h-11 items-center justify-center gap-2 rounded-full border-0 px-3.5 text-[15px] font-medium ${MOTION} ${FOCUS} ` +
  'bg-transparent text-snb-t2 hover:bg-snb-t1/[0.06] hover:text-snb-t1 ' +
  'active:scale-[0.97] active:duration-press ' +
  'disabled:cursor-not-allowed disabled:opacity-60 motion-reduce:transform-none'

interface StateProps {
  title: string
  body: string
  /** 空态的按钮必须指向「能把这里填满的那个动作」，不许只写「暂无数据」 */
  actionLabel: string
  onAction: () => void
}

/** 空态：请求成功且结果为空才用它。判定与错误态不可互换 */
export function EmptyState({ title, body, actionLabel, onAction }: StateProps) {
  return (
    <UiEmptyState
      title={title}
      description={body}
      action={
        <button type="button" className={secondaryClass} onClick={onAction}>
          {actionLabel}
        </button>
      }
    />
  )
}

/** 错误态：请求失败/超时/解析失败一律走它，绝不假装「暂无数据」。
 *  文案三要素：哪件事失败了 / 责任在谁 / 用户的东西丢没丢。按钮必带「再试一次」 */
export function ErrorState({ title, body, actionLabel, onAction }: StateProps) {
  return <UiErrorState title={title} description={body} onRetry={onAction} retryLabel={actionLabel} />
}
