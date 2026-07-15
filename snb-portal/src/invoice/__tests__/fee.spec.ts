import { describe, expect, it } from 'vitest'
import { feeCents, fmtYuan, selectedTotalCents } from '../fee'

const overview = { minTotal: 1000, freeThreshold: 3000, feeRate: 0.05 }
const orders = [
  { orderId: '1', orderNo: 'T1', amount: 600, completedAt: '' },
  { orderId: '2', orderNo: 'T2', amount: 500.5, completedAt: '' },
]

describe('fee mirror of FeePolicy', () => {
  it('sums selected orders in cents (float-safe)', () => {
    expect(selectedTotalCents(orders, new Set(['1', '2']))).toBe(110050)
    expect(selectedTotalCents(orders, new Set(['1']))).toBe(60000)
  })
  it('charges 5% half-up below threshold, free at/above', () => {
    expect(feeCents(100000, overview)).toBe(5000)     // ¥1000 → ¥50
    expect(feeCents(299999, overview)).toBe(15000)    // ¥2999.99 → ¥150.00(四舍五入)
    expect(feeCents(300000, overview)).toBe(0)        // ¥3000 免
  })
  it('formats cents as yuan', () => {
    expect(fmtYuan(110050)).toBe('1100.50')
    expect(fmtYuan(0)).toBe('0.00')
  })
})
