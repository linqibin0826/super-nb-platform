import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
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

const EBOOK = { ...DETAIL, type: 'ebook', bodyHtml: null, ebookPath: 'books/hello.html', coverUrl: null }

/** ebook 用例专用：detail 与 EbookBody 的 HEAD 探活分流 stub。 */
function stubDetailAndHead(body: unknown, opts: { headOk?: boolean } = {}) {
  vi.stubGlobal('fetch', vi.fn(async (_url: string, init?: RequestInit) => {
    if (init?.method === 'HEAD') return new Response(null, { status: opts.headOk === false ? 404 : 200 })
    return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
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

/** 往 jsdom 的同源 iframe 塞一本假书（封面+书内目录页+两章），再触发 load 走分章逻辑。 */
const FAKE_BOOK = `
  <div class="cover">封面块</div>
  <div class="content"><h2 id="toc">目录</h2></div>
  <div class="content"><h2 id="ch1">第一章标题</h2><p>正文A</p></div>
  <div class="content"><h2 id="ch2">第二章标题</h2><p>正文B</p></div>`

async function loadFakeBook(): Promise<HTMLIFrameElement> {
  await waitFor(() => expect(screen.getByTitle('你好 Codex')).toBeTruthy())
  const iframe = screen.getByTitle('你好 Codex') as HTMLIFrameElement
  // jsdom 不加载 src：手工初始化 iframe 文档再触发 load
  const doc = iframe.contentDocument ?? iframe.contentWindow!.document
  doc.open()
  doc.write(`<!doctype html><html><body>${FAKE_BOOK}</body></html>`)
  doc.close()
  fireEvent.load(iframe)
  return iframe
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

  it('ebook 走同版式：书体 iframe 嵌正文位、署名行无阅读时长带新窗口打开、速览照常', async () => {
    stubDetailAndHead(EBOOK)
    renderAt('hello')

    await waitFor(() => expect(screen.getByTestId('hub-ebook')).toBeTruthy())
    const iframe = screen.getByTitle('你好 Codex') as HTMLIFrameElement
    expect(iframe.getAttribute('src')).toBe('/books/hello.html')
    expect(screen.getByTestId('hub-tldr')).toBeTruthy()
    const byline = screen.getByTestId('hub-byline').textContent!
    expect(byline).not.toMatch(/分钟读完|min read/) // ebook 无 bodyHtml，不估时长
    expect(screen.getByTestId('hub-reader-open').getAttribute('href')).toBe('/books/hello.html')
  })

  it('ebook 文件缺失（HEAD 404）在正文位显示缺失提示', async () => {
    stubDetailAndHead(EBOOK, { headOk: false })
    renderAt('hello')
    await waitFor(() => expect(screen.getByTestId('hub-reader-missing')).toBeTruthy())
  })

  it('ebook 书首页：目录卡列出章节（跳过封面与书内目录页），章内容隐藏只留封面', async () => {
    stubDetailAndHead(EBOOK)
    renderAt('hello')
    const iframe = await loadFakeBook()

    await waitFor(() => expect(screen.getByTestId('hub-book-toc')).toBeTruthy())
    const links = Array.from(screen.getByTestId('hub-book-toc').querySelectorAll('a'))
    expect(links.map((a) => a.textContent?.trim())).toEqual(['第一章标题', '第二章标题'])
    expect(links.map((a) => a.getAttribute('href'))).toEqual(['/a/hello/1', '/a/hello/2'])

    const doc = iframe.contentDocument!
    expect((doc.querySelector('.cover') as HTMLElement).style.display).toBe('')
    expect((doc.getElementById('ch1')!.parentElement as HTMLElement).style.display).toBe('none')
  })

  it('ebook 章节页：只显示当前章，顶条 k/N，上一/下一章导航就位', async () => {
    stubDetailAndHead(EBOOK)
    renderAt('hello/1')
    const iframe = await loadFakeBook()

    await waitFor(() => expect(screen.getByTestId('hub-book-strip')).toBeTruthy())
    expect(screen.getByTestId('hub-book-strip').textContent).toContain('1 / 2')
    expect(screen.queryByTestId('hub-book-toc')).toBeNull()

    const doc = iframe.contentDocument!
    expect((doc.getElementById('ch1')!.parentElement as HTMLElement).style.display).toBe('')
    expect((doc.getElementById('ch2')!.parentElement as HTMLElement).style.display).toBe('none')
    expect((doc.querySelector('.cover') as HTMLElement).style.display).toBe('none')

    const pager = screen.getByTestId('hub-book-pager')
    const nextLink = Array.from(pager.querySelectorAll('a')).find((a) => a.textContent?.includes('第二章标题'))!
    expect(nextLink.getAttribute('href')).toBe('/a/hello/2')
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
