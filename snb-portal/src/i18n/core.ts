export type LocaleDict = { [key: string]: string | LocaleDict }
export type Locale = 'zh' | 'en'

export function createT(messages: Record<Locale, LocaleDict>, locale: Locale) {
  return function t(key: string, params?: Record<string, string | number>): string {
    let node: string | LocaleDict | undefined = messages[locale]
    for (const part of key.split('.')) {
      node = typeof node === 'object' ? node[part] : undefined
    }
    if (typeof node !== 'string') return key
    if (!params) return node
    return node.replace(/\{(\w+)\}/g, (_, name: string) => String(params[name] ?? ''))
  }
}

export function detectLocale(): Locale {
  const saved = localStorage.getItem('sub2api_locale')
  if (saved === 'zh' || saved === 'en') return saved
  // 兜底中文:`sub2api_locale` 只由主站 fork 写在 super-nb.me 这个 origin 上,localStorage
  // 按 origin 隔离 ⇒ studio / hub / invoice 三个子站永远读不到用户在主站选过的语言;
  // 而客群是中国程序员、英文系统是常态,拿 navigator.language 当代理会把中文用户判成英文
  // (发票中心尤其伤:抬头/税号/联次没有自然英文对应,且 portal 内没有语言切换器无法自救)。
  // 这里是止血;根治=父域 cookie `snb_locale`(Domain=.super-nb.me),契约照抄 snb_theme。
  return 'zh'
}
