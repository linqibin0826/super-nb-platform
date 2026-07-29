// 2026-07-29 画图机位改版的三条硬约束（会静默回退，人眼看不出来）：
// ① 访客版规格牌**一个模型名、一个价格数字都不许有**——模型清单绑 Key、估价绑分组，
//    编一个数字出去就是业务事故（业务事实护栏优先级高于任何设计指示）；
// ② 灵感卡按 imageW/H 预占 aspect-ratio——没有它图片落地会把整墙推开（CLS 0.23 的真凶）；
// ③ 大图浮层的位次口径是「第 N 张 · 本次筛选」，且提示词全文与两个动作同界面。
import { describe, expect, it, vi, beforeAll } from 'vitest'
import { render, screen } from '@testing-library/react'
import { GuestSpecBoard } from '../GuestSpecBoard'
import { WallLightbox } from '../WallLightbox'
import { WallCard } from '../WallCard'
import { st } from '../i18nStudio'

vi.mock('../../lib/galleryApi', async (orig) => {
  const actual = await orig<typeof import('../../lib/galleryApi')>()
  return { ...actual, fetchPromptDetail: vi.fn().mockResolvedValue({ promptText: 'hello prompt', category: null }) }
})

beforeAll(() => {
  vi.stubGlobal('ResizeObserver', class { observe() {} unobserve() {} disconnect() {} })
  vi.stubGlobal('matchMedia', (q: string) => ({ matches: false, media: q, addEventListener() {}, removeEventListener() {}, addListener() {}, removeListener() {}, onchange: null, dispatchEvent: () => false }))
})

const item = { id: '1', title: '白色运维仪表盘', imageUrl: 'https://media.super-nb.me/gallery/1.webp', imageW: 1536, imageH: 1024, authorName: '柜台阿姨', likeCount: 96, favCount: 33 }

describe('新件冒烟', () => {
  it('GuestSpecBoard 渲染：三格标题 + CTA，且不出现任何模型名/价格数字', () => {
    render(<GuestSpecBoard />)
    expect(screen.getByText(st('studio.guestBoard.modelsTitle'))).toBeTruthy()
    expect(screen.getByText(st('studio.guestBoard.cta'))).toBeTruthy()
    expect(screen.getByText('1:1')).toBeTruthy()
    expect(document.body.textContent).not.toMatch(/gpt-image|grok|\$\s?\d/)
  })

  it('WallCard 渲染：标题/署名常驻 + aspect-ratio 预占位', () => {
    const { container } = render(<WallCard src={item.imageUrl} alt={item.title} width={item.imageW} height={item.imageH} title={item.title} author={item.authorName} stats={<span>s</span>} onOpen={() => {}} />)
    expect(screen.getByText(item.title)).toBeTruthy()
    expect(screen.getByText('@柜台阿姨')).toBeTruthy()
    const shell = container.querySelector('img')!.parentElement as HTMLElement
    expect(shell.style.aspectRatio).toBe('1536 / 1024')
  })

  it('WallLightbox 渲染：计数 + 提示词全文 + 两个动作', async () => {
    render(<WallLightbox item={item} index={2} isMember={false} liked={false} favorited={false} likeCount={96} favCount={33} onToggleLike={() => {}} onToggleFavorite={() => {}} pending={false} copied={false} onUse={() => {}} onCopy={() => {}} onPrev={() => {}} onNext={() => {}} onClose={() => {}} />)
    expect(screen.getByText(st('studio.big.counter', { n: 2 }))).toBeTruthy()
    expect(await screen.findByText('hello prompt')).toBeTruthy()
    expect(screen.getByRole('button', { name: st('studio.big.use') })).toBeTruthy()
    expect(screen.getByText(st('studio.big.guestNote'))).toBeTruthy()
  })
})
