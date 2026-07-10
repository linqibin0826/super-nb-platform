import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ListPage } from '../pages/ListPage'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function jsonResponse(body: unknown) {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

const CATS = [
  { slug: 'tutorials', name: '教程', sortOrder: 1, count: 2 },
  { slug: 'ebooks', name: '电子书', sortOrder: 4, count: 1 },
]
const ART = (slug: string, extra: Record<string, unknown> = {}) => ({
  id: '42', slug, type: 'article', title: '标题' + slug, summary: '摘要', coverUrl: null,
  categorySlug: 'tutorials', categoryName: '教程', tags: ['Codex'], sourceName: null,
  publishedAt: '2026-07-10T00:00:00Z', ...extra,
})

function stubFetch(routes: Record<string, unknown>) {
  const calls: string[] = []
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    calls.push(url)
    for (const [prefix, body] of Object.entries(routes)) {
      if (url.startsWith(prefix)) return jsonResponse(typeof body === 'function' ? (body as (u: string) => unknown)(url) : body)
    }
    throw new Error('unexpected url ' + url)
  }))
  return calls
}

function renderPage() {
  return render(<MemoryRouter><ListPage /></MemoryRouter>)
}

describe('ListPage', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
  })

  it('渲染分类 tab 与卡片，电子书卡指向阅读页', async () => {
    stubFetch({
      '/content/v1/categories': CATS,
      '/content/v1/articles': { items: [ART('hello'), ART('book-x', { type: 'ebook' })], total: 2, page: 1, pages: 1 },
    })
    renderPage()

    await waitFor(() => expect(screen.getByText('标题hello')).toBeTruthy())
    const tabs = screen.getAllByRole('tab')
    expect(tabs).toHaveLength(3) // 全部 + 两个动态分类
    expect(tabs[1].textContent).toBe('教程')
    const ebookLink = screen.getByText('标题book-x').closest('a')!
    expect(ebookLink.getAttribute('href')).toBe('/reader/book-x')
    expect(screen.getByText('标题hello').closest('a')!.getAttribute('href')).toBe('/a/hello')
    expect(screen.getByTestId('hub-no-more')).toBeTruthy() // pages=1 无更多
  })

  it('切分类 tab 重新请求带 category 参数', async () => {
    const calls = stubFetch({
      '/content/v1/categories': CATS,
      '/content/v1/articles': { items: [ART('hello')], total: 1, page: 1, pages: 1 },
    })
    renderPage()
    await waitFor(() => expect(screen.getByText('标题hello')).toBeTruthy())

    screen.getAllByRole('tab')[1].click()
    await waitFor(() => expect(calls.some((u) => u.includes('category=tutorials'))).toBe(true))
  })

  it('多页时显示加载更多，点击追加下一页', async () => {
    stubFetch({
      '/content/v1/categories': CATS,
      '/content/v1/articles': (url: string) =>
        url.includes('page=2')
          ? { items: [ART('second')], total: 2, page: 2, pages: 2 }
          : { items: [ART('first')], total: 2, page: 1, pages: 2 },
    })
    renderPage()
    await waitFor(() => expect(screen.getByText('标题first')).toBeTruthy())

    screen.getByTestId('hub-load-more').click()
    await waitFor(() => expect(screen.getByText('标题second')).toBeTruthy())
    expect(screen.getByText('标题first')).toBeTruthy() // 追加而非替换
  })

  it('空态与错误重试', async () => {
    stubFetch({
      '/content/v1/categories': CATS,
      '/content/v1/articles': { items: [], total: 0, page: 1, pages: 1 },
    })
    renderPage()
    await waitFor(() => expect(screen.getByTestId('hub-empty')).toBeTruthy())
    cleanup()

    let fail = true
    vi.stubGlobal('fetch', vi.fn(async (url: string) => {
      if (url.startsWith('/content/v1/categories')) return jsonResponse(CATS)
      if (fail) { fail = false; throw new Error('network down') }
      return jsonResponse({ items: [ART('recovered')], total: 1, page: 1, pages: 1 })
    }))
    renderPage()
    await waitFor(() => expect(screen.getByTestId('hub-error')).toBeTruthy())
    screen.getByTestId('hub-retry').click()
    await waitFor(() => expect(screen.getByText('标题recovered')).toBeTruthy())
  })
})
