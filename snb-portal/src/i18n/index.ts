import { createT, detectLocale, type Locale } from './core'
import { messages } from './messages'

export type { Locale, LocaleDict } from './core'
export { createT, detectLocale } from './core'

// 模块级单例：locale 页面加载时定死（一期不做运行时切换）
export const locale: Locale = detectLocale()
export const t = createT(messages, locale)
