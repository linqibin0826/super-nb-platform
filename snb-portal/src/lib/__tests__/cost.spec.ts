import { describe, expect, it } from 'vitest'
import { estimateCost, type ImageCostGroup } from '../cost'

function group(overrides: Partial<ImageCostGroup> = {}): ImageCostGroup {
  return {
    rate_multiplier: 1,
    image_rate_independent: false,
    image_rate_multiplier: 1,
    image_price_1k: 0.05,
    image_price_2k: 0.1,
    image_price_4k: 0.2,
    ...overrides,
  }
}

// 公式镜像后端 CalculateImageCost（billing_service.go:1181）+
// resolveImageRateMultiplier（image_billing_multiplier.go:3）
describe('estimateCost', () => {
  it('基础：单价 × 张数 × 分组倍率', () => {
    expect(estimateCost(group(), '1024x1024', 2)).toBeCloseTo(0.1)
    expect(estimateCost(group({ rate_multiplier: 2 }), '1024x1024', 2)).toBeCloseTo(0.2)
  })

  it('用户自定义分组倍率优先于 group.rate_multiplier', () => {
    expect(estimateCost(group({ rate_multiplier: 2 }), '1024x1024', 1, 0.5)).toBeCloseTo(0.025)
  })

  it('image_rate_independent 时用 image_rate_multiplier 且负数按 0', () => {
    const g = group({ image_rate_independent: true, image_rate_multiplier: 3, rate_multiplier: 9 })
    expect(estimateCost(g, '1024x1024', 1)).toBeCloseTo(0.15)
    expect(
      estimateCost(group({ image_rate_independent: true, image_rate_multiplier: -1 }), '1024x1024', 1)
    ).toBe(0)
  })

  it('1536x1024 按 2K 档取价（不是 1K）', () => {
    expect(estimateCost(group(), '1536x1024', 1)).toBeCloseTo(0.1)
  })

  it('auto 按 2K 档估价', () => {
    expect(estimateCost(group(), 'auto', 1)).toBeCloseTo(0.1)
  })

  it('该档价格未配置（null）→ null（后端走 LiteLLM 默认价，前端不可知）', () => {
    expect(estimateCost(group({ image_price_1k: null }), '1024x1024', 1)).toBeNull()
  })

  it('价格配置为 0 → 估价 0（免费分组合法）', () => {
    expect(estimateCost(group({ image_price_1k: 0 }), '1024x1024', 3)).toBe(0)
  })

  it('group 缺失或 n<=0 → null', () => {
    expect(estimateCost(null, '1024x1024', 1)).toBeNull()
    expect(estimateCost(group(), '1024x1024', 0)).toBeNull()
  })
})
