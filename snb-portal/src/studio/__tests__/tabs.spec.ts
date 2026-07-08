import { describe, expect, it } from 'vitest'
import { TAB_ITEMS } from '../tabs'

describe('TAB_ITEMS', () => {
  it('三档、顺序 gallery→favorites→history', () => {
    expect(TAB_ITEMS.map((x) => x.id)).toEqual(['gallery', 'favorites', 'history'])
  })
  it('每档有 labelKey', () => {
    for (const item of TAB_ITEMS) expect(item.labelKey).toMatch(/^playground\.tabs\./)
  })
})
