/**
 * vendor 同步断言（Spec 3 Task 1）。
 *
 * 🚨 为什么需要：`src/ui/` 是 `super-nb-ui` 的**手工全量拷贝**，package.json 里
 *    没有 @super-nb/ui 依赖。只改源仓不重新 vendor，四站会**静默停留在旧皮肤且
 *    不报错**——AppHeader.tsx 曾这样漂移 13 天无人发现。
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
const uiIndex = read('src/ui/index.ts')

const extend = (preset as Record<string, any>).theme.extend
const colors = extend.colors
const shadow = extend.boxShadow
const fonts = extend.fontFamily

describe('vendor 同步：设计系统已是港风霓虹', () => {
  it('primary 是霓虹橙，赤陶橙退役', () => {
    expect(colors.primary['500']).toBe('#FF6B35')
    expect(JSON.stringify(colors)).not.toMatch(/CC785C/i)
  })

  it('三灯色齐备：橙=钱 / 青玉=量 / 品红=仪式', () => {
    expect(colors.jade['500']).toBe('#00E5C7')
    expect(colors.magenta['500']).toBe('#FF3DDB')
    expect(colors.core).toBe('#FFF4EA')
  })

  it('不得覆盖 Tailwind 内置色名（点缀灯用 magenta 不用 fuchsia）', () => {
    expect(colors.fuchsia).toBeUndefined()
  })

  it('既有消费方依赖的键不得删除（Tailwind 删键只静默不生成类、不报错）', () => {
    expect(colors.dark['900']).toBeDefined()
    expect(colors.paper.DEFAULT).toBeDefined()
    expect(colors.snb.ember).toBeDefined()
    expect(shadow.glass).toBeDefined()
    expect(shadow['glass-sm']).toBeDefined()
  })

  it('衬线 display 退役，招牌字体就位', () => {
    expect(fonts.display).toBeUndefined()
    expect(fonts.sign[0]).toBe('Jost')
  })

  it('发光纪律②：tube 五层衰减且半径严格递增', () => {
    const r = [...String(shadow.tube).matchAll(/0 0 (\d+)px/g)].map((m) => Number(m[1]))
    expect(r).toHaveLength(5)
    expect(r).toEqual([...r].sort((a, b) => a - b))
    expect(new Set(r).size).toBe(5)
  })

  it('辉光分级不塌陷：次级最大半径 < Hero 最大半径', () => {
    const max = (s: string) => Math.max(...[...s.matchAll(/0 0 (\d+)px/g)].map((m) => Number(m[1])))
    expect(max(String(shadow.glow))).toBeLessThan(max(String(shadow.tube)))
    expect(max(String(shadow['glow-jade']))).toBeLessThan(max(String(shadow.tube)))
  })

  it('恒暗：tokens 的 :root 与 .dark 合并声明，浅色块退役', () => {
    expect(tokens).toMatch(/:root,\s*\n?\s*\.dark\s*\{/)
    // 合并声明后同一个变量只该出现一次；出现两次说明浅色块还在
    expect((tokens.match(/--snb-bg:/g) ?? []).length).toBe(1)
  })

  it('发光纪律③：底色偏青，蓝分量严格高于红与绿', () => {
    const v = tokens.match(/--snb-bg:\s*(\d+) (\d+) (\d+);/)
    expect(v).not.toBeNull()
    const [r, g, b] = [Number(v![1]), Number(v![2]), Number(v![3])]
    expect(b).toBeGreaterThan(r)
    expect(b).toBeGreaterThan(g)
  })

  it('发光纪律⑤：中性用锈不用灰（t2 红分量高出蓝 ≥30）', () => {
    const v = tokens.match(/--snb-t2:\s*(\d+) (\d+) (\d+);/)
    expect(v).not.toBeNull()
    expect(Number(v![1]) - Number(v![3])).toBeGreaterThanOrEqual(30)
  })

  it('ThemeSwitch 已从 vendor 出口移除（恒暗，留任何入口都可能切回浅色）', () => {
    expect(uiIndex).not.toMatch(/ThemeSwitch/)
  })
})
