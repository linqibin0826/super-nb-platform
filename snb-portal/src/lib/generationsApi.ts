// 生成历史 API 客户端（snb-platform /gallery/v1 新契约：camelCase、id 由服务端雪花生成——
// 请求体不再带客户端 uuid，防重靠队列单飞）。鉴权走与 galleryApi 同款的 Bearer + 401 刷新重试。
import { GALLERY_API_BASE, GalleryAuthError } from './galleryApi'
import { getToken, isExpiringSoon } from '../auth/tokens'
import { refreshTokens } from '../auth/refresh'

async function authFetch<T>(path: string, init: RequestInit = {}, retried = false): Promise<T> {
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
    if (!retried && (await refreshTokens())) return authFetch<T>(path, init, true)
    throw new GalleryAuthError()
  }
  if (!res.ok) throw new Error(`generations api HTTP ${res.status}`)
  return (await res.json()) as T
}

export interface CreateGenerationInput {
  prompt: string
  size: string
  n: number
  quality: string
  status: 'done' | 'error'
  cost: number | null
  elapsedMs: number
  groupName: string | null
  keyId: number | null
  error: string | null
  outputImages: { b64: string }[]
  refImages: { b64: string; contentType: string }[]
}

/** 建单：字段名与平台请求 DTO 一一对应，直接透传；响应回服务端生成的雪花 id（字符串） */
export function createGeneration(input: CreateGenerationInput): Promise<{ id: string }> {
  return authFetch('/me/generations', { method: 'POST', body: JSON.stringify(input) })
}

export interface GenerationListItem {
  id: string
  createdAt: string
  prompt: string
  size: string
  n: number
  quality: string
  status: 'done' | 'error'
  cost: number | null
  elapsedMs: number
  error: string | null
  thumbUrl: string | null
}

export interface GenerationListResponse {
  items: GenerationListItem[]
  total: number
  page: number
  pages: number
}

export interface GenerationDetail extends Omit<GenerationListItem, 'thumbUrl'> {
  groupName: string | null
  keyId: number | null
  outputImages: { url: string; width: number | null; height: number | null }[]
  refImages: { url: string }[]
}

export const GENERATIONS_PAGE_SIZE = 24

export function listGenerations(page = 1, signal?: AbortSignal): Promise<GenerationListResponse> {
  return authFetch(`/me/generations?page=${page}&pageSize=${GENERATIONS_PAGE_SIZE}`, { signal })
}

export function getGeneration(id: string, signal?: AbortSignal): Promise<GenerationDetail> {
  return authFetch(`/me/generations/${id}`, { signal })
}

export function deleteGeneration(id: string): Promise<void> {
  return authFetch(`/me/generations/${id}`, { method: 'DELETE' })
}
