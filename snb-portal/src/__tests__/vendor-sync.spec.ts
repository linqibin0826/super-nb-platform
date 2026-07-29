/**
 * vendor 同步断言（Spec 3 Task 1 立约；2026-07-28 升零发光网吧 v2 口径）。
 *
 * 🚨 为什么需要：`src/ui/` 是 `super-nb-ui` 的**手工全量拷贝**，package.json 里
 *    没有 @super-nb/ui 依赖。只改源仓不重新 vendor，四站会**静默停留在旧皮肤且
 *    不报错**——AppHeader.tsx 曾这样漂移 13 天无人发现；2026-07-28 清点时又实测
 *    到一次（源仓翻 v2 后 vendored 副本还披着霓虹，当天重新 vendor 拉齐）。
 *
 * 本文件只能断言「vendor 侧已经是新的」，不能保证「源仓改了就同步」。
 * 真正的防线是把 super-nb-ui 发成私有 npm 包，超出本次范围。
 */
import { describe, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
// @ts-expect-error preset 是无类型声明的 .js，此处只读值不需要类型
import preset from '../../tailwind-preset.js'

// ⚠️ 读 CSS 必须用 node:fs，不能用 vite 的 `?raw`——vitest 默认 css:false，
//    CSS 导入（含 ?raw）被 stub 成空字符串，断言会拿到 '' 而静默"通过成空"。
//    实测踩过：三条 tokens 断言全变成 expected '' to match。
//    （`?raw` 对 .ts 文件正常，只有 CSS 被 CSS 插件拦截。）
// ⚠️ 另：不要用 import.meta.url + fileURLToPath——vitest 下它不是 file: scheme。
const read = (p: string) => readFileSync(resolve(process.cwd(), p), 'utf8')
const tokens = read('src/ui/tokens/tokens.css')
const styles = read('src/ui/styles.css')
const uiIndex = read('src/ui/index.ts')

// 双档后按块取值：全文匹配会拿到**先出现的浅色档**，深色断言会静默测错对象。
// 2026-07-29 实测踩过：t2 的「不许是锈色」断言拿到白天档的暖墨 #5A5750 直接红。
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

describe('vendor 同步：设计系统已是零发光网吧 v2', () => {
  it('primary 是工业安全橙，赤陶橙与霓虹橙都退役', () => {
    expect(colors.primary['500']).toBe('#FF5C00')
    expect(JSON.stringify(colors)).not.toMatch(/CC785C|FF6B35/i)
  })

  it('🪦 三灯管与管芯清零：jade / magenta / core 键不复存在', () => {
    expect(colors.jade).toBeUndefined()
    expect(colors.magenta).toBeUndefined()
    expect(colors.core).toBeUndefined()
    expect(JSON.stringify(colors)).not.toMatch(/00E5C7|FF3DDB|FFF4EA/i)
  })

  it('主字纸米白：white 覆盖 #EFEBE4，沥青底偏青', () => {
    expect(colors.white).toBe('#EFEBE4')
    expect(colors.asphalt.DEFAULT).toBe('#0E1014')
  })

  it('既有消费方依赖的键不得删除（Tailwind 删键只静默不生成类、不报错）', () => {
    expect(colors.dark['900']).toBeDefined()
    expect(colors.paper.DEFAULT).toBeDefined()
    expect(colors.snb.ember).toBe('#E5484D') // CardStat/HistoryTab 在用；v2 功能红
    expect(colors.snb.amber).toBe('#FF5C00') // v2 色板没有黄：警告=安全橙
    expect(shadow.glass).toBeDefined()
    expect(shadow['glass-sm']).toBeDefined()
    // Composer 的呼吸点动画键；studio 若丢它，点会静止且不报错
    expect(extend.animation['snb-dot']).toBeDefined()
  })

  it('衬线 display 退役，招牌字体就位', () => {
    expect(fonts.display).toBeUndefined()
    expect(fonts.sign[0]).toBe('Jost')
  })

  it('🚨 零发光：辉光 shadow / 点火动画 / 霓虹渐变全部退役', () => {
    expect(shadow.tube).toBeUndefined()
    expect(shadow.glow).toBeUndefined()
    expect(shadow['glow-jade']).toBeUndefined()
    expect(extend.keyframes.tubeOn).toBeUndefined()
    expect(extend.backgroundImage).toBeUndefined()
    expect(styles).not.toMatch(/text-shadow:/)
  })

  it('双档：浅色进 :root/.snb-light、深色进 .dark，且 .dark 必须写在后面', () => {
    // 2026-07-29 双档补全。恒暗期是 `:root, .dark` 合并声明（分期上线第一步的形态），
    // 现在必须是两个独立块。
    expect(tokens).toMatch(/:root,\s*\n?\s*\.snb-light\s*\{/)
    expect(tokens).toMatch(/\n\.dark\s*\{/)
    // 每个槽位两档各一份；只剩一份说明某一档漏了
    expect((tokens.match(/--snb-bg:/g) ?? []).length).toBe(2)
    // 🚨 顺序铁律：两个选择器特异度同为 (0,1,0)，谁在后谁赢。
    //    浅色块被挪到 .dark 之后 = 深色档整片失效（且不报错）。
    expect(tokens.indexOf('\n.dark {')).toBeGreaterThan(tokens.indexOf('.snb-light {'))
  })

  it('🚨 浅色档三条不可选结论就位（照抄定稿，不许自己调）', () => {
    // ① 主按钮浅色是**墨块**不是纸白：浅色里最响的一块只能是最黑的一块
    expect(lightBlock).toContain('--snb-cta-bg: 28 26 22')
    // ② 安全橙压深到 #BA4400：原 #FF5C00 压纸仅 2.7:1，连大字 3:1 都够不着
    expect(lightBlock).toContain('--snb-safety: 186 68 0')
    // ③ 键帽黑底边换实色暖灰侧壁（读作侧壁，不是影子）
    expect(lightBlock).toContain('--snb-key-side: #C9C2B4')
    // 遮罩两档都是暗的——变白会让底下页面看着像没关掉
    expect(lightBlock).toMatch(/--snb-mask:\s*rgba\(28, 26, 22/)
  })

  it('v2 槽位就位：沥青 #0E1014、双强调 safety/danger、灯管槽位清零', () => {
    expect(darkBlock).toContain('--snb-bg: 14 16 20')
    expect(darkBlock).toContain('--snb-safety: 255 92 0')
    expect(darkBlock).toContain('--snb-danger: 229 72 77')
    expect(tokens).not.toMatch(/--snb-(tangerine|jade|fuchsia|core)/)
  })

  it('中性回归灰阶：深色 t2 不再是锈色（红分量不得明显高于蓝）', () => {
    // ⚠️ 必须在**深色块内**取值：双档后 tokens 里有两个 --snb-t2，
    // 直接全文匹配拿到的是浅色档的 #5A5750（暖墨，红本来就高于蓝，属白天档定稿）。
    const v = darkBlock.match(/--snb-t2:\s*(\d+) (\d+) (\d+);/)
    expect(v).not.toBeNull()
    expect(Number(v![3])).toBeGreaterThanOrEqual(Number(v![1]))
  })

  it('主题开关件与契约都在 vendor 出口里（双档补全后，缺任一样下游就切不了档）', () => {
    expect(uiIndex).toMatch(/ThemeToggle/)
    expect(uiIndex).toMatch(/THEME_BOOT_SNIPPET/)
    expect(uiIndex).toMatch(/initTheme/)
  })
})
