import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { PromptSheet } from '../PromptSheet'
import type { PromptListItem } from '../../lib/galleryApi'
import { t } from '../../i18n'

vi.mock('../../lib/galleryApi', async (orig) => {
  const actual = await orig<typeof import('../../lib/galleryApi')>()
  return { ...actual, fetchPromptDetail: vi.fn() }
})
import { fetchPromptDetail } from '../../lib/galleryApi'

const item: PromptListItem = {
  id: '7',
  title: 'Neon Cat',
  authorName: 'Daniel',
  imageUrl: 'https://media.super-nb.me/gallery/x.webp',
  imageW: 480,
  imageH: 640,
} as PromptListItem

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('PromptSheet', () => {
  it('展示大图/标题/作者，异步拉到提示词全文', async () => {
    ;(fetchPromptDetail as ReturnType<typeof vi.fn>).mockResolvedValue({ id: 7, promptText: 'a neon cat on a rooftop' })
    render(<PromptSheet item={item} pending={false} copied={false} onUse={vi.fn()} onCopy={vi.fn()} onClose={vi.fn()} />)
    expect(screen.getByText('Neon Cat · @Daniel')).toBeTruthy()
    expect((screen.getByAltText('Neon Cat') as HTMLImageElement).src).toContain('x.webp')
    await waitFor(() => expect(screen.getByText('a neon cat on a rooftop')).toBeTruthy())
  })

  it('「直接使用」调 onUse，「复制提示词」调 onCopy', async () => {
    ;(fetchPromptDetail as ReturnType<typeof vi.fn>).mockResolvedValue({ id: 7, promptText: 'p' })
    const onUse = vi.fn()
    const onCopy = vi.fn()
    render(<PromptSheet item={item} pending={false} copied={false} onUse={onUse} onCopy={onCopy} onClose={vi.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.use') }))
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.copy') }))
    expect(onUse).toHaveBeenCalledWith(item)
    expect(onCopy).toHaveBeenCalledWith(item)
  })

  it('Esc 与关闭钮都调 onClose', () => {
    ;(fetchPromptDetail as ReturnType<typeof vi.fn>).mockResolvedValue({ id: 7, promptText: 'p' })
    const onClose = vi.fn()
    render(<PromptSheet item={item} pending={false} copied={false} onUse={vi.fn()} onCopy={vi.fn()} onClose={onClose} />)
    // 关闭钮与背板都用 sheetClose 作可及名——取「关闭」文字钮（末尾那个）点击
    const closeButtons = screen.getAllByRole('button', { name: t('studio.gallery.sheetClose') })
    fireEvent.click(closeButtons[closeButtons.length - 1])
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(2)
  })

  it('传交互 props 时渲染赞/藏行，点击调对应 toggle', () => {
    ;(fetchPromptDetail as ReturnType<typeof vi.fn>).mockResolvedValue({ id: 7, promptText: 'p' })
    const onToggleLike = vi.fn()
    const onToggleFavorite = vi.fn()
    render(
      <PromptSheet
        item={item}
        pending={false}
        copied={false}
        onUse={vi.fn()}
        onCopy={vi.fn()}
        onClose={vi.fn()}
        liked={false}
        favorited={true}
        likeCount={5}
        favCount={3}
        onToggleLike={onToggleLike}
        onToggleFavorite={onToggleFavorite}
      />
    )
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.like') }))
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.save') }))
    expect(onToggleLike).toHaveBeenCalled()
    expect(onToggleFavorite).toHaveBeenCalled()
  })
})
