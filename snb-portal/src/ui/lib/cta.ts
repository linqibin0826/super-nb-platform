/** 链接版按钮配方——给 `<a>` 用的三条类串（值与 Button.tsx 的 primary / secondary / ghost 逐字同源，
 *  改任何一态必须两边同步）。三条都不含横向内边距与宽度：普通场景补 `px-[22px]`
 *  （手机顶栏内 `px-4`），整宽场景补 `w-full`。
 *
 *  🚨 焦点段 `focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus` 三条都要有
 *     ——它在 Button.tsx 里挂在 `base` 上，链接版没有 base，漏了就是「按钮有焦点环、同一枚
 *     CTA 换成 <a> 就没有」。填充版（ctaAnchorClass）另加 `ring-offset-2 ring-offset-snb-bg`：
 *     环色与填充同为强调橙，不隔一圈页面底就是 1:1 隐形（同 Button 的 ctaFourStates）。
 *     描边款 / 幽灵不加 offset——底本来就是页面底。
 *     同源比对由 Button.test.tsx「链接版 CTA 配方与 Button primary 逐字同源」守着。 */

/** 主 CTA：胶囊 / 高 44 / 15px semibold / 强调色填充；hover 加深、press 缩 .97。 */
export const ctaAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-full bg-snb-cta text-[15px] font-semibold text-snb-cta-fg no-underline transition-[background-color,transform] duration-quick ease-snb-quick focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus focus-visible:ring-offset-2 focus-visible:ring-offset-snb-bg hover:bg-snb-cta-hover active:bg-snb-cta-press active:scale-[0.97] active:duration-press'

/** 次按钮：白胶囊，panel 底 + hairline-strong 描边 + 卡片投影。 */
export const secondaryAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-full border border-snb-hairline-strong bg-snb-panel text-[15px] font-medium text-snb-t1 no-underline shadow-card transition-[border-color,transform] duration-quick ease-snb-quick focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus hover:border-snb-hairline-heavy active:scale-[0.97] active:duration-press'

/** 幽灵：无边 t2 字，hover 提 t1 + 6% 底。 */
export const ghostAnchorClass =
  'inline-flex h-11 items-center justify-center rounded-full bg-transparent text-[15px] font-medium text-snb-t2 no-underline transition-[background-color,color,transform] duration-quick ease-snb-quick focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus hover:bg-snb-t1/[0.06] hover:text-snb-t1 active:scale-[0.97] active:duration-press'
