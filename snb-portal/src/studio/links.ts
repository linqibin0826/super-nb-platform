// studio 内跳主站的直链（与 TopBar 内部 consoleHref 同规则）：
// 本地开发同源直达（本地 sub2api 栈），生产直链控制台根域。
// 注册/登录都不做跨域回跳——登录后切回子站由聚焦对账自动接上（子域名部署约定）。
import { CONSOLE_ORIGIN } from '../config'

const isLocalDev =
  typeof location !== 'undefined' &&
  (location.hostname === 'localhost' || location.hostname === '127.0.0.1')

/** 「开卡上机」注册页 */
export function registerUrl(): string {
  return isLocalDev ? '/register' : `${CONSOLE_ORIGIN}/register`
}
