// 比例/画质本地态与 size 字符串的互转（纯函数）：对上层仍是一个 size 字符串（契约不变），
// 控件侧拆成两轴。从 Composer.tsx 抽出，Composer 与 SpecPanel 共用单一来源，避免类型漂移。
import {
  billingTierOrDefault,
  clampTierForRatio,
  matchRatioTier,
  sizeForRatio,
  type AspectRatio,
  type BillingTier,
} from '../../lib/sizes'

export type RatioChoice = AspectRatio | 'custom'

export interface LocalSpec {
  ratio: RatioChoice
  tier: BillingTier
  /** 自定义宽高的输入原文（允许敲一半），合法才合成 size 上抛 */
  w: string
  h: string
}

export function deriveFromSize(size: string): LocalSpec {
  const matched = matchRatioTier(size)
  // 收敛画质档：历史/灵感里的越界组合（如 1:1 4K）反推后落到该比例下的有效档，别再复现无效尺寸
  if (matched) return { ratio: matched.ratio, tier: clampTierForRatio(matched.ratio, matched.tier), w: '1024', h: '1024' }
  const m = /^(\d+)x(\d+)$/.exec(size.trim())
  if (m) return { ratio: 'custom', tier: billingTierOrDefault(size), w: m[1], h: m[2] }
  return { ratio: '1:1', tier: '1K', w: '1024', h: '1024' }
}

export function composeSize(spec: LocalSpec): string | null {
  if (spec.ratio === 'custom') {
    const w = Number.parseInt(spec.w, 10)
    const h = Number.parseInt(spec.h, 10)
    // 正整数即产出；是否满足 gpt-image-2 约束由上层 isValidGptImageSize 判定并决定能否生成
    if (!Number.isInteger(w) || !Number.isInteger(h) || w <= 0 || h <= 0) return null
    return `${w}x${h}`
  }
  return sizeForRatio(spec.ratio, spec.tier)
}
