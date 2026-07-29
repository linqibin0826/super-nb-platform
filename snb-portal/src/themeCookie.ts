/**
 * 明暗主题契约 —— snb-portal 侧（四边契约第二边）。
 *
 * 【本文件为什么只剩转发】
 * 契约逻辑（父域 cookie 读写、缺席=跟随系统、切到与系统同值时删 cookie、聚焦对账、
 * 首帧防闪片段、旧键退役）现在只写在 **`src/ui/theme/snbTheme.ts`** 一处，
 * 那是跨子域名的唯一真源；portal 已经整包 vendor 了 `super-nb-ui/src/`，
 * 所以这里直接转发即可，不再手抄一份逻辑。
 * ⚠️ 契约要改，改真源（`src/ui/theme/snbTheme.ts` 文件头列了四个消费方与七条硬规则），
 * 然后重新 vendor —— 别在本文件里补分支。
 *
 * 【分期说明，别读成方向反复】
 * 双档一直在计划内。2026-07-26「全站恒暗」是**分期上线的第一步**（先把网吧化改造
 * 整车推上线），浅色皮肤排在第二步。分期期间本文件被削成只剩一次性清理、且写了
 * 「契约作废 / 不再导出任何写入口」——那是防回潮的**临时护栏**，不是产品意图。
 * 2026-07-29 按原计划补全第二步，契约装回。
 *
 * 【保留本文件而不是让业务直接 import 'src/ui'】
 * 四个入口的 main.tsx 与既有测试都指着这个路径；留一层转发比改散在各处的 import 稳，
 * 也给「portal 若哪天不再 vendor 整包」留一个收口点。
 */
export {
  THEME_COOKIE,
  THEME_COOKIE_DOMAIN,
  THEME_COOKIE_MAX_AGE,
  THEME_BOOT_SNIPPET,
  LEGACY_THEME_KEYS,
  themeCookieDomainAttr,
  readThemeCookie,
  systemTheme,
  resolveTheme,
  applyTheme,
  setThemePref,
  setTheme,
  toggleTheme,
  /** 旧 localStorage 键一次性迁移 + 无条件退役（含 studio 的 `snb-studio-theme`） */
  purgeLegacyThemeState,
  initTheme,
  type ThemeChoice,
  type ThemePref,
  type InitThemeOptions,
} from './ui/theme/snbTheme'
export { useTheme, type UseThemeResult } from './ui/theme/useTheme'
