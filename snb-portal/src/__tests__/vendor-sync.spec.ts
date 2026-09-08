/**
 * vendor 同步断言（Spec 3 Task 1 立约；2026-09-07 升苹果式 v3 口径）。
 *
 * 🚨 为什么需要：`src/ui/` 是 `super-nb-ui` 的**手工全量拷贝**，package.json 里
 *    没有 @super-nb/ui 依赖。只改源仓不重新 vendor，五站会**静默停留在旧皮肤且
 *    不报错**——AppHeader.tsx 曾这样漂移 13 天无人发现；2026-07-28 清点时又实测
 *    到一次（源仓翻网吧 v2 后 vendored 副本还披着霓虹）。raffle-admin 与 ops-admin
 *    历史上被整站漏过（spec §8.4）。
 *
 * 本文件只能断言「vendor 侧已经是新的」，不能保证「源仓改了就同步」。
 * 真正的防线是把 super-nb-ui 发成私有 npm 包，超出本次范围。
 */
import { describe, expect, it } from 'vitest'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'
// @ts-expect-error preset 是无类型声明的 .js，此处只读值不需要类型
import preset from '../../tailwind-preset.js'

// ⚠️ 读 CSS 必须用 node:fs，不能用 vite 的 `?raw`——vitest 默认 css:false，
//    CSS 导入（含 ?raw）被 stub 成空字符串，断言会拿到 '' 而静默"通过成空"。
// ⚠️ 另：不要用 import.meta.url + fileURLToPath——vitest 下它不是 file: scheme。
const read = (p: string) => readFileSync(resolve(process.cwd(), p), 'utf8')
const tokens = read('src/ui/tokens/tokens.css')
const styles = read('src/ui/styles.css')
const uiIndex = read('src/ui/index.ts')
const appHeader = read('src/ui/components/AppHeader/AppHeader.tsx')
const navSrc = read('src/ui/components/AppHeader/nav.tsx')
const ctaSrc = read('src/ui/lib/cta.ts')
const masonry = read('src/ui/components/MasonryCard/MasonryCard.tsx')

// 双档后按块取值：全文匹配会拿到**先出现的浅色档**，深色断言会静默测错对象。
const sliceBlock = (start: RegExp) => {
  const i = tokens.search(start)
  if (i < 0) throw new Error(`tokens.css 里找不到 ${start}`)
  const from = tokens.indexOf('{', i)
  return tokens.slice(from, tokens.indexOf('\n}', from))
}
const lightBlock = sliceBlock(/^:root,\s*\n\.snb-light\s*\{/m)
const darkBlock = sliceBlock(/^\.dark\s*\{/m)

const extend = (preset as Record<string, any>).theme.extend
const colors = extend.colors
const shadow = extend.boxShadow
const fonts = extend.fontFamily
const radius = extend.borderRadius
const json = JSON.stringify(preset)

describe('vendor 同步：设计系统已是苹果式 v3', () => {
  it('primary 三阶=赤陶橙：400 深色强调 / 500 品牌原色 / 600 浅色强调；网吧橙与霓虹橙清零', () => {
    expect(colors.primary['400']).toBe('#E08F72')
    expect(colors.primary['500']).toBe('#CC785C')
    expect(colors.primary['600']).toBe('#A85A3F')
    expect(json).not.toMatch(/FF5C00|BA4400|FF6B35/i)
  })

  it('🪦 三灯管与管芯清零：jade / magenta / core 键不复存在', () => {
    expect(colors.jade).toBeUndefined()
    expect(colors.magenta).toBeUndefined()
    expect(colors.core).toBeUndefined()
    expect(JSON.stringify(colors)).not.toMatch(/00E5C7|FF3DDB|FFF4EA/i)
  })

  it('🪦 纸米白 white 与沥青 asphalt 退役：white 回纯白、asphalt 回纯黑', () => {
    expect(colors.white).toBe('#FFFFFF')
    expect(colors.asphalt.DEFAULT).toBe('#000000')
    expect(colors.paper.DEFAULT).toBe('#F5F5F7')
    expect(json).not.toMatch(/EFEBE4|0E1014|242A33|F2EEE6/i)
  })

  it('既有消费方依赖的键不得删除（Tailwind 删键只静默不生成类、不报错）', () => {
    expect(colors.dark['900']).toBeDefined()
    expect(colors.paper.DEFAULT).toBeDefined()
    // 🪦 批 1 终审波起，amber / ember 两枚旧语义键从定值改指语义槽、随档翻。
    //    保名是硬要求：studio CardStat / HistoryTab 与 Alert 直接写着 snb-ember / snb-amber，
    //    删键 Tailwind 只静默不生成类。
    expect(colors.snb.ember).toBe('rgb(var(--snb-danger) / <alpha-value>)')
    expect(colors.snb.amber).toBe('rgb(var(--snb-safety) / <alpha-value>)')
    expect(shadow.glass).toBeDefined()
    expect(shadow['glass-sm']).toBeDefined()
    // Composer 的呼吸点动画键；studio 若丢它，点会静止且不报错
    expect(extend.animation['snb-dot']).toBeDefined()
    // 本仓 Task 6/10 的浮层物化要用
    expect(extend.animation['snb-materialize']).toBeDefined()
  })

  it('🪦 招牌字 Jost 退役：sign 回系统栈，字体文件随 vendor 一起删掉了', () => {
    expect(fonts.display).toBeUndefined()
    expect(fonts.sans[0]).toBe('-apple-system')
    expect(fonts.sign).toEqual(fonts.sans)
    expect(json).not.toMatch(/Jost|Futura/)
    expect(styles).not.toContain('@font-face')
    expect(existsSync(resolve(process.cwd(), 'src/ui/assets/fonts/jost-latin-var.woff2'))).toBe(false)
  })

  it('🚨 零发光：辉光 shadow / 点火动画 / 噪点 .snb-air 全部退役', () => {
    expect(shadow.tube).toBeUndefined()
    expect(shadow.glow).toBeUndefined()
    expect(shadow['glow-jade']).toBeUndefined()
    expect(extend.keyframes.tubeOn).toBeUndefined()
    expect(extend.backgroundImage).toBeUndefined()
    expect(styles).not.toMatch(/text-shadow:/)
    expect(styles).not.toContain('.snb-air')
  })

  it('双档：浅色进 :root/.snb-light、深色进 .dark，且 .dark 必须写在后面', () => {
    expect(tokens).toMatch(/:root,\s*\n?\s*\.snb-light\s*\{/)
    expect(tokens).toMatch(/\n\.dark\s*\{/)
    expect((tokens.match(/--snb-bg:/g) ?? []).length).toBe(2)
    // 🚨 顺序铁律：两个选择器特异度同为 (0,1,0)，谁在后谁赢
    expect(tokens.indexOf('\n.dark {')).toBeGreaterThan(tokens.indexOf('.snb-light {'))
  })

  it('🚨 浅色档三条不可选结论就位（照批 1 定稿，不许自己调）', () => {
    // ① 底是冷灰白 #F5F5F7，绝不是暖纸
    expect(lightBlock).toContain('--snb-bg: 245 245 247')
    expect(lightBlock).toContain('--snb-panel: 255 255 255')
    // ② 唯一强调色=赤陶橙压深 #A85A3F（白底 5.0:1）
    expect(lightBlock).toContain('--snb-safety: 168 90 63')
    expect(lightBlock).toContain('--snb-cta-bg: 168 90 63')
    expect(lightBlock).toContain('--snb-cta-fg: 255 255 255')
    // ③ 遮罩两档都是暗的
    expect(lightBlock).toMatch(/--snb-mask:\s*rgba\(0, 0, 0, 0\.40\)/)
  })

  it('深色档：纯黑底 + #1D1D1F 面 + 提亮赤陶橙 #E08F72', () => {
    expect(darkBlock).toContain('--snb-bg: 0 0 0')
    expect(darkBlock).toContain('--snb-panel: 29 29 31')
    expect(darkBlock).toContain('--snb-safety: 224 143 114')
    expect(darkBlock).toContain('--snb-danger: 255 69 58')
    expect(tokens).not.toMatch(/--snb-(tangerine|jade|fuchsia|core)/)
  })

  it('新增槽位随 vendor 进来：brand / live / live-ink / safety-ink / danger-ink / t4 / 圆角五档 / 玻璃配方', () => {
    for (const b of [lightBlock, darkBlock]) {
      for (const k of ['brand', 'live', 'live-ink', 'safety-ink', 'danger-ink', 't4']) {
        expect(b, `缺 --snb-${k}`).toContain(`--snb-${k}:`)
      }
    }
    // 终审波压深：浅色绿字压白 6.1:1（旧值 27 127 52 只有 4.7:1，压 well 不达标）
    expect(lightBlock).toContain('--snb-live-ink: 23 113 46')
    expect(tokens).toContain('--snb-glass-filter: saturate(180%) blur(20px)')
    expect(tokens).toContain('--snb-r-pill: 999px')
    expect(tokens).toContain('--snb-r-card: 18px')
    expect(tokens).toContain('--snb-ease: cubic-bezier(0.32, 0.72, 0, 1)')
    expect(tokens).toContain('--snb-ease-quick: cubic-bezier(0.25, 1, 0.5, 1)')
  })

  it('玻璃工具类与三条无障碍媒体查询随 vendor 进来（studio 靠 index.css @import 吃到）', () => {
    expect(styles).toContain('.snb-glass {')
    expect(styles).toContain('.snb-glass-side {')
    expect(styles).toContain('.snb-glass-chip {')
    expect(styles).toContain('backdrop-filter: var(--snb-glass-filter)')
    expect(styles).toContain('prefers-reduced-motion: reduce')
    expect(styles).toContain('prefers-reduced-transparency: reduce')
    expect(styles).toContain('prefers-contrast: more')
    // 🚨 studio 的 index.css 必须 @import 它，否则 .snb-glass 不生成、顶栏静默变透明
    expect(read('src/index.css')).toContain("@import './ui/styles.css'")
  })

  it('preset 圆角五档与动效四档就位（本仓类名直接映射它们）', () => {
    expect(radius.chip).toBe('var(--snb-r-chip)')
    expect(radius.lg).toBe('var(--snb-r-thumb)')
    expect(radius.xl).toBe('var(--snb-r-sheet)')
    expect(radius['2xl']).toBe('var(--snb-r-card)')
    expect(radius['3xl']).toBe('var(--snb-r-panel)')
    expect(radius['4xl']).toBe('var(--snb-r-hero)')
    const dur = extend.transitionDuration
    expect([dur.press, dur.quick, dur.settle, dur.breath]).toEqual(['100ms', '200ms', '400ms', '2200ms'])
    const ease = extend.transitionTimingFunction
    expect(ease.snb).toBe('cubic-bezier(0.32, 0.72, 0, 1)')
    expect(ease['snb-quick']).toBe('cubic-bezier(0.25, 1, 0.5, 1)')
    expect(json).not.toContain('cubic-bezier(0.2, 0, 0, 1)')
  })

  it('批 5：浮卡厚材质 .snb-glass-sheet 就位（.92 近实底、不挂 backdrop-filter）', () => {
    // 🚨 浮卡是带毛玻璃顶栏的后代，子层再滤是空转；且它压整屏正文，.72 会把底下的字透上来
    expect(styles).toContain('.snb-glass-sheet { background: rgba(255, 255, 255, 0.92); }')
    expect(styles).toContain('.dark .snb-glass-sheet { background: rgba(29, 29, 31, 0.92); }')
    // AppHeader 的 <lg 浮卡必须用它，不能退回顶栏那枚 .snb-glass
    expect(appHeader).toContain('snb-glass-sheet absolute inset-x-0 top-full')
    // 高对比档：面变实的同时边也要抬，两档分开写
    expect(styles).toMatch(/\.snb-glass-sheet \{[^}]*border-color: rgba\(0, 0, 0, 0\.35\)/)
    expect(styles).toMatch(/\.dark \.snb-glass-sheet \{[^}]*border-color: rgba\(255, 255, 255, 0\.35\)/)
  })

  it('批 5：顶栏 ≥lg 三列 grid 真居中（<lg 仍是 flex 两端对齐，不许整条 grid）', () => {
    expect(appHeader).toContain('lg:grid lg:grid-cols-[1fr_auto_1fr]')
    expect(appHeader).toContain('lg:justify-self-center')
    // 场景槽不显式 justify-self-end 会被拉满右列、账户区整体左移
    expect(appHeader).toContain('lg:justify-self-end')
    expect(appHeader).toContain('lg:justify-self-start')
  })

  it('批 5：「新手指南」nav key = help（写成 hub 会让内容中心误点亮、真在 help 上反而不亮）', () => {
    expect(navSrc).toMatch(/key: 'help',\s*\n\s*label: '新手指南',\s*\n\s*href: 'https:\/\/help\.super-nb\.me\//)
    expect(navSrc).not.toMatch(/key: 'hub'/)
  })

  it('批 5：焦点环补齐——填充变体垫 ring-offset，链接版三条都有 ring-2', () => {
    // 环色与填充同为强调橙，不隔一圈页面底就是 1:1 隐形（WCAG 2.4.11）
    expect(ctaSrc).toMatch(/ctaAnchorClass =\s*\n?\s*'[^']*focus-visible:ring-2 focus-visible:ring-snb-focus focus-visible:ring-offset-2 focus-visible:ring-offset-snb-bg/)
    for (const name of ['secondaryAnchorClass', 'ghostAnchorClass']) {
      const body = ctaSrc.slice(ctaSrc.indexOf(`${name} =`))
      expect(body.slice(0, body.indexOf("'\n")), name).toContain('focus-visible:ring-snb-focus')
    }
    // 描边款 / 幽灵不加 offset——底本来就是页面底，垫一圈只会凭空多一道白边
    const secondary = ctaSrc.slice(ctaSrc.indexOf('secondaryAnchorClass ='))
    expect(secondary.slice(0, secondary.indexOf("'\n"))).not.toContain('ring-offset')
  })

  it('批 5：MasonryCard 图上遮罩改纯黑（网吧墨底 rgb 写法躲得过 hex 断言）', () => {
    expect(masonry).toContain('rgba(0,0,0,0.97)_100%')
    expect(masonry).not.toMatch(/rgba\(14, ?16, ?20/)
    expect(masonry).not.toMatch(/0[Ee]1014/)
  })

  it('主题开关件与契约都在 vendor 出口里（缺任一样下游就切不了档）', () => {
    expect(uiIndex).toMatch(/ThemeToggle/)
    expect(uiIndex).toMatch(/THEME_BOOT_SNIPPET/)
    expect(uiIndex).toMatch(/initTheme/)
    // 链接版三条配方：本仓 studio/parts.tsx 直接再导出
    expect(uiIndex).toMatch(/ctaAnchorClass/)
    expect(uiIndex).toMatch(/secondaryAnchorClass/)
    expect(uiIndex).toMatch(/ghostAnchorClass/)
  })
})
