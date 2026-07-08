import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent, cleanup } from '@testing-library/react'
import { CardStat } from '../CardStat'
import { t } from '../../i18n'

afterEach(cleanup)

describe('CardStat', () => {
  it('点击调 onToggle 且阻止冒泡（触屏点赞不触发整卡 onActivate）', () => {
    const onToggle = vi.fn()
    const onCard = vi.fn()
    render(
      <div onClick={onCard}>
        <CardStat kind="like" on={false} count={3} label={t('studio.gallery.like')} onToggle={onToggle} />
      </div>
    )
    fireEvent.click(screen.getByRole('button', { name: t('studio.gallery.like') }))
    expect(onToggle).toHaveBeenCalledOnce()
    expect(onCard).not.toHaveBeenCalled()
  })

  it('like on=true → aria-pressed + 图标 ember 上色 + 计数恒白', () => {
    render(<CardStat kind="like" on={true} count={9} label={t('studio.gallery.like')} onToggle={() => {}} />)
    const btn = screen.getByRole('button', { name: t('studio.gallery.like') })
    expect(btn.getAttribute('aria-pressed')).toBe('true')
    expect(btn.querySelector('.text-snb-ember')).toBeTruthy() // 品牌色只上图标 span
    expect(btn.className).toContain('text-white') // 计数恒白
    expect(screen.getByText('9')).toBeTruthy()
  })

  it('save on=true → 图标 amber 上色', () => {
    render(<CardStat kind="save" on={true} count={2} label={t('studio.gallery.save')} onToggle={() => {}} />)
    expect(
      screen.getByRole('button', { name: t('studio.gallery.save') }).querySelector('.text-snb-amber')
    ).toBeTruthy()
  })
})
