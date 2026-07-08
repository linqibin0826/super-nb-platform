// snb-platform 灵感库 API 客户端（/gallery/v1，2026-07-07 起对齐自写平台新契约：
// camelCase 字段、实体 id 一律字符串（雪花超 JS 安全整数）、错误体 problem+json）。
// 旧 gallery-svc（/gallery/api、snake_case、数字 id）已由平台收编，割接与前端同波切换。
export const GALLERY_API_BASE = '/gallery/v1'

import { getToken, isExpiringSoon } from '../auth/tokens'
import { refreshTokens } from '../auth/refresh'

/** 未登录 / 刷新后仍 401：交给上层引导登录 */
export class GalleryAuthError extends Error {
  constructor() {
    super('gallery auth required')
    this.name = 'GalleryAuthError'
  }
}

/** 带鉴权的 gallery 请求：复用主站 token 原语（临期先刷 + 401 刷一次重试）。
 *  apiFetch 打的是 /api/v1、base 不同，故这里另起一个指向 GALLERY_API_BASE 的。 */
async function galleryFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
  if (!retried && isExpiringSoon()) await refreshTokens()
  const token = getToken()
  if (!token) throw new GalleryAuthError()
  const res = await fetch(`${GALLERY_API_BASE}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(init.headers ?? {}),
    },
  })
  if (res.status === 401) {
    if (!retried && (await refreshTokens())) return galleryFetch<T>(path, init, true)
    throw new GalleryAuthError()
  }
  if (!res.ok) throw new Error(`gallery api HTTP ${res.status}`)
  return (await res.json()) as T
}

export const DEFAULT_PAGE_SIZE = 24

/** 列表项（瘦身：不含 prompt 全文）。id 是服务端雪花 id 的字符串形态，全程当不透明串用 */
export interface PromptListItem {
  id: string
  title: string
  imageUrl: string
  imageW: number
  imageH: number
  authorName: string | null
  likeCount: number
  favCount: number
}

export interface PromptListResponse {
  items: PromptListItem[]
  total: number
  page: number
  pages: number
}

/** 详情所属类目（平台读视图内嵌对象，取代旧 category_id） */
export interface PromptCategory {
  slug: string
  axis: CategoryAxis
  nameZh: string
  nameEn: string
}

/** 详情全字段（对齐平台 PromptDetail 读视图；「直接使用/复制」时才取） */
export interface PromptDetail extends PromptListItem {
  source: string
  description: string | null
  promptText: string
  lang: string | null
  authorLink: string | null
  sourceLink: string | null
  sourcePublishedAt: string | null
  createdAt: string
  category: PromptCategory | null
}

export type CategoryAxis = 'scene' | 'style' | 'subject'

export interface CategoryItem {
  slug: string
  nameZh: string
  nameEn: string
  count: number
}

/** 三轴类目树：场景 / 风格 / 主体 */
export type CategoryTree = Record<CategoryAxis, CategoryItem[]>

/** 列表排序：featured=收录默认序（服务端 id 倒序）；newest=按源发布时间倒序（无时间沉底） */
export type GallerySort = 'featured' | 'newest' | 'likes' | 'favorites'

export interface FetchPromptsParams {
  /** 单类目 slug（一期每条单类目，任一轴选中即传） */
  category?: string | null
  q?: string
  sort?: GallerySort
  page?: number
  pageSize?: number
}

/** 拼列表查询串（纯函数，单测覆盖）：category/q 为空、sort 为默认 featured 则省略，page/pageSize 恒定输出 */
export function buildPromptsQuery(params: FetchPromptsParams = {}): string {
  const search = new URLSearchParams()
  if (params.category) search.set('category', params.category)
  const q = params.q?.trim()
  if (q) search.set('q', q)
  if (params.sort && params.sort !== 'featured') search.set('sort', params.sort)
  search.set('page', String(params.page ?? 1))
  search.set('pageSize', String(params.pageSize ?? DEFAULT_PAGE_SIZE))
  return search.toString()
}

async function requestJson<T>(url: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(url, { signal })
  if (!response.ok) throw new Error(`gallery api HTTP ${response.status}`)
  return (await response.json()) as T
}

export async function fetchPrompts(
  params: FetchPromptsParams = {},
  signal?: AbortSignal
): Promise<PromptListResponse> {
  // 首屏默认参数命中：优先消费 index.html 内联脚本的预取（一次性，用后即弃，失败回退正常请求）
  const isFirstDefault =
    !params.category &&
    !params.q?.trim() &&
    (params.sort ?? 'featured') === 'featured' &&
    (params.page ?? 1) === 1 &&
    (params.pageSize ?? DEFAULT_PAGE_SIZE) === DEFAULT_PAGE_SIZE
  if (isFirstDefault && typeof window !== 'undefined') {
    const cache = (window as unknown as {
      __SNB_PREFETCH__?: { prompts?: Promise<PromptListResponse | null> }
    }).__SNB_PREFETCH__
    const pending = cache?.prompts
    if (pending) {
      cache!.prompts = undefined
      const prefetched = await pending.catch(() => null)
      if (prefetched && Array.isArray(prefetched.items)) return prefetched
    }
  }

  const data = await requestJson<Partial<PromptListResponse>>(
    `${GALLERY_API_BASE}/prompts?${buildPromptsQuery(params)}`,
    signal
  )
  if (!Array.isArray(data.items)) throw new Error('gallery api malformed: items missing')
  return {
    items: data.items,
    total: data.total ?? data.items.length,
    page: data.page ?? params.page ?? 1,
    pages: data.pages ?? 1,
  }
}

// 详情内存缓存（数据只读，缓存 Promise 同时挡住连点双击的并发双请求；失败则清除以便重试）
const detailCache = new Map<string, Promise<PromptDetail>>()

export function fetchPromptDetail(id: string, signal?: AbortSignal): Promise<PromptDetail> {
  const cached = detailCache.get(id)
  if (cached) return cached
  const pending = requestJson<PromptDetail>(`${GALLERY_API_BASE}/prompts/${id}`, signal).catch(
    (err: unknown) => {
      detailCache.delete(id)
      throw err
    }
  )
  detailCache.set(id, pending)
  return pending
}

/** 仅测试用：清空详情缓存 */
export function clearPromptDetailCache(): void {
  detailCache.clear()
}

function normalizeCategoryTree(data: Partial<CategoryTree>): CategoryTree {
  return {
    scene: Array.isArray(data.scene) ? data.scene : [],
    style: Array.isArray(data.style) ? data.style : [],
    subject: Array.isArray(data.subject) ? data.subject : [],
  }
}

export async function fetchCategories(signal?: AbortSignal): Promise<CategoryTree> {
  // 首屏预取消费（一次性，与 fetchPrompts 对称）：拿到就归一返回，失败/缺席回退正常请求
  if (typeof window !== 'undefined') {
    const cache = (window as unknown as {
      __SNB_PREFETCH__?: { categories?: Promise<Partial<CategoryTree> | null> }
    }).__SNB_PREFETCH__
    const pending = cache?.categories
    if (pending) {
      cache!.categories = undefined
      const prefetched = await pending.catch(() => null)
      if (prefetched) return normalizeCategoryTree(prefetched)
    }
  }

  const data = await requestJson<Partial<CategoryTree>>(`${GALLERY_API_BASE}/categories`, signal)
  return normalizeCategoryTree(data)
}

/** 类目名按 locale 取值并互相兜底 */
export function categoryName(item: CategoryItem, locale: string): string {
  if (locale.toLowerCase().startsWith('zh')) return item.nameZh || item.nameEn
  return item.nameEn || item.nameZh
}

export interface InteractionResult {
  likeCount?: number
  favCount?: number
  liked?: boolean
  favorited?: boolean
}

/** 点赞 / 取消赞（on=true 走 POST，false 走 DELETE），回本条最新计数 */
export function toggleLike(id: string, on: boolean): Promise<InteractionResult> {
  return galleryFetch(`/prompts/${id}/like`, { method: on ? 'POST' : 'DELETE' })
}

/** 收藏 / 取消收藏 */
export function toggleFavorite(id: string, on: boolean): Promise<InteractionResult> {
  return galleryFetch(`/prompts/${id}/favorite`, { method: on ? 'POST' : 'DELETE' })
}

export interface MyInteractions {
  liked: string[]
  favorited: string[]
}

/** 批量回「这批 id 里我赞了 / 藏了哪些」；空 ids 直接短路 */
export function fetchInteractions(ids: string[], signal?: AbortSignal): Promise<MyInteractions> {
  if (ids.length === 0) return Promise.resolve({ liked: [], favorited: [] })
  return galleryFetch(`/me/interactions?ids=${ids.join(',')}`, { signal })
}

/** 我的收藏分页（同列表信封） */
export function fetchMyFavorites(page = 1, signal?: AbortSignal): Promise<PromptListResponse> {
  return galleryFetch(`/me/favorites?page=${page}&pageSize=${DEFAULT_PAGE_SIZE}`, { signal })
}
