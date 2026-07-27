/**
 * 「旧体系清零」断言（Spec 3 Task 6 立约；2026-07-28 升零发光网吧 v2 口径）。
 *
 * 这套检查在 Spec 2（主站 fork）里**多次**抓出人工遍历漏掉的东西——整套没换的
 * onboarding.css、骨架屏渐变里的旧墨色、rgb 写法躲过 hex 断言的 sticky 列。
 * 本批范围更大（四站 12000+ 行 + vendor），人眼一定会漏，得靠断言兜。
 *
 * 2026-07-28 起清两代：
 *   ① 霓虹之前的上一代（赤陶橙 / 墨色黄铜 / 衬线遗产 / 主题切换 / 旧导航文案）；
 *   ② 🪦 港风霓虹本代（三灯管 / 管芯霓虹纸 / 锈色 / 旧沥青 / 辉光 / 点火）——
 *      随 2026-07-27 霓虹整条退役升为全域清零，「每文件至多一处 tube」的
 *      分级纪律作废，现在是零。
 *
 * ⚠️ 扫描排除 __tests__（本文件的正则字面量里就写着旧色值）。
 */
import { describe, expect, it } from 'vitest'
import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, resolve } from 'node:path'

const SRC = resolve(process.cwd(), 'src')
const EXTS = ['.ts', '.tsx', '.css']

function walk(dir: string, acc: string[] = []): string[] {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) {
      if (name === '__tests__' || name === 'node_modules') continue
      walk(p, acc)
    } else if (EXTS.some((e) => name.endsWith(e))) {
      acc.push(p)
    }
  }
  return acc
}

/** 剥掉注释再扫——墓碑注释里写着旧类名/旧色值，直接扫会把它当成残留（Spec 2 实测踩过） */
function strip(src: string): string {
  return src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '')
}

const files = walk(SRC).map((path) => ({
  path: path.slice(SRC.length + 1),
  body: strip(readFileSync(path, 'utf8')),
}))

function offenders(re: RegExp, skip: (p: string) => boolean = () => false): string[] {
  return files.filter((f) => !skip(f.path) && re.test(f.body)).map((f) => f.path)
}

/**
 * 发票中心的票据本体是**有意保留**的旧色系（spec §7.1「外壳换、票据本体不换」）：
 * 朱砂 / 票号红 / 票据工具字色 / 仿宋 / 印章 SVG 的衬线。
 * 这不是漏改，是设计决策——全量换装会让用户怀疑发票真伪，属伤业务而非伤审美。
 */
const isInvoiceTicket = (p: string) => p.startsWith('invoice/')

describe('旧体系清零（换皮遗漏靠断言兜，人眼一定会漏）', () => {
  it('赤陶橙色板清零（发票票据本体除外）', () => {
    expect(offenders(/CC785C|B5634A|97503C|7A4231|204,\s*120,\s*92/i, isInvoiceTicket)).toEqual([])
  })

  it('墨色黄铜色板清零', () => {
    expect(offenders(/#ded6c9|#35302b|#262220|#8f8578|#c9beae|#f5efe6|#1c1917|#131110|D4AF6A/i)).toEqual([])
  })

  it('衬线 display 字体遗产清零（发票票据本体除外）', () => {
    expect(offenders(/font-display|Georgia|Songti|STSong/, isInvoiceTicket)).toEqual([])
  })

  it('主题切换能力清零（恒暗，留任何入口都可能把站点切回浅色）', () => {
    expect(offenders(/ThemeSwitch|toggleTheme|useTheme\b|writeThemeCookie|effectiveTheme|readThemeCookie/)).toEqual([])
  })

  it('顶栏一级导航旧文案清零（12+ 处手抄副本，漏一处就是漂移）', () => {
    // ⚠️ 只查导航语境的四个词。合规文本 / 第三方产品名 / 页面场景名不在此列，
    //    本仓库暂无此类用法，故可直接全量扫。
    expect(offenders(/控制台|创作工坊|内容中心/)).toEqual([])
  })

  it('🪦 港风霓虹三灯/管芯/霓虹纸清零（hex 与 rgb 逗号/空格双写法全钉）', () => {
    expect(offenders(/FF6B35|00E5C7|FF3DDB|FFF4EA|F3E7DA|FF9C6F|FFBE9F/i)).toEqual([])
    expect(offenders(/255,?\s+107,?\s+53|0,?\s+229,?\s+199|255,?\s+61,?\s+219/)).toEqual([])
  })

  it('🪦 锈色中性档与旧沥青阶清零（v2 中性回归灰阶，沥青换 #0E1014 系）', () => {
    expect(offenders(/A08876|6B5749/i)).toEqual([])
    expect(offenders(/#070910|#0D111A|#151A25|#252B38|#333A4A/i)).toEqual([])
  })

  it('🚨 零发光：辉光类/点火/灯色工具全域清零（曾是「每文件至多一处」，现在是零）', () => {
    expect(offenders(/snb-tube|snb-glow|tube-on|tubeOn/)).toEqual([])
    expect(offenders(/shadow-tube|shadow-glow|glow-jade/)).toEqual([])
    expect(offenders(/jade-\d|magenta-\d|bg-core|text-core/)).toEqual([])
  })

  it('🚨 已删 preset 键零引用（Tailwind 删键只静默不生成类：引用=样式凭空消失）', () => {
    // gradient-primary 实测踩过：TaskCard 进度条曾引用它，删键后条子直接隐形
    expect(offenders(/gradient-primary/)).toEqual([])
  })

  it('text-shadow 全域清零（唯二例外：画墙图片蒙层上的可读性投影，不是辉光）', () => {
    const isImageOverlay = (p: string) => p === 'studio/GalleryTab.tsx' || p === 'studio/FavoritesTab.tsx'
    expect(offenders(/text-shadow/, isImageOverlay)).toEqual([])
  })
})
