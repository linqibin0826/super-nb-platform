import { describe, expect, it } from 'vitest'
import {
  SIZE_PRESETS,
  DEFAULT_SIZE,
  RATIO_OPTIONS,
  classifyBillingTier,
  billingTierOrDefault,
  matchRatioTier,
  sizeForRatio,
  isValidGptImageSize,
  validTiersForRatio,
  clampTierForRatio,
} from '../sizes'

describe('sizes', () => {
  it('预设表完整且默认值在表内', () => {
    const values = SIZE_PRESETS.map((p) => p.value)
    expect(values).toEqual([
      '1024x1024',
      '1536x1024',
      '1024x1536',
      '2048x2048',
      '2048x1152',
      '3840x2160',
      '2160x3840',
    ])
    expect(values).toContain(DEFAULT_SIZE)
    for (const p of SIZE_PRESETS) expect(p.labelKey).toMatch(/^playground\.sizes\./)
  })

  // 镜像后端 ClassifyImageBillingTier（image_billing_size.go:28）
  it.each([
    ['1024x1024', '1K'],
    ['1536x1024', '2K'], // ⚠️ 最长边 1536 → 2K，spec §5.2 的坑
    ['1024x1536', '2K'],
    ['2048x2048', '2K'],
    ['2048x1152', '2K'],
    ['3840x2160', '4K'],
    ['2160x3840', '4K'],
    ['1k', '1K'],
    ['2K', '2K'],
    ['4k', '4K'],
    ['800x600', '1K'],
    ['4096x4096', '4K'],
  ] as const)('classifyBillingTier(%s) → %s', (size, tier) => {
    expect(classifyBillingTier(size)).toBe(tier)
  })

  it.each(['auto', '', '  ', 'abc', '1024x', 'x768', '0x100', '-1x5'])(
    'classifyBillingTier(%j) → null',
    (size) => {
      expect(classifyBillingTier(size)).toBeNull()
    }
  )

  it('billingTierOrDefault 对不可归档值兜底 2K（镜像 NormalizeImageBillingTierOrDefault）', () => {
    expect(billingTierOrDefault('auto')).toBe('2K')
    expect(billingTierOrDefault('1024x1024')).toBe('1K')
  })

  it.each([
    ['1:1', '1K', '1024x1024'],
    ['1:1', '2K', '2048x2048'],
    ['16:9', '2K', '2048x1152'], // 与既有预设重合
    ['16:9', '4K', '3840x2160'],
    ['9:16', '4K', '2160x3840'],
    ['3:2', '2K', '2048x1344'], // 短边就近取整到 64 倍数
  ] as const)('sizeForRatio(%s, %s) → %s', (ratio, tier, size) => {
    expect(sizeForRatio(ratio, tier)).toBe(size)
  })

  it('sizeForRatio 产出的尺寸都落在对应计费档内', () => {
    for (const r of RATIO_OPTIONS) {
      for (const tier of ['1K', '2K', '4K'] as const) {
        expect(classifyBillingTier(sizeForRatio(r.value, tier))).toBe(tier)
      }
    }
  })

  it('matchRatioTier 与 sizeForRatio 互逆；自定义/auto 返回 null', () => {
    expect(matchRatioTier(sizeForRatio('3:4', '2K'))).toEqual({ ratio: '3:4', tier: '2K' })
    expect(matchRatioTier('1024x1536')).toBeNull() // 旧预设不在比例矩阵内 → 走自定义
    expect(matchRatioTier('auto')).toBeNull()
  })
})

describe('gpt-image-2 尺寸约束', () => {
  it('有效尺寸通过（含默认 9:16 2K 与卡满上限的 4K 宽屏）', () => {
    expect(isValidGptImageSize('1024x1024')).toBe(true)
    expect(isValidGptImageSize('1152x2048')).toBe(true) // 默认
    expect(isValidGptImageSize('3840x2160')).toBe(true) // 正好卡 829 万像素上限
  })
  it('超像素上限的非宽屏 4K 无效（含生产坑 1:1 4K）', () => {
    expect(isValidGptImageSize('3840x3840')).toBe(false) // 1474 万像素
    expect(isValidGptImageSize('3840x2560')).toBe(false) // 3:2 4K
  })
  it('低于像素下限的 1K 宽屏无效', () => {
    expect(isValidGptImageSize('1024x576')).toBe(false)
  })
  it('非 16 倍数 / 超最大边 / 超 3:1 比例无效', () => {
    expect(isValidGptImageSize('1000x1000')).toBe(false) // 非 16 倍数
    expect(isValidGptImageSize('4096x1024')).toBe(false) // 最大边 > 3840
    expect(isValidGptImageSize('2048x512')).toBe(false) // 4:1 > 3:1
  })
  it('auto / 非 WxH 一律 false（不在此判定）', () => {
    expect(isValidGptImageSize('auto')).toBe(false)
    expect(isValidGptImageSize('')).toBe(false)
  })
})

describe('validTiersForRatio / clampTierForRatio', () => {
  it('宽屏只给 2K/4K，其余比例给 1K/2K', () => {
    expect(validTiersForRatio('16:9')).toEqual(['2K', '4K'])
    expect(validTiersForRatio('9:16')).toEqual(['2K', '4K'])
    expect(validTiersForRatio('1:1')).toEqual(['1K', '2K'])
    expect(validTiersForRatio('3:2')).toEqual(['1K', '2K'])
  })
  it('有效档保持；方形 4K 落 2K；宽屏 1K 抬 2K', () => {
    expect(clampTierForRatio('1:1', '2K')).toBe('2K')
    expect(clampTierForRatio('1:1', '4K')).toBe('2K')
    expect(clampTierForRatio('16:9', '1K')).toBe('2K')
  })
})
