import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { EbookLongRead } from '../EbookLongRead'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
  localStorage.clear()
})

const BOOK = {
  title: 'OpenAI Codex 从入门到精通',
  subtitle: '面向工程师与产品经理',
  badge: '2026年5月 · v2.0.0',
  author: '花叔',
  metaLines: ['适用版本：CLI 0.130+'],
  totalMinutes: 137,
  chapters: [
    { index: 1, num: null, kind: 'preface', eyebrow: '序', title: '给读者的话', minutes: 5, html: '<p>前言正文</p>' },
    { index: 2, num: '01', kind: 'chapter', eyebrow: '基础', title: 'Codex 是什么', en: 'Five Forms', intro: '章引言', minutes: 12, html: '<p>正文</p>' },
    { index: 6, num: '05', kind: 'chapter', eyebrow: '进阶', title: 'AGENTS.md', minutes: 10, html: '<ol class="book-steps"><li><p>步骤</p></li></ol>' },
    { index: 13, num: 'B', kind: 'appendix', eyebrow: '附录', title: '定价', minutes: 3, html: '<p>附录正文</p>' },
  ],
}

function stubBook(book: unknown, ok = true) {
  vi.stubGlobal('fetch', vi.fn(async (url: string) =>
    String(url).endsWith('/book.json') && ok
      ? new Response(JSON.stringify(book), { status: 200, headers: { 'Content-Type': 'application/json' } })
      : new Response('nope', { status: 404 })))
}

function renderBook(opts: { slug?: string; part?: string } = {}) {
  const slug = opts.slug ?? 'codex-complete-guide-zh'
  const entry = opts.part ? `/a/${slug}/${opts.part}` : `/a/${slug}`
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <Routes>
        <Route path="/a/:slug/:part?" element={<EbookLongRead slug={slug} path={`books/${slug}`} />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('EbookLongRead', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
    vi.stubGlobal('IntersectionObserver', class { observe() {} disconnect() {} })
  })

  it('目录：masthead 信任行 + 14 部分卡片墙（编号/标题/时长），点卡链到独立页', async () => {
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-index')).toBeTruthy())
    // masthead 信任行
    const stats = screen.getByTestId('hub-book-stats').textContent!
    expect(stats).toContain('137')
    expect(stats).toContain('4')
    // 卡片：4 张，链到 /a/slug/{index}
    const cards = Array.from(screen.getByTestId('hub-book-index').querySelectorAll('a.hub-part-card'))
    expect(cards).toHaveLength(4)
    expect(cards.map((a) => a.getAttribute('href'))).toEqual([
      '/a/codex-complete-guide-zh/1',
      '/a/codex-complete-guide-zh/2',
      '/a/codex-complete-guide-zh/6',
      '/a/codex-complete-guide-zh/13',
    ])
    expect(cards[1].textContent).toContain('§01')
    expect(cards[1].textContent).toContain('Codex 是什么')
    expect(cards[3].textContent).toContain('B') // 附录编号
    // 「从头开始读」链到第一部分
    expect(screen.getByText(/从头开始读|Start reading/).closest('a')!.getAttribute('href')).toBe('/a/codex-complete-guide-zh/1')
  })

  it('部分页：章头（眉标/§chip/衬线题/英文/导语）+ 正文 + 上一/下一部分 + 顶条 k/N', async () => {
    stubBook(BOOK)
    renderBook({ part: '6' }) // §05
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    expect(screen.getByTestId('hub-book-parttop').textContent).toContain('3 / 4') // 第 3/4 部分
    expect(screen.getByText('§05')).toBeTruthy()
    expect(document.querySelector('.hub-sec-title')?.textContent).toBe('AGENTS.md')
    expect(screen.getByText('进阶')).toBeTruthy()
    expect(document.querySelector('.hub-prose .book-steps')).toBeTruthy() // 正文渲染
    // 上一/下一部分
    const nav = screen.getByTestId('hub-book-partnav')
    const links = Array.from(nav.querySelectorAll('a'))
    expect(links.find((a) => a.classList.contains('prev'))!.getAttribute('href')).toBe('/a/codex-complete-guide-zh/2')
    expect(links.find((a) => a.classList.contains('next'))!.getAttribute('href')).toBe('/a/codex-complete-guide-zh/13')
    expect(document.title).toContain('§05 AGENTS.md')
  })

  it('机制舞台 bespoke：codex §05 部分页注入 AGENTS.md 发现链；别的书不注入', async () => {
    stubBook(BOOK)
    renderBook({ part: '6' })
    await waitFor(() => expect(screen.getByTestId('hub-book-stage')).toBeTruthy())

    cleanup()
    stubBook(BOOK)
    renderBook({ slug: 'other-book', part: '6' })
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    expect(screen.queryByTestId('hub-book-stage')).toBeNull()
  })

  it('续读：localStorage 有位置则目录页显续读条链到那部分', async () => {
    localStorage.setItem('hub-book-pos:codex-complete-guide-zh', JSON.stringify({ index: 6 }))
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-resume')).toBeTruthy())
    const resume = screen.getByTestId('hub-book-resume')
    expect(resume.textContent).toContain('§05')
    expect(resume.getAttribute('href')).toBe('/a/codex-complete-guide-zh/6')
  })

  it('部分页读到即写 localStorage（供续读）', async () => {
    stubBook(BOOK)
    renderBook({ part: '13' })
    await waitFor(() => expect(screen.getByTestId('hub-book-parttop')).toBeTruthy())
    expect(JSON.parse(localStorage.getItem('hub-book-pos:codex-complete-guide-zh')!)).toEqual({ index: 13 })
  })

  it('无效 part 重定向回目录', async () => {
    stubBook(BOOK)
    renderBook({ part: '99' })
    await waitFor(() => expect(screen.getByTestId('hub-book-index')).toBeTruthy())
  })

  it('book.json 缺失显示内容缺失提示', async () => {
    stubBook(BOOK, false)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-reader-missing')).toBeTruthy())
  })
})
