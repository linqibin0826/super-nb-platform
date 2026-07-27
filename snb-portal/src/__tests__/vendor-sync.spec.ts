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

  it('恒暗：tokens 的 :root 与 .dark 合并声明，浅色块退役', () => {
    expect(tokens).toMatch(/:root,\s*\n?\s*\.dark\s*\{/)
    // 合并声明后同一个变量只该出现一次；出现两次说明浅色块还在
    expect((tokens.match(/--snb-bg:/g) ?? []).length).toBe(1)
  })

  it('v2 槽位就位：沥青 #0E1014、双强调 safety/danger、灯管槽位清零', () => {
    expect(tokens).toContain('--snb-bg: 14 16 20')
    expect(tokens).toContain('--snb-safety: 255 92 0')
    expect(tokens).toContain('--snb-danger: 229 72 77')
    expect(tokens).not.toMatch(/--snb-(tangerine|jade|fuchsia|core)/)
  })

  it('中性回归灰阶：t2 不再是锈色（红分量不得明显高于蓝）', () => {
    const v = tokens.match(/--snb-t2:\s*(\d+) (\d+) (\d+);/)
    expect(v).not.toBeNull()
    expect(Number(v![3])).toBeGreaterThanOrEqual(Number(v![1]))
  })

  it('ThemeSwitch 已从 vendor 出口移除（恒暗，留任何入口都可能切回浅色）', () => {
    expect(uiIndex).not.toMatch(/ThemeSwitch/)
  })
})
