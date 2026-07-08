// 拷自 sub2api fork feat/image-playground@4f873b56 frontend/src/features/playground/lib/ —— 框架无关，与 fork 同步演进须手动
// 灵感库数据：线上由 Caddy file_server 提供（super-nb-gallery rsync 更新，零发版），
// dev 由 frontend/public/playground-gallery/ 样例兜底（同 URL，无环境分支）。
export const GALLERY_BASE = '/playground-gallery'

export interface GalleryTitle {
  zh?: string
  en?: string
}

export interface GallerySource {
  author?: string
  author_url?: string
  via?: string
  via_url?: string
  license?: string
}

export interface GalleryPrompt {
  id: string
  title: GalleryTitle
  prompt: string
  lang?: string
  category?: string
  tags?: string[]
  image?: string
  params?: { size?: string; quality?: string }
  source?: GallerySource
}

export interface GalleryCategory {
  id: string
  title: GalleryTitle
}

export interface GalleryIndex {
  version: number
  updated_at?: string
  categories: GalleryCategory[]
  prompts: GalleryPrompt[]
}

export async function fetchGalleryIndex(signal?: AbortSignal): Promise<GalleryIndex> {
  const response = await fetch(`${GALLERY_BASE}/index.json`, { signal, cache: 'no-cache' })
  if (!response.ok) throw new Error(`gallery index HTTP ${response.status}`)
  const data = (await response.json()) as Partial<GalleryIndex>
  if (!Array.isArray(data.prompts)) throw new Error('gallery index malformed: prompts missing')
  return {
    version: data.version ?? 0,
    updated_at: data.updated_at,
    categories: Array.isArray(data.categories) ? data.categories : [],
    prompts: data.prompts,
  }
}

export function galleryImageUrl(prompt: GalleryPrompt): string | null {
  if (!prompt.image) return null
  return `${GALLERY_BASE}/${prompt.image.replace(/^\/+/, '')}`
}

export function displayTitle(title: GalleryTitle | undefined, locale: string): string {
  if (!title) return ''
  if (locale.toLowerCase().startsWith('zh')) return title.zh || title.en || ''
  return title.en || title.zh || ''
}

export function matchesFilter(
  prompt: GalleryPrompt,
  category: string | null,
  keyword: string
): boolean {
  if (category && prompt.category !== category) return false
  const kw = keyword.trim().toLowerCase()
  if (!kw) return true
  const haystack = [prompt.title.zh, prompt.title.en, prompt.prompt, ...(prompt.tags ?? [])]
    .filter(Boolean)
    .join('\n')
    .toLowerCase()
  return haystack.includes(kw)
}
