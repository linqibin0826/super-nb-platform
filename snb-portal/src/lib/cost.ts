// 拷自 sub2api fork feat/image-playground@4f873b56 frontend/src/features/playground/lib/ —— 框架无关，与 fork 同步演进须手动
// 估价公式镜像后端：
//   CalculateImageCost = 分组档位单价 × 张数 × 倍率（backend/internal/service/billing_service.go:1181）
//   倍率 = image_rate_independent ? max(0, image_rate_multiplier)
//         : (用户自定义分组倍率 ?? group.rate_multiplier)   （image_billing_multiplier.go:3）
// 分组未配置该档单价（null）时后端回退 LiteLLM 默认价，前端不可知 → 返回 null，UI 隐藏估价。
// 价格显式配置为 0 是合法免费档 → 估价 $0.00（与后端 getImageUnitPrice 语义一致）。
// ⚠️ 改动前先对照后端；禁止 import vue/pinia/vue-router（画布子应用复用）。
import { billingTierOrDefault, type BillingTier } from './sizes'

export interface ImageCostGroup {
  rate_multiplier: number
  image_rate_independent: boolean
  image_rate_multiplier: number
  image_price_1k: number | null
  image_price_2k: number | null
  image_price_4k: number | null
}

export function estimateCost(
  group: ImageCostGroup | null | undefined,
  size: string,
  n: number,
  userGroupRate?: number
): number | null {
  if (!group || n <= 0) return null
  const price = tierPrice(group, billingTierOrDefault(size))
  if (price === null || price === undefined) return null
  const multiplier = group.image_rate_independent
    ? Math.max(0, group.image_rate_multiplier)
    : (userGroupRate ?? group.rate_multiplier)
  return price * n * multiplier
}

function tierPrice(group: ImageCostGroup, tier: BillingTier): number | null {
  switch (tier) {
    case '1K':
      return group.image_price_1k
    case '2K':
      return group.image_price_2k
    case '4K':
      return group.image_price_4k
  }
}
