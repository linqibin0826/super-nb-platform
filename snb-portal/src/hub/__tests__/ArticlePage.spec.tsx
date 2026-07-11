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

const EBOOK = { ...DETAIL, type: 'ebook', bodyHtml: null, ebookPath: 'books/hello', coverUrl: null }

const FAKE_TOC = {
  title: '你好 Codex',
  subtitle: '副题一行',
  badge: 'v2.0.0',
  author: '花叔',
  metaLines: ['适用版本：CLI 0.130+'],
  chapters: [
    { index: 1, title: '§01 第一章标题', en: 'One' },
    { index: 2, title: '§02 第二章标题' },
  ],
}

/** ebook 用例专用：detail / toc.json / 章 html 三路分流 stub。 */
function stubBook(detail: unknown, opts: { tocOk?: boolean } = {}) {
  vi.stubGlobal('fetch', vi.fn(async (url: string) => {
    const u = String(url)
    if (u.endsWith('/toc.json')) {
      if (opts.tocOk === false) return new Response('nope', { status: 404 })
      return new Response(JSON.stringify(FAKE_TOC), { status: 200, headers: { 'Content-Type': 'application/json' } })
    }
    const ch = u.match(/\/books\/hello\/(\d+)\.html$/)
    if (ch) {
      return new Response(`<p class="book-lead">第${ch[1]}章引言</p><p>正文段</p>`, {
        status: 200,
        headers: { 'Content-Type': 'text/html' },
      })
    }
    return new Response(JSON.stringify(detail), { status: 200, headers: { 'Content-Type': 'application/json' } })
  }))
}

function renderAt(slugPath: string) {
  return render(
    <MemoryRouter initialEntries={[`/a/${slugPath}`]}>
      <Routes>
        <Route path="/a/:slug/:chapter?" element={<ArticlePage />} />
      </Routes>
    </MemoryRouter>,
  )
}


describe('ArticlePage', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
    vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
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

  it('编辑部版式：速览面板显摘要、署名行带阅读时长与来源', async () => {
    stubDetail(DETAIL)
    renderAt('hello')

    await waitFor(() => expect(screen.getByTestId('hub-tldr')).toBeTruthy())
    expect(screen.getByTestId('hub-tldr').textContent).toContain('摘要')
    const byline = screen.getByTestId('hub-byline').textContent!
    expect(byline).toMatch(/分钟读完|min read/) // 阅读时长（jsdom locale 无关）
    expect(byline).toContain('站长整理')
  })

  it('封面：coverUrl 有值渲染 <img>，为空不渲染', async () => {
    stubDetail({ ...DETAIL, coverUrl: 'https://img.example/x.png' })
    renderAt('hello')
    await waitFor(() => expect(screen.getByText('正文段落')).toBeTruthy())
    expect(document.querySelector('img[src="https://img.example/x.png"]')).toBeTruthy()

    cleanup()
    stubDetail({ ...DETAIL, summary: '' })
    renderAt('hello')
    await waitFor(() => expect(screen.getByText('正文段落')).toBeTruthy())
    expect(document.querySelector('figure img')).toBeNull()
    expect(screen.queryByTestId('hub-tldr')).toBeNull() // 摘要为空不渲染速览
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

  it('ebook 书首页：书档案卡（副题/meta/作者·版本）+ 目录卡，署名行无阅读时长', async () => {
    stubBook(EBOOK)
    renderAt('hello')

    await waitFor(() => expect(screen.getByTestId('hub-book-toc')).toBeTruthy())
    expect(screen.getByTestId('hub-tldr')).toBeTruthy() // 速览照常
    expect(screen.getByTestId('hub-byline').textContent).not.toMatch(/分钟读完|min read/)

    const meta = screen.getByTestId('hub-book-meta')
    expect(meta.textContent).toContain('副题一行')
    expect(meta.textContent).toContain('适用版本：CLI 0.130+')
    expect(meta.textContent).toContain('花叔 · v2.0.0')

    const links = Array.from(screen.getByTestId('hub-book-toc').querySelectorAll('a'))
    expect(links.map((a) => a.textContent?.trim())).toEqual(['§01 第一章标题', '§02 第二章标题'])
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/a/hello/1', '/a/hello/2'])
  })

  it('ebook 章节页：章头衬线题+英文副题、正文走 hub-prose（book-lead 保留、净化兜底）、上一/下一章', async () => {
    stubBook(EBOOK)
    renderAt('hello/1')

    await waitFor(() => expect(screen.getByTestId('hub-book-strip')).toBeTruthy())
    expect(screen.getByTestId('hub-book-strip').textContent).toContain('1 / 2')
    expect(screen.queryByTestId('hub-book-toc')).toBeNull()
    expect(screen.getByText('§01 第一章标题')).toBeTruthy()
    expect(screen.getByText('One')).toBeTruthy()

    await waitFor(() => expect(screen.getByText('第1章引言')).toBeTruthy())
    expect(document.querySelector('.hub-prose .book-lead')).toBeTruthy()

    const pager = screen.getByTestId('hub-book-pager')
    const nextLink = Array.from(pager.querySelectorAll('a')).find((a) => a.textContent?.includes('第二章标题'))!
    expect(nextLink.getAttribute('href')).toBe('/a/hello/2')
    expect(document.title).toContain('§01 第一章标题')
  })

  it('ebook toc.json 缺失显示内容缺失提示', async () => {
    stubBook(EBOOK, { tocOk: false })
    renderAt('hello')
    await waitFor(() => expect(screen.getByTestId('hub-reader-missing')).toBeTruthy())
  })

  it('普通文章带章节段重定向净化回 /a/:slug', async () => {
    stubDetail(DETAIL)
    render(
      <MemoryRouter initialEntries={['/a/hello/3']}>
        <Routes>
          <Route path="/a/:slug/:chapter?" element={<ArticlePage />} />
        </Routes>
      </MemoryRouter>,
    )
    await waitFor(() => expect(screen.getByText('正文段落')).toBeTruthy())
  })
})
