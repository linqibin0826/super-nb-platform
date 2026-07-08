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
  return navigator.language.toLowerCase().startsWith('zh') ? 'zh' : 'en'
}
