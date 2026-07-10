import { afterEach, describe, expect, it, vi } from 'vitest'
import { getArticle, getCategories, listArticles, NotFoundError } from '../api'

function jsonResponse(status: number, body: unknown) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

afterEach(() => vi.unstubAllGlobals())

describe('hub api', () => {
  it('listArticles 拼装查询串（含缺省省略）并返回信封', async () => {
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(jsonResponse(200, { items: [], total: 0, page: 1, pages: 1 })))
    vi.stubGlobal('fetch', fetchMock)

    const page = await listArticles({ page: 2, category: 'tutorials', tag: 'MCP' })

    expect(fetchMock).toHaveBeenCalledWith('/content/v1/articles?page=2&pageSize=12&category=tutorials&tag=MCP')
    expect(page.pages).toBe(1)

    await listArticles({ page: 1 })
    expect(fetchMock).toHaveBeenLastCalledWith('/content/v1/articles?page=1&pageSize=12')
  })

  it('getCategories 返回数组', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(200, [{ slug: 't', name: '教程', sortOrder: 1, count: 2 }])))
    const cats = await getCategories()
    expect(cats[0].slug).toBe('t')
  })

  it('getArticle 404 抛 NotFoundError，其余非 2xx 抛普通错误', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(404, { detail: 'x' })))
    await expect(getArticle('nope')).rejects.toBeInstanceOf(NotFoundError)

    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(500, {})))
    await expect(getArticle('boom')).rejects.toThrow('http 500')
  })
})
