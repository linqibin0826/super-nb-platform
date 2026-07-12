import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
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
    { index: 2, num: '01', kind: 'chapter', eyebrow: '基础', title: 'Codex 是什么', en: 'Five Forms', intro: '章引言', minutes: 12, html: '<p>导语</p><p>正文</p>' },
    { index: 6, num: '05', kind: 'chapter', eyebrow: '进阶', title: 'AGENTS.md', minutes: 10, html: '<ol class="book-steps"><li><p>步骤</p></li></ol>' },
    { index: 13, num: 'B', kind: 'appendix', eyebrow: '附录', title: '定价', minutes: 3, html: '<figure class="book-table book-table--reference"><table><tbody><tr><td>x</td></tr></tbody></table></figure>' },
  ],
}

function stubBook(book: unknown, ok = true) {
  vi.stubGlobal('fetch', vi.fn(async (url: string) =>
    String(url).endsWith('/book.json') && ok
      ? new Response(JSON.stringify(book), { status: 200, headers: { 'Content-Type': 'application/json' } })
      : new Response('nope', { status: 404 })))
}

function renderBook(slug = 'codex-complete-guide-zh') {
  return render(
    <MemoryRouter>
      <EbookLongRead slug={slug} path={`books/${slug}`} />
    </MemoryRouter>,
  )
}

const chipTexts = () => Array.from(document.querySelectorAll('.hub-numchip')).map((e) => e.textContent)

describe('EbookLongRead', () => {
  beforeEach(() => {
    vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener: () => {}, removeEventListener: () => {} }))
    vi.stubGlobal('IntersectionObserver', class { observe() {} disconnect() {} })
  })

  it('masthead：刊号/衬线书名/信任行含总时长与章数', async () => {
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-stats')).toBeTruthy())
    expect(screen.getByText('OpenAI Codex 从入门到精通')).toBeTruthy()
    expect(screen.getByText('2026年5月 · v2.0.0')).toBeTruthy()
    const stats = screen.getByTestId('hub-book-stats').textContent!
    expect(stats).toContain('137') // totalMinutes
    expect(stats).toContain('4') // 章数
    expect(document.title).toContain('OpenAI Codex')
  })

  it('一条流：全章锚点、编号 chip（前言无/§01/附录 B）、正文走 hub-prose、时码轨就位', async () => {
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-rail')).toBeTruthy())
    expect(document.getElementById('s1')).toBeTruthy()
    expect(document.getElementById('s6')).toBeTruthy()
    expect(document.getElementById('s13')).toBeTruthy()
    const chips = chipTexts()
    expect(chips).toContain('§01')
    expect(chips).toContain('B')
    expect(chips).not.toContain('§序') // 前言无 chip
    expect(document.querySelector('.hub-prose')?.textContent).toContain('正文')
  })

  it('机制舞台 bespoke：codex §05 注入 AGENTS.md 发现链；别的书不注入', async () => {
    stubBook(BOOK)
    renderBook('codex-complete-guide-zh')
    await waitFor(() => expect(screen.getByTestId('hub-book-stage')).toBeTruthy())

    cleanup()
    stubBook(BOOK)
    renderBook('other-book')
    await waitFor(() => expect(screen.getByTestId('hub-book-rail')).toBeTruthy())
    expect(screen.queryByTestId('hub-book-stage')).toBeNull()
  })

  it('速览模式：点开切 is-scan 显横幅，退出复原', async () => {
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-stats')).toBeTruthy())
    fireEvent.click(screen.getByText(/90 秒|90s/))
    expect(screen.getByTestId('hub-book-scan')).toBeTruthy()
    expect(document.querySelector('.hub-book.is-scan')).toBeTruthy()
    fireEvent.click(screen.getByText(/退出速览|Exit scan/))
    expect(screen.queryByTestId('hub-book-scan')).toBeNull()
  })

  it('续读：localStorage 有较远位置则显续读条（不被时码轨挂载写覆盖）', async () => {
    localStorage.setItem('hub-book-pos:codex-complete-guide-zh', JSON.stringify({ index: 6 }))
    stubBook(BOOK)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-book-resume')).toBeTruthy())
    expect(screen.getByTestId('hub-book-resume').textContent).toContain('§05')
  })

  it('book.json 缺失显示内容缺失提示', async () => {
    stubBook(BOOK, false)
    renderBook()
    await waitFor(() => expect(screen.getByTestId('hub-reader-missing')).toBeTruthy())
  })
})
