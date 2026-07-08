import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  buildPromptsQuery,
  categoryName,
  clearPromptDetailCache,
  DEFAULT_PAGE_SIZE,
  fetchCategories,
  fetchInteractions,
  fetchPromptDetail,
  fetchPrompts,
  GalleryAuthError,
  toggleLike,
  type PromptDetail,
  type PromptListItem,
} from '../galleryApi'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function listItem(overrides: Partial<PromptListItem> = {}): PromptListItem {
  return {
    id: '1',
    title: '像素城市',
    imageUrl: 'https://media.super-nb.me/gallery/ym-1.webp',
    imageW: 480,
    imageH: 640,
    authorName: 'Alice',
    likeCount: 0,
    favCount: 0,
    ...overrides,
  }
}

function detail(overrides: Partial<PromptDetail> = {}): PromptDetail {
  return {
    ...listItem(),
    source: 'youmind',
    description: 'desc',
    promptText: 'isometric pixel city',
    lang: 'en',
    authorLink: 'https://x.com/alice',
    sourceLink: 'https://x.com/alice/status/1',
    category: { slug: 'poster', axis: 'scene', nameZh: '海报', nameEn: 'Poster' },
    sourcePublishedAt: null,
    createdAt: '2026-07-04T00:00:00Z',
    ...overrides,
  }
}

beforeEach(() => {
  clearPromptDetailCache()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('buildPromptsQuery', () => {
  it('缺省只带 page/pageSize（1/24）', () => {
    expect(buildPromptsQuery()).toBe('page=1&pageSize=24')
    expect(buildPromptsQuery({})).toBe('page=1&pageSize=24')
  })

  it('category / q / 自定义分页全拼上，q 会 trim', () => {
    expect(
      buildPromptsQuery({ category: 'poster', q: '  cat  ', page: 3, pageSize: 48 })
    ).toBe('category=poster&q=cat&page=3&pageSize=48')
  })

  it('category 为 null、q 为空白 → 省略', () => {
    expect(buildPromptsQuery({ category: null, q: '   ' })).toBe('page=1&pageSize=24')
  })

  it('q 含中文/特殊字符按 URL 编码', () => {
    expect(buildPromptsQuery({ q: '城市 海报&' })).toBe(
      `q=${encodeURIComponent('城市 海报&').replace(/%20/g, '+')}&page=1&pageSize=24`
    )
  })

  it('sort=newest 拼上；featured/缺省为服务端默认 → 省略', () => {
    expect(buildPromptsQuery({ sort: 'newest' })).toBe('sort=newest&page=1&pageSize=24')
    expect(buildPromptsQuery({ sort: 'featured' })).toBe('page=1&pageSize=24')
    expect(buildPromptsQuery({ category: 'poster', sort: 'newest' })).toBe(
      'category=poster&sort=newest&page=1&pageSize=24'
    )
  })
})

describe('fetchPrompts', () => {
  it('成功：URL 带查询串，响应字段兜底', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(200, { items: [listItem()], total: 100, page: 2, pages: 5 })
    )
    vi.stubGlobal('fetch', fetchMock)

    const res = await fetchPrompts({ category: 'poster', q: 'cat', page: 2 })
    expect(res.items).toHaveLength(1)
    expect(res.total).toBe(100)
    expect(res.page).toBe(2)
    expect(res.pages).toBe(5)

    const [url] = fetchMock.mock.calls[0]
    expect(url).toBe('/gallery/v1/prompts?category=poster&q=cat&page=2&pageSize=24')
  })

  it('响应缺 total/page/pages → 兜底为条数/请求页/1', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse(200, { items: [listItem(), listItem({ id: '2' })] }))
    )
    const res = await fetchPrompts({ page: 3 })
    expect(res.total).toBe(2)
    expect(res.page).toBe(3)
    expect(res.pages).toBe(1)
  })

  it('HTTP 非 200 → 抛错并带状态码', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('boom', { status: 502 })))
    await expect(fetchPrompts()).rejects.toThrow('gallery api HTTP 502')
  })

  it('结构缺 items → 抛错', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(jsonResponse(200, { total: 0 })))
    await expect(fetchPrompts()).rejects.toThrow('items missing')
  })
})

describe('fetchPromptDetail', () => {
  it('同 id 二次调用命中缓存，只发一次请求（并发连点也只发一次）', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, detail()))
    vi.stubGlobal('fetch', fetchMock)

    // 并发两次（模拟连点双击）+ 完成后再取一次
    const [a, b] = await Promise.all([fetchPromptDetail('1'), fetchPromptDetail('1')])
    const c = await fetchPromptDetail('1')
    expect(a.promptText).toBe('isometric pixel city')
    expect(b).toBe(a)
    expect(c).toBe(a)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(fetchMock.mock.calls[0][0]).toBe('/gallery/v1/prompts/1')
  })

  it('不同 id 各自请求', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, detail({ id: '1' })))
      .mockResolvedValueOnce(jsonResponse(200, detail({ id: '2' })))
    vi.stubGlobal('fetch', fetchMock)

    await fetchPromptDetail('1')
    await fetchPromptDetail('2')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[1][0]).toBe('/gallery/v1/prompts/2')
  })

  it('失败不进缓存：报错后重试会重新请求并成功', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response('nope', { status: 404 }))
      .mockResolvedValueOnce(jsonResponse(200, detail()))
    vi.stubGlobal('fetch', fetchMock)

    await expect(fetchPromptDetail('1')).rejects.toThrow('gallery api HTTP 404')
    const ok = await fetchPromptDetail('1')
    expect(ok.id).toBe('1')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })
})

describe('fetchCategories', () => {
  it('成功解析三轴，缺轴兜底为空数组', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(200, {
        scene: [{ slug: 'poster', nameZh: '海报', nameEn: 'Poster', count: 12 }],
        style: [{ slug: 'pixel', nameZh: '像素', nameEn: 'Pixel', count: 5 }],
      })
    )
    vi.stubGlobal('fetch', fetchMock)

    const tree = await fetchCategories()
    expect(tree.scene).toHaveLength(1)
    expect(tree.style[0].slug).toBe('pixel')
    expect(tree.subject).toEqual([])
    expect(fetchMock.mock.calls[0][0]).toBe('/gallery/v1/categories')
  })

  it('HTTP 非 200 → 抛错', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('x', { status: 500 })))
    await expect(fetchCategories()).rejects.toThrow('gallery api HTTP 500')
  })
})

describe('categoryName', () => {
  const item = { slug: 'poster', nameZh: '海报', nameEn: 'Poster', count: 1 }

  it('按 locale 取值并互相兜底', () => {
    expect(categoryName(item, 'zh-CN')).toBe('海报')
    expect(categoryName(item, 'en')).toBe('Poster')
    expect(categoryName({ ...item, nameZh: '' }, 'zh-CN')).toBe('Poster')
    expect(categoryName({ ...item, nameEn: '' }, 'en')).toBe('海报')
  })
})

describe('fetchPrompts 首屏预取消费', () => {
  afterEach(() => {
    delete (window as unknown as { __SNB_PREFETCH__?: unknown }).__SNB_PREFETCH__
  })

  it('命中默认首页参数：消费 __SNB_PREFETCH__.prompts，不再打 fetch', async () => {
    const payload = { items: [listItem()], total: 1, page: 1, pages: 3 }
    ;(window as unknown as { __SNB_PREFETCH__: unknown }).__SNB_PREFETCH__ = { prompts: Promise.resolve(payload) }
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    const res = await fetchPrompts({ page: 1, pageSize: DEFAULT_PAGE_SIZE })
    expect(res).toEqual(payload)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('预取只用一次：第二次同参走正常 fetch', async () => {
    const payload = { items: [listItem()], total: 1, page: 1, pages: 3 }
    ;(window as unknown as { __SNB_PREFETCH__: unknown }).__SNB_PREFETCH__ = { prompts: Promise.resolve(payload) }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse(200, { items: [], total: 0, page: 1, pages: 3 }))
    )
    await fetchPrompts({ page: 1, pageSize: DEFAULT_PAGE_SIZE })
    await fetchPrompts({ page: 1, pageSize: DEFAULT_PAGE_SIZE })
    expect(globalThis.fetch as unknown as ReturnType<typeof vi.fn>).toHaveBeenCalledTimes(1)
  })

  it('带翻页：不碰预取，走正常 fetch', async () => {
    const payload = { items: [listItem({ id: '99' })], total: 1, page: 1, pages: 3 }
    ;(window as unknown as { __SNB_PREFETCH__: unknown }).__SNB_PREFETCH__ = { prompts: Promise.resolve(payload) }
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(jsonResponse(200, { items: [], total: 0, page: 2, pages: 3 }))
    )
    const res = await fetchPrompts({ page: 2, pageSize: DEFAULT_PAGE_SIZE })
    expect(globalThis.fetch as unknown as ReturnType<typeof vi.fn>).toHaveBeenCalledTimes(1)
    expect(res.page).toBe(2)
  })
})

describe('galleryFetch 鉴权客户端', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('auth_token', 'at-1')
    localStorage.setItem('token_expires_at', String(Date.now() + 3_600_000))
  })
  afterEach(() => vi.restoreAllMocks())

  it('toggleLike POST 带 Bearer，回计数', async () => {
    const mock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ likeCount: 3, liked: true }), { status: 200 })
    )
    vi.stubGlobal('fetch', mock)
    expect(await toggleLike('7', true)).toEqual({ likeCount: 3, liked: true })
    const [url, init] = mock.mock.calls[0]
    expect(url).toBe('/gallery/v1/prompts/7/like')
    expect(init.method).toBe('POST')
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer at-1')
  })

  it('无 token → GalleryAuthError，不发请求', async () => {
    localStorage.clear()
    const mock = vi.fn()
    vi.stubGlobal('fetch', mock)
    await expect(fetchInteractions(['1', '2'])).rejects.toBeInstanceOf(GalleryAuthError)
    expect(mock).not.toHaveBeenCalled()
  })

  it('fetchInteractions 空 ids 短路不发请求', async () => {
    const mock = vi.fn()
    vi.stubGlobal('fetch', mock)
    expect(await fetchInteractions([])).toEqual({ liked: [], favorited: [] })
    expect(mock).not.toHaveBeenCalled()
  })
})

describe('buildPromptsQuery sort 扩展', () => {
  it('likes/favorites 进 query，featured 省略', () => {
    expect(buildPromptsQuery({ sort: 'likes' })).toContain('sort=likes')
    expect(buildPromptsQuery({ sort: 'favorites' })).toContain('sort=favorites')
    expect(buildPromptsQuery({ sort: 'featured' })).not.toContain('sort=')
  })
})
