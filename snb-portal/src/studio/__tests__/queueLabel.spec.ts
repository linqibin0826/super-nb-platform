import { describe, expect, it } from 'vitest'
import { queueLabel } from '../queueLabel'
import { t } from '../../i18n'

describe('queueLabel', () => {
  it('非零项用 · 连接', () => {
    expect(queueLabel(2, 1, 1)).toBe(
      [
        t('studio.queue.metaRunning', { count: 2 }),
        t('studio.queue.metaQueued', { count: 1 }),
        t('studio.queue.metaDone', { count: 1 }),
      ].join(' · ')
    )
  })
  it('零项省略', () => {
    expect(queueLabel(0, 0, 3)).toBe(t('studio.queue.metaDone', { count: 3 }))
  })
  it('全零为空串', () => {
    expect(queueLabel(0, 0, 0)).toBe('')
  })
})
