import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ReaderPage } from '../pages/ReaderPage'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const BOOK = {
  id: '42', slug: 'codex-book', type: 'ebook', title: 'Codex 从入门到精通', summary: 's',
  coverUrl: null, categorySlug: 'ebooks', categoryName: '电子书', tags: [],
  bodyHtml: null, ebookPath: 'books/codex-book.html', sourceName: null, sourceUrl: null,
  publishedAt: '2026-07-10T00:00:00Z',
}

function stubFetch(detail: unknown, opts: { detailStatus?: number; headOk?: boolean } = {}) {
  vi.stubGlobal('fetch', vi.fn(async (_url: string, init?: RequestInit) => {
    if (init?.method === 'HEAD') return new Response(null, { status: opts.headOk === false ? 404 : 200 })
    return new Response(JSON.stringify(detail), {
      status: opts.detailStatus ?? 200,
      headers: { 'Content-Type': 'application/json' },
    })
  }))
}

function renderAt(slug: string) {
  return render(
    <MemoryRouter initialEntries={[`/reader/${slug}`]}>
      <Routes>
        <Route path="/reader/:slug" element={<ReaderPage />} />
        <Route path="/a/:slug" element={<div data-testid="article-route" />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ReaderPage', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
  })

  it('探活通过渲染同源 iframe 与顶部条（返回/书名/新窗口）', async () => {
    stubFetch(BOOK)
    renderAt('codex-book')

    await waitFor(() => expect(screen.getByTitle('Codex 从入门到精通')).toBeTruthy())
    const iframe = screen.getByTitle('Codex 从入门到精通') as HTMLIFrameElement
    expect(iframe.getAttribute('src')).toBe('/books/codex-book.html')
    expect(screen.getByTestId('hub-reader-back').getAttribute('href')).toBe('/')
    const openNew = screen.getByTestId('hub-reader-open')
    expect(openNew.getAttribute('href')).toBe('/books/codex-book.html')
    expect(openNew.getAttribute('target')).toBe('_blank')
  })

  it('电子书文件缺失（HEAD 404）显示错误提示', async () => {
    stubFetch(BOOK, { headOk: false })
    renderAt('codex-book')
    await waitFor(() => expect(screen.getByTestId('hub-reader-missing')).toBeTruthy())
  })

  it('article 类型重定向文章页', async () => {
    stubFetch({ ...BOOK, type: 'article', ebookPath: null, bodyHtml: '<p>x</p>' })
    renderAt('codex-book')
    await waitFor(() => expect(screen.getByTestId('article-route')).toBeTruthy())
  })

  it('404 渲染内容不存在空态', async () => {
    stubFetch({ detail: 'x' }, { detailStatus: 404 })
    renderAt('nope')
    await waitFor(() => expect(screen.getByTestId('hub-not-found')).toBeTruthy())
  })
})
