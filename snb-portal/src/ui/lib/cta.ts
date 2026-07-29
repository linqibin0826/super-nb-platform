/** 链接版按钮配方（GlobalParts v3 §01/§03）——给 `<a>` 用的三条类串。
 *  React 站点点击类动作请直接用 <Button>；本文件只解决「必须是真链接」的场合
 *  （顶栏 CTA / 浮卡底部 / GuestGate 整宽 CTA），值与 Button.tsx 的
 *  primary / secondary / ghost 逐字同源，**改任何一态必须两边同步**。
 *
 *  ⚠️ 三条都不含横向内边距与宽度：
 *    - 普通场景补 `px-5`（20；手机顶栏内 `px-4`=16）
 *    - 整宽场景（浮卡底部 / GuestGate）补 `w-full`，不加 px
 */

/** 主 CTA：高 44 / 圆角 8 / 14px semibold / 底边 0 2px 0 --snb-edge。
 *  🚨 双档镜像：**深夜 = 纸白底沥青字，白天 = 墨块底纸白字**——两档各只有一块最响的，
 *  用户换档不用重新学。hover 深色提亮 / 浅色加深（白天里「更用力」等于更黑），
 *  active 沉 1px 走 55ms。四态值全在 --snb-cta-* 里翻，本文件不写死任何颜色。 */
export const ctaAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] bg-snb-cta text-sm font-semibold text-snb-cta-fg no-underline shadow-edge-2 transition-all duration-quick ease-snb hover:-translate-y-px hover:bg-snb-cta-hover hover:shadow-edge-3 active:translate-y-px active:bg-snb-cta-press active:shadow-edge-1 active:duration-press'

/** 次按钮：hairline-strong 描边 + 主字色，hover 底 panel、边提到 heavy 档。 */
export const secondaryAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] border border-snb-hairline-strong bg-transparent text-sm text-snb-t1 no-underline transition-all duration-quick ease-snb hover:border-snb-hairline-heavy hover:bg-snb-panel active:translate-y-px active:duration-press'

/** 幽灵：无边框 dim 字，hover 提主字色 + 6% 主字色底（顶栏「登录」即此款）。 */
export const ghostAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-[8px] bg-transparent text-sm text-snb-t2 no-underline transition-all duration-quick ease-snb hover:bg-snb-t1/[0.06] hover:text-snb-t1 active:translate-y-px active:duration-press'
