import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ArticlePage } from '../pages/ArticlePage'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

const DETAIL = {
  id: '42', slug: 'hello', type: 'article', title: '你好 Codex', summary: '摘要',
  coverUrl: null, categorySlug: 'tutorials', categoryName: '教程', tags: ['Codex'],
  bodyHtml: '<script>window.__pwned = true</script><p>正文段落</p>',
  ebookPath: null, sourceName: '站长整理', sourceUrl: 'https://example.com/origin',
  publishedAt: '2026-07-10T00:00:00Z',
}

function stubDetail(body: unknown, status = 200) {
  vi.stubGlobal('fetch', vi.fn(async () =>
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })))
}

function renderAt(slug: string) {
  return render(
    <MemoryRouter initialEntries={[`/a/${slug}`]}>
      <Routes>
        <Route path="/a/:slug" element={<ArticlePage />} />
        <Route path="/reader/:slug" element={<div data-testid="reader-route" />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('ArticlePage', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
  })

  it('渲染净化后的正文：script 被剥、段落保留、来源区带安全外链', async () => {
    stubDetail(DETAIL)
    renderAt('hello')

    await waitFor(() => expect(screen.getByText('正文段落')).toBeTruthy())
    const prose = document.querySelector('.hub-prose')!
    expect(prose.querySelector('script')).toBeNull() // DOMPurify 兜底生效
    expect((window as unknown as { __pwned?: boolean }).__pwned).toBeUndefined()

    const source = screen.getByTestId('hub-source')
    expect(source.textContent).toContain('站长整理')
    const link = source.querySelector('a')!
    expect(link.getAttribute('href')).toBe('https://example.com/origin')
    expect(link.getAttribute('rel')).toContain('noopener')
    expect(document.title).toContain('你好 Codex')
  })

  it('无来源信息不渲染来源区', async () => {
    stubDetail({ ...DETAIL, sourceName: null, sourceUrl: null })
    renderAt('hello')
    await waitFor(() => expect(screen.getByText('正文段落')).toBeTruthy())
    expect(screen.queryByTestId('hub-source')).toBeNull()
  })

  it('404 渲染内容不存在空态', async () => {
    stubDetail({ detail: 'x' }, 404)
    renderAt('nope')
    await waitFor(() => expect(screen.getByTestId('hub-not-found')).toBeTruthy())
  })

  it('ebook 类型重定向阅读页', async () => {
    stubDetail({ ...DETAIL, type: 'ebook', bodyHtml: null, ebookPath: 'books/hello.html' })
    renderAt('hello')
    await waitFor(() => expect(screen.getByTestId('reader-route')).toBeTruthy())
  })
})
