// 拷自 sub2api fork feat/image-playground@4f873b56 frontend/src/features/playground/lib/ —— 框架无关，与 fork 同步演进须手动
// ⚠️ 本文件镜像后端 backend/internal/service/image_billing_size.go 的归档逻辑，
// 改动前先对照后端；禁止 import vue/pinia/vue-router（画布子应用复用）。
export type BillingTier = '1K' | '2K' | '4K'

export interface SizePreset {
  /** 原样传给 /v1/images/generations 的 size 值 */
  value: string
  /** i18n key（playground.sizes.*） */
  labelKey: string
}

export const SIZE_PRESETS: SizePreset[] = [
  { value: '1024x1024', labelKey: 'playground.sizes.square1024' },
  { value: '1536x1024', labelKey: 'playground.sizes.landscape1536' },
  { value: '1024x1536', labelKey: 'playground.sizes.portrait1536' },
  { value: '2048x2048', labelKey: 'playground.sizes.square2048' },
  { value: '2048x1152', labelKey: 'playground.sizes.landscape2048' },
  { value: '3840x2160', labelKey: 'playground.sizes.landscape4k' },
  { value: '2160x3840', labelKey: 'playground.sizes.portrait4k' },
]

export const DEFAULT_SIZE = '1024x1024'

/** 无法归档（auto/空/非法）返回 null；后端计费对这类值兜底 2K。 */
export function classifyBillingTier(size: string): BillingTier | null {
  const normalized = size.trim().toLowerCase()
  switch (normalized) {
    case '':
    case 'auto':
      return null
    case '1k':
      return '1K'
    case '2k':
      return '2K'
    case '4k':
      return '4K'
    case '2048x2048':
    case '2048x1152':
      return '2K'
    case '3840x2160':
    case '2160x3840':
      return '4K'
  }
  const parts = normalized.split('x')
  if (parts.length !== 2) return null
  const width = parsePositiveInt(parts[0])
  const height = parsePositiveInt(parts[1])
  if (width === null || height === null) return null
  const maxEdge = Math.max(width, height)
  if (maxEdge <= 1024) return '1K'
  if (maxEdge <= 2048) return '2K'
  return '4K'
}

export function billingTierOrDefault(size: string): BillingTier {
  return classifyBillingTier(size) ?? '2K'
}

function parsePositiveInt(raw: string): number | null {
  const trimmed = raw.trim()
  if (!/^\d+$/.test(trimmed)) return null
  const value = Number.parseInt(trimmed, 10)
  return value > 0 ? value : null
}

// ——— 比例 × 画质 → 尺寸（创作栏直选控件用；上层 size 字符串契约不变） ———

export type AspectRatio = '1:1' | '4:3' | '3:4' | '3:2' | '2:3' | '16:9' | '9:16'

export const RATIO_OPTIONS: Array<{ value: AspectRatio; w: number; h: number }> = [
  { value: '1:1', w: 1, h: 1 },
  { value: '4:3', w: 4, h: 3 },
  { value: '3:4', w: 3, h: 4 },
  { value: '3:2', w: 3, h: 2 },
  { value: '2:3', w: 2, h: 3 },
  { value: '16:9', w: 16, h: 9 },
  { value: '9:16', w: 9, h: 16 },
]

/** 各画质档的长边像素：1K/2K 取整档边长；4K 对齐既有预设 3840×2160（UHD），
 *  三档均落在 classifyBillingTier 对应档内（长边 ≤1024 / ≤2048 / >2048） */
const TIER_LONG_EDGE: Record<BillingTier, number> = { '1K': 1024, '2K': 2048, '4K': 3840 }

/** 比例 + 画质 → size 字符串：长边取档位边长；短边按比例除得尽就取精确值
 *  （保住 3840×2160 这类已知可用预设），除不尽才就近取整到 64 的倍数。
 *  与既有预设自然重合：16:9@2K=2048x1152、16:9@4K=3840x2160、1:1 全档等。 */
export function sizeForRatio(ratio: AspectRatio, tier: BillingTier): string {
  const def = RATIO_OPTIONS.find((r) => r.value === ratio)
  if (!def) return DEFAULT_SIZE
  const long = TIER_LONG_EDGE[tier]
  if (def.w === def.h) return `${long}x${long}`
  const exact = (long * Math.min(def.w, def.h)) / Math.max(def.w, def.h)
  const short = Number.isInteger(exact) ? exact : Math.max(64, Math.round(exact / 64) * 64)
  return def.w > def.h ? `${long}x${short}` : `${short}x${long}`
}

/** size 字符串反推（比例, 画质）；对不上任何组合（自定义/auto/非法）返回 null */
export function matchRatioTier(size: string): { ratio: AspectRatio; tier: BillingTier } | null {
  for (const r of RATIO_OPTIONS) {
    for (const tier of ['1K', '2K', '4K'] as const) {
      if (sizeForRatio(r.value, tier) === size) return { ratio: r.value, tier }
    }
  }
  return null
}

// ——— gpt-image-2 官方尺寸约束（2026-07 核实）：任意 WxH，但最大边 ≤3840、边长都是 16 的倍数、
// 长短比 ≤3:1、总像素 655,360~8,294,400。UI 据此只暴露有效的「比例×画质」组合，
// 不再让用户选到会被上游丢弃的尺寸（如 1:1 4K=3840×3840 越像素上限，是 2026-07-05 生产坑）。———
export const GPT_IMAGE_MAX_EDGE = 3840
export const GPT_IMAGE_MIN_PIXELS = 655360
export const GPT_IMAGE_MAX_PIXELS = 8294400

const TIER_ORDER: BillingTier[] = ['1K', '2K', '4K']

/** size 是否满足 gpt-image-2 约束；auto / 非 WxH 返回 false（不在此判定，各自另管） */
export function isValidGptImageSize(size: string): boolean {
  const m = /^(\d+)x(\d+)$/.exec(size.trim())
  if (!m) return false
  const w = Number(m[1])
  const h = Number(m[2])
  if (w % 16 !== 0 || h % 16 !== 0) return false
  if (Math.max(w, h) > GPT_IMAGE_MAX_EDGE) return false
  if (Math.max(w, h) / Math.min(w, h) > 3) return false
  const px = w * h
  return px >= GPT_IMAGE_MIN_PIXELS && px <= GPT_IMAGE_MAX_PIXELS
}

/** 某比例下有效的画质档（结果 size 满足 gpt-image-2 约束）；供 UI 只显示可用档 */
export function validTiersForRatio(ratio: AspectRatio): BillingTier[] {
  return TIER_ORDER.filter((tier) => isValidGptImageSize(sizeForRatio(ratio, tier)))
}

/** 把画质档收敛到该比例下有效的档：优先保留 ≤ 原档的最高有效档，否则取最低有效档 */
export function clampTierForRatio(ratio: AspectRatio, tier: BillingTier): BillingTier {
  const valid = validTiersForRatio(ratio)
  if (valid.includes(tier)) return tier
  const ci = TIER_ORDER.indexOf(tier)
  const below = valid.filter((t) => TIER_ORDER.indexOf(t) <= ci)
  return below.length ? below[below.length - 1] : valid[0]
}
