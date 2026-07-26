/**
 * 港风霓虹换装的「旧体系清零」断言（Spec 3 Task 6）。
 *
 * 这套检查在 Spec 2（主站 fork）里**两次**抓出人工遍历漏掉的东西——整套没换的
 * onboarding.css、骨架屏渐变里的旧墨色。本批范围更大（四站 12000+ 行 + vendor），
 * 人眼一定会漏，得靠断言兜。
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
 * 这不是漏改，是设计决策——全量霓虹化会让用户怀疑发票真伪，属伤业务而非伤审美。
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

  it('Hero 五层辉光每个文件至多一处（辉光三档混用即层级塌陷）', () => {
    // ⚠️ 不能用 \bsnb-tube\b：`-` 是非单词字符，会把青玉档 snb-tube-2 一起算进来
    //    （Spec 2 实测误报过）。(?![\w-]) 才是「恰好是 snb-tube 这个类名」。
    const over = files
      .map((f) => ({
        file: f.path,
        hits: (f.body.match(/["'\s]snb-tube(?![\w-])/g) ?? []).length,
      }))
      .filter((x) => x.hits > 1)
    expect(over).toEqual([])
  })
})
