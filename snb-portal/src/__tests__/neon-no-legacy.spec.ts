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
 *
 * 🚨 2026-09-07 收窄（原来是 `p.startsWith('invoice/')` 整目录放行）：票面像素只住在
 *    两个文件里，其余 invoice 文件（GuestGate / HomeBar / App / FirstVisitGuide / pages/*）
 *    是**外壳**，与其他业务面同等零容忍。实测：票据色（#97503c / #d9a35c）与仿宋 / Songti
 *    字体栈只出现在下面这两个文件，收窄后衬线那条断言依然通过。
 */
const TICKET_FILES = new Set(['invoice/invoice.css', 'invoice/pages/shared.tsx'])
const isInvoiceTicket = (p: string) => TICKET_FILES.has(p)

/**
 * vendored 设计系统（`src/ui/**`）由**上游**的 `super-nb-ui/src/__tests__/no-wangba.test.ts`
 * 守，本仓不重复守：它里面合法地含有 `--snb-brand: 204 120 92`（#CC785C 品牌原色开了独立槽）、
 * 三处 `backdrop-filter`（.snb-glass / -side / -chip）、新导航四项文案。
 * 🚨 例外只给 `ui/`，业务面（studio/hub/invoice 外壳/两个 admin/auth/i18n）一律零容忍。
 */
const isVendored = (p: string) => p.startsWith('ui/')

describe('旧体系清零（换皮遗漏靠断言兜，人眼一定会漏）', () => {
  it('赤陶橙品牌原色只许住在 vendored 定义处（业务面零命中新老写法）', () => {
    // 2026-09-07 苹果式 v3：赤陶橙**回来了**（#CC785C = --snb-brand），
    // 这条从「清零」翻成「限区」——只许出现在 vendored 的两处定义文件；
    // 业务面（studio/hub/invoice 外壳/admin/auth/i18n）一律零命中，
    // 无论 hex 还是空格三元组 rgb（vendor tokens.css 写的正是 `204 120 92` 空格写法，
    // 旧版正则只认逗号 rgba，这条曾假绿）。
    const isBrandSource = (p: string) => p === 'ui/tokens/tokens.css' || p === 'ui/tailwind-preset.js'
    expect(
      offenders(/CC785C|204\s+120\s+92|204,\s*120,\s*92/i, (p) => isBrandSource(p) || isInvoiceTicket(p))
    ).toEqual([])
  })

  // 2026-09-07 苹果式 v3：赤陶橙**回来了**（#CC785C 是 --snb-brand，#A85A3F 是 --snb-safety），
  // 这条从「清赤陶」翻成「清网吧 v2」——熔铁橙 / 安全橙 / 沥青四档 / 暖纸四档 / 纸米白 / 暖灰侧壁。
  // 业务面一律零容忍；vendored 与发票票据本体除外（票据的棕红 #97503C 与黄铜 #d9a35c 是它
  // 自己的票框色系，spec 决策 10 明写不换；发票**外壳**的回归靠批 3 Task 8 的边界表 + diff 核验）。
  it('🪦 网吧 v2 色板清零（vendored 与发票票据本体除外）', () => {
    const WANGBA_V2 =
      /#(BA4400|FF5C00|FFA372|0E1014|171A20|242A33|F2EEE6|FBF9F5|E5DFD3|EAE5DB|CFC8B8|C9C2B4|EFEBE4|1C1A16|D9A35C|141821|1B1F27|2C333D|3A424E|221E18|14120F|100F0D|1B1710|BDB4A9|8F877D|D6CCBE|7E766B|6F6B62|5A5750|3A362E|CACFD5|1a1613|0F0E0C)\b|(?:239[,\s]+235[,\s]+228)|(?:186[,\s]+68[,\s]+0)|(?:255[,\s]+92[,\s]+0)|(?:14[,\s]+16[,\s]+20)|(?:28[,\s]+26[,\s]+22)/i
    // ⏳ 苹果式 v3 分期收口：以下文件在批 3 Task 6–9 逐个清干净，清完必须从本表删掉。
    //    这张表**只减不增**；Task 11 的守卫断言它最终是空的。
    const PENDING = new Set<string>([
      'hub/hub.css',
      'hub/SerialSpotlight.tsx',
      'raffle-admin/raffle-admin.css',
      'invoice/GuestGate.tsx', // 🪦 纸米白 rgba(239,235,228,.28) 两处（Task 8 Step 6 清）
    ])
    expect(
      offenders(WANGBA_V2, (p) => isVendored(p) || isInvoiceTicket(p) || PENDING.has(p))
    ).toEqual([])
  })

  it('墨色黄铜色板清零', () => {
    expect(offenders(/#ded6c9|#35302b|#262220|#8f8578|#c9beae|#f5efe6|#1c1917|#131110|D4AF6A/i)).toEqual([])
  })

  it('衬线 display 字体遗产清零（发票票据本体除外）', () => {
    // font-display(?!\s*:) 否定环视:CSS 合法属性 font-display: swap(@font-face,2026-07-28
    // 随 Jost 自托管入库)与退役的 Tailwind font-display 工具类同名,fork 侧同款断言先例
    expect(offenders(/font-display(?!\s*:)|Georgia|Songti|STSong/, isInvoiceTicket)).toEqual([])
  })

  it('主题实现只许有一份：契约在 vendor 真源，业务面不许手抄第二套', () => {
    // 2026-07-29 双档补全后，这条从「切换能力清零」翻成「切换实现唯一」。
    // 恒暗期间之所以要清零，是分期上线的临时护栏（先推网吧化整车、浅色排第二步），
    // 不是「永远只做深色」；现在契约装回来了，要防的变成**另一个**风险：
    // 谁在业务面又手写一份 cookie 读写，就会出现两个真源、切档只切一半。
    // 允许出现的只有三处：vendor 进来的契约本体、它的 React 外壳、以及 portal 的转发层。
    const isThemeSource = (p: string) =>
      p === 'ui/theme/snbTheme.ts' ||
      p === 'ui/theme/useTheme.ts' ||
      p === 'ui/components/ThemeToggle/ThemeToggle.tsx' ||
      p === 'ui/index.ts' ||
      p === 'themeCookie.ts'
    // 手抄的判据 = 自己拼 snb_theme cookie 字符串 / 自己读 prefers-color-scheme
    expect(offenders(/snb_theme|prefers-color-scheme/, isThemeSource)).toEqual([])
  })

  // 🪦 「控制台|创作工坊|内容中心 零命中」整条退役（2026-09-07）：这三个词现在是**正确答案**
  //    （spec §4 文案回退），锁反了。新方向的守卫（网吧词零命中）见 src/__tests__/no-wangba.spec.ts。

  it('🪦 港风霓虹三灯/管芯/霓虹纸清零（hex 与 rgb 逗号/空格双写法全钉）', () => {
    expect(offenders(/FF6B35|00E5C7|FF3DDB|FFF4EA|F3E7DA|FF9C6F|FFBE9F/i)).toEqual([])
    expect(offenders(/255[,\s]+107[,\s]+53|0[,\s]+229[,\s]+199|255[,\s]+61[,\s]+219/)).toEqual([])
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

  it('text-shadow 全域清零（唯一例外：画墙灵感卡信息层的可读性实压投影，不是辉光）', () => {
    // 2026-07-29 画图机位改版后，画墙卡片信息层收敛进 studio/WallCard.tsx 一处
    // （GalleryTab/FavoritesTab 各自手抄一份的老形态已退役）。这里的 text-shadow 是
    // 纯黑实压阴影——浅色图（白仪表盘/米色信息图/热敏小票）上标题与署名读不出的解法之一，
    // 与「零发光」不冲突：它没有色相、没有扩散光晕。
    // 2026-07-29 双档补全：vendor 的 MasonryCard 同理由加入例外——它的署名行是 CC BY
    // 授权要求，压在用户上传的任意图上（可能是白仪表盘也可能是黑夜景），
    // 实压阴影是它两档恒定可读的手段之一。
    const isWallCard = (p: string) =>
      p === 'studio/WallCard.tsx' || p === 'ui/components/MasonryCard/MasonryCard.tsx'
    expect(offenders(/text-shadow/, isWallCard)).toEqual([])
  })

  it('🚨 backdrop-filter 业务面清零（唯一例外：vendored 设计系统）', () => {
    // 2026-07-28 全局 review #3 立的这条纪律不变：业务面不许自己写玻璃。
    // 2026-09-07 苹果式 v3 后，玻璃收口进 vendored 的三条工具类（.snb-glass / -side / -chip）
    // 与 AppHeader，业务面要玻璃就**用类名**，不许自己写 backdrop-filter/backdrop-blur。
    expect(offenders(/backdrop-filter|backdrop-blur/, isVendored)).toEqual([])
  })
})
