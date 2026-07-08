import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  displayTitle,
  fetchGalleryIndex,
  galleryImageUrl,
  matchesFilter,
  type GalleryPrompt,
} from '../gallery'

afterEach(() => {
  vi.unstubAllGlobals()
})

function prompt(overrides: Partial<GalleryPrompt> = {}): GalleryPrompt {
  return {
    id: 'ff-001',
    title: { zh: '城市图鉴', en: 'Urban Atlas' },
    prompt: 'isometric city infographic',
    category: 'infographic',
    tags: ['city'],
    image: 'images/ff-001.webp',
    ...overrides,
  }
}

describe('gallery', () => {
  it('fetchGalleryIndex 成功解析并兜底缺省字段', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ version: 1, prompts: [prompt()] }), { status: 200 })
      )
    )
    const index = await fetchGalleryIndex()
    expect(index.prompts).toHaveLength(1)
    expect(index.categories).toEqual([])
    const [url] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(url).toBe('/playground-gallery/index.json')
  })

  it('HTTP 非 200 或结构缺 prompts → 抛错', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('nope', { status: 404 })))
    await expect(fetchGalleryIndex()).rejects.toThrow()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ version: 1 }), { status: 200 }))
    )
    await expect(fetchGalleryIndex()).rejects.toThrow()
  })

  it('galleryImageUrl 拼相对路径，无图返回 null', () => {
    expect(galleryImageUrl(prompt())).toBe('/playground-gallery/images/ff-001.webp')
    expect(galleryImageUrl(prompt({ image: '/images/a.webp' }))).toBe('/playground-gallery/images/a.webp')
    expect(galleryImageUrl(prompt({ image: undefined }))).toBeNull()
  })

  it('displayTitle 按 locale 取值并互相兜底', () => {
    expect(displayTitle({ zh: '中', en: 'EN' }, 'zh-CN')).toBe('中')
    expect(displayTitle({ zh: '中', en: 'EN' }, 'en')).toBe('EN')
    expect(displayTitle({ zh: '中' }, 'en')).toBe('中')
    expect(displayTitle({ en: 'EN' }, 'zh-CN')).toBe('EN')
    expect(displayTitle(undefined, 'zh-CN')).toBe('')
  })

  it('matchesFilter 组合分类与关键词（标题/提示词/tags，大小写不敏感）', () => {
    const p = prompt()
    expect(matchesFilter(p, null, '')).toBe(true)
    expect(matchesFilter(p, 'infographic', '')).toBe(true)
    expect(matchesFilter(p, 'poster', '')).toBe(false)
    expect(matchesFilter(p, null, 'ISOMETRIC')).toBe(true)
    expect(matchesFilter(p, null, '城市')).toBe(true)
    expect(matchesFilter(p, null, 'city')).toBe(true)
    expect(matchesFilter(p, null, 'nomatch')).toBe(false)
  })
})
