/**
 * 前端可配置项:域名相关常量外置为 VITE_* 环境变量,缺省值 = super-nb 生产值。
 * 开源者改自己域名时,设 .env.local 的 VITE_PARENT_DOMAIN / VITE_CONSOLE_ORIGIN 即可,无需改源码。
 * 缺省即生产值 → 既有测试与线上行为零变化。
 */
export const PARENT_DOMAIN: string = import.meta.env.VITE_PARENT_DOMAIN ?? 'super-nb.me'
export const CONSOLE_ORIGIN: string = import.meta.env.VITE_CONSOLE_ORIGIN ?? 'https://super-nb.me'
/** 控制台 host(从 CONSOLE_ORIGIN 解析):判断"当前站本身就是控制台根域"用 */
export const CONSOLE_HOST: string = new URL(CONSOLE_ORIGIN).host

/** host 是否属于父域(根域或其子域)——决定是否种父域 cookie / 是否跨站跳控制台 */
export function isParentDomainHost(host: string): boolean {
  return host === PARENT_DOMAIN || host.endsWith(`.${PARENT_DOMAIN}`)
}
