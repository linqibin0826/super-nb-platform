/**
 * 主题状态清理（港风霓虹恒暗改造，2026-07-27）。
 *
 * 全站已恒定深色，`snb_theme` 父域 cookie 契约作废。本文件只剩一件事：
 * 把存量状态清干净。不清的话，cookie 面上会长期挂一枚已无消费方的
 * `snb_theme`（无害但污染），旧 localStorage 键也会一直留着。
 *
 * ⚠️ 原为四边契约（fork ↔ snb-portal ↔ help ↔ 活动页）。fork 侧已在 Spec 2 拆除，
 *    本文件是第二边；help 侧在 Spec 3 Task 5 拆。活动页早已恒暗、无需改动。
 * ⚠️ 本文件不再导出任何写入口。留一个 write 就可能让某处又把站点切回浅色。
 */
import { PARENT_DOMAIN, isParentDomainHost } from './config'

const THEME_COOKIE_NAME = 'snb_theme'

/**
 * 生产（*.super-nb.me）当年种在父域，本地开发是 host-only。
 * 删除时 Domain 必须与写入时一致，否则删不掉——这是 cookie 删除最常见的失手处。
 */
function domainAttr(): string {
  return isParentDomainHost(location.hostname) ? `; Domain=.${PARENT_DOMAIN}` : ''
}

/** 一次性清理：删父域 cookie + 退役旧 localStorage 键。四个入口的启动路径各调一次。 */
export function purgeLegacyThemeState(): void {
  document.cookie = `${THEME_COOKIE_NAME}=${domainAttr()}; Path=/; Secure; SameSite=Lax; Max-Age=0`
  try {
    localStorage.removeItem('snb-studio-theme')
  } catch {
    // localStorage 不可用（隐私模式）时跳过
  }
}
