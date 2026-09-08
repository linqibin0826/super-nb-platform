/**
 * 苹果式 v3 防漂移守卫（批 3 Task 11 立约，2026-09-07）。
 *
 * 扫 `src/**` 的业务面：网吧 v2 的母题、色值、任意值圆角/动效、网吧文案一个都不许回流。
 * ⚠️ 排除三类：
 *   ① `src/ui/**` —— vendored 设计系统，由上游 super-nb-ui 的 no-wangba.test.ts 守；
 *      本仓要改它只能去源仓改完重新 vendor（README 的 rsync 命令）。
 *   ② `__tests__` —— 本文件的正则字面量里就写着旧色值。
 *   ③ 注释——先剥再扫（墓碑注释里写着旧类名/旧色值，直接扫会当成残留；Spec 2 实测踩过）。
 *
 * 🚨 Tailwind 找不到键只会**静默不生成类、不报错**——所以「类名写着、样式没有」是本仓
 *    最常见的隐形故障（.snb-glass 进不了 studio 就是同一类）。这支守卫是它的第一道网。
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
      // ⚠️ `name === 'ui'` 跳的是**任意深度**叫 ui 的目录。本仓当前只有 src/ui 一处（vendored），
      //    将来谁在业务面新建 `xxx/ui/` 子目录会被静默跳过——那天记得改成路径前缀判断。
      if (name === '__tests__' || name === 'node_modules' || name === 'ui') continue
      walk(p, acc)
    } else if (EXTS.some((e) => name.endsWith(e))) {
      acc.push(p)
    }
  }
  return acc
}

/** 剥注释再扫 */
const strip = (src: string) => src.replace(/\/\*[\s\S]*?\*\//g, '').replace(/^\s*\/\/.*$/gm, '')

/**
 * 具名白名单：这些字串**不是**网吧遗产，扫描前先摘掉。
 * - 「机房后勤簿」= 站长 2026-08-18 亲自定的内部台账产品名（同「义父群」「女秘书」的性质：
 *   专名，早于/独立于网吧化），且 ops-admin 是内部工具不面向客户。要改先问站长。
 */
const ALLOWED: RegExp[] = [/机房后勤簿/g]

const files = walk(SRC).map((path) => ({
  path: path.slice(SRC.length + 1),
  body: ALLOWED.reduce((s, re) => s.replace(re, ''), strip(readFileSync(path, 'utf8'))),
}))

/**
 * 发票**票面像素**是有意保留的旧色系（spec 决策 10）：朱砂 / 票号红 / 票框棕红 / 仿宋。
 * 🚨 例外只给两个具名文件，不是 `invoice/` 整目录——外壳（GuestGate / HomeBar / App /
 *    FirstVisitGuide / pages/*）与其他业务面同等零容忍（原来整目录放行，`GuestGate.tsx`
 *    里两处 🪦 纸米白 rgba(239,235,228,.28) 就这样躲了过去）。
 * ⚠️ 例外只覆盖**颜色与字体**：动效值（缓动/时长）不在其中，缓动那条断言对 invoice 一视同仁。
 */
const TICKET_FILES = new Set(['invoice/invoice.css', 'invoice/pages/shared.tsx'])
const isInvoiceTicket = (p: string) => TICKET_FILES.has(p)
/** 画墙信息层的实压阴影（CC BY 署名压任意图的可读性设施，不是辉光） */
const isWallCard = (p: string) => p === 'studio/WallCard.tsx'

function offenders(re: RegExp, skip: (p: string) => boolean = () => false): string[] {
  return files
    .filter((f) => !skip(f.path) && re.test(f.body))
    .map((f) => `${f.path} → ${f.body.match(re)![0]}`)
}

describe('苹果式 v3 防漂移 · 网吧母题零回流（业务面，vendored 由上游守）', () => {
  it('🪦 网吧文案零命中（中英两套一起退）', () => {
    expect(offenders(/我的机位|画图机位|今日活动|杂志架|开卡上机|开卡|上机卡|上机|机位|网费|网管|机房|开灯|关灯/)).toEqual([])
    expect(offenders(/My Station|Art Station|Magazine Rack|Open a Card|Today's Events/)).toEqual([])
  })

  it('🪦 网吧 v2 色值零命中（票据本体除外）', () => {
    const HEX = /#(BA4400|FF5C00|FFA372|0E1014|171A20|242A33|F2EEE6|FBF9F5|E5DFD3|EAE5DB|CFC8B8|C9C2B4|EFEBE4|1C1A16|D9A35C|141821|1B1F27|2C333D|3A424E|221E18|14120F|100F0D|1B1710|BDB4A9|8F877D|D6CCBE|7E766B|6F6B62|5A5750|3A362E|CACFD5|1a1613|0F0E0C|D8D3CA|828B96|86837C|A08876|6B5749)\b/i
    const RGB = /(?:239[,\s]+235[,\s]+228)|(?:186[,\s]+68[,\s]+0)|(?:255[,\s]+92[,\s]+0)|(?:14[,\s]+16[,\s]+20)|(?:28[,\s]+26[,\s]+22)|(?:36[,\s]+42[,\s]+51)/
    expect(offenders(HEX, isInvoiceTicket)).toEqual([])
    expect(offenders(RGB, isInvoiceTicket)).toEqual([])
  })

  it('🪦 发票 invoice.css 里的**外壳色**不许躲在票据例外后面（熔铁橙 / 纸米白 / 沥青都不是票据色）', () => {
    // 票据例外放行整份 invoice.css（票框棕红 #97503c、黄铜 #d9a35c 住在里面），
    // 但 #BA4400 / #FF5C00 / #EFEBE4 / #0E1014 这些是网吧外壳色，与票面无关，得单独钉死。
    const css = readFileSync(resolve(SRC, 'invoice/invoice.css'), 'utf8')
    expect(strip(css)).not.toMatch(/#(BA4400|FF5C00|FFA372|EFEBE4|F2EEE6|0E1014|242A33|1C1A16)\b/i)
  })

  it('🪦 键帽底边 / 招牌字 / 噪点零命中', () => {
    expect(offenders(/shadow-edge-[123]|shadow-key(-down)?\b|border-b-snb-key-side|border-b-\[3px\]/)).toEqual([])
    expect(offenders(/font-sign|Jost|jost-latin/)).toEqual([])
    expect(offenders(/snb-air|feTurbulence/)).toEqual([])
  })

  it('圆角只用五档：任意值 rounded-[Npx] 零命中（rounded-[3px] 图形微件是唯一例外）', () => {
    // rounded-[3px] = RatioIcon / SpecPanel 的 13px 比例图示描边，不是容器圆角
    expect(offenders(/rounded-\[(?!3px\])[^\]]+\]/)).toEqual([])
  })

  it('动效只用四档 + 两把缓动：任意值时长与网吧缓动零命中', () => {
    expect(offenders(/duration-\[\d+ms\]/)).toEqual([])
    expect(offenders(/cubic-bezier\(0\.2,\s*0,\s*0,\s*1\)/)).toEqual([])
    expect(offenders(/duration-(150|200|300)\b/)).toEqual([])
  })

  it('数据不闪：animate-pulse 零命中（骨架静止、进行中走绿在线点扩散环）', () => {
    expect(offenders(/animate-pulse/)).toEqual([])
  })

  it('🚨 零发光：text-shadow 只许画墙信息层；backdrop-filter 业务面零命中（玻璃走 .snb-glass 类）', () => {
    expect(offenders(/text-shadow/, isWallCard)).toEqual([])
    expect(offenders(/backdrop-filter|backdrop-blur/)).toEqual([])
  })

  it('焦点环走语义槽：ring-paper 零命中（paper 是浅色定值，浅色档下等于隐形）', () => {
    expect(offenders(/ring-paper/)).toEqual([])
  })

  it('🚨 studio 入口确实吃到了 vendored 的组件层（.snb-glass 不生成 = 玻璃静默失效）', () => {
    const indexCss = readFileSync(resolve(SRC, 'index.css'), 'utf8')
    expect(indexCss).toContain("@import './ui/styles.css'")
    // ⚠️ 必须先剥注释（本文件开头规则 ③）：index.css 的抬头注释里就写着「本文件不得再写
    //    @tailwind」这句纪律，裸 toContain 会把这句**说明**当成违规，守卫第一次跑就假红。
    expect(strip(indexCss)).not.toContain('@tailwind') // 否则整套工具类会打两遍
  })

  it('🚨 旧体系清零守卫的分期清单已经空了（Task 6–9 的收口债还清）', () => {
    const neon = readFileSync(resolve(SRC, '__tests__/neon-no-legacy.spec.ts'), 'utf8')
    // ⚠️ 预检：这里锁的是**源码字面量**，正则必须容忍空格与换行——写死成
    //    `new Set<string>([])` 一串，格式化工具换个行就假红（守卫本身变成漂移源）。
    expect(neon).toMatch(/PENDING\s*=\s*new Set<string>\(\s*\[\s*\]\s*\)/)
  })
})
