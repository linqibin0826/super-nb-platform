/** hub 内容 API 数据层：公开只读三端点（同源 /content/v1，生产由 Caddy 反代 snb-platform）。 */

export interface CategoryView {
  slug: string
  name: string
  sortOrder: number
  count: number
}

export interface ArticleSummary {
  id: string
  slug: string
  type: 'article' | 'ebook'
  title: string
  summary: string
  coverUrl: string | null
  categorySlug: string
  categoryName: string
  tags: string[]
  sourceName: string | null
  publishedAt: string
}

export interface ArticleDetail extends ArticleSummary {
  bodyHtml: string | null
  ebookPath: string | null
  sourceUrl: string | null
}

/** 分页信封（后端 Page<T> 契约）；hasMore = page < pages 由调用方判断。 */
export interface PageEnv<T> {
  items: T[]
  total: number
  page: number
  pages: number
}

/** 404 专用错误：详情页/阅读页据此渲染「内容不存在」空态。 */
export class NotFoundError extends Error {
  constructor() {
    super('not found')
    this.name = 'NotFoundError'
  }
}

const BASE = '/content/v1'

async function getJson<T>(url: string): Promise<T> {
  const res = await fetch(url)
  if (res.status === 404) throw new NotFoundError()
  if (!res.ok) throw new Error('http ' + res.status)
  return (await res.json()) as T
}

/** 全部分类（sort_order 排序，count 只计可见文章）。 */
export function getCategories(): Promise<CategoryView[]> {
  return getJson(`${BASE}/categories`)
}

/** 可见文章分页；category/tag 省缺即不过滤。 */
export function listArticles(p: {
  page: number
  pageSize?: number
  category?: string
  tag?: string
}): Promise<PageEnv<ArticleSummary>> {
  const q = new URLSearchParams()
  q.set('page', String(p.page))
  q.set('pageSize', String(p.pageSize ?? 12))
  if (p.category) q.set('category', p.category)
  if (p.tag) q.set('tag', p.tag)
  return getJson(`${BASE}/articles?${q}`)
}

/** 可见文章详情；不存在/已下架 → NotFoundError。 */
export function getArticle(slug: string): Promise<ArticleDetail> {
  return getJson(`${BASE}/articles/${encodeURIComponent(slug)}`)
}

/** book.json 契约（发布管线 buildBook 产出；dev 由 devBooks 现场转换吐同构数据）。 */
export interface BookChapter {
  index: number
  num: string | null // 前言=null、章节 "01".."10"、附录 "A"/"B"/"C"
  kind: 'preface' | 'chapter' | 'appendix'
  eyebrow: string
  title: string
  en?: string
  intro?: string
  minutes: number
  sections?: string[]
  html: string
}

export interface BookData {
  title: string
  subtitle?: string
  en?: string
  badge?: string
  author?: string
  metaLines?: string[]
  totalMinutes: number
  chapters: BookChapter[]
}

/** 电子书整本（book.json）；path = ebookPath = 'books/<slug>'，长读版一次拿全书一条流渲染。 */
export async function getBook(path: string): Promise<BookData> {
  const res = await fetch(`/${path}/book.json`)
  if (res.status === 404) throw new NotFoundError()
  if (!res.ok) throw new Error('http ' + res.status)
  return (await res.json()) as BookData
}
