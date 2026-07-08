import { apiFetch } from '../auth/apiFetch'
import type { StudioApiKey, StudioGroup } from '../types'

interface Paginated<T> {
  items: T[]
  total: number
  page: number
  pages: number
}

export async function fetchAllKeys(): Promise<StudioApiKey[]> {
  const first = await apiFetch<Paginated<StudioApiKey>>('/keys?page=1&page_size=100')
  const all = [...first.items]
  for (let page = 2; page <= first.pages; page++) {
    all.push(...(await apiFetch<Paginated<StudioApiKey>>(`/keys?page=${page}&page_size=100`)).items)
  }
  return all
}

export const fetchGroups = () => apiFetch<StudioGroup[]>('/groups/available')
export const fetchRates = () => apiFetch<Record<number, number> | null>('/groups/rates')
