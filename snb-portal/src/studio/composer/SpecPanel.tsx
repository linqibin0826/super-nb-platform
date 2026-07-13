import { motion, useReducedMotion } from 'motion/react'
import { OptionChips, rowVariants } from './OptionChips'
import { ConfigRow } from './ConfigRow'
import { RatioIcon } from './RatioIcon'
import type { LocalSpec } from './spec'
import { billingTierOrDefault, isValidGptImageSize, RATIO_OPTIONS, validTiersForRatio, type BillingTier } from '../../lib/sizes'
import { t } from '../../i18n'
import type { EligibleKey } from '../../types'
import { displayName, sizeModeOf } from '../../lib/modelFamilies'

export interface SpecPanelProps {
  spec: LocalSpec
  /** 上层 size 字符串（自定义/自动时用于显示计费档、宽×高） */
  sizeText: string
  /** 登录但无可用 Key：换引导文案 */
  showEmptyState: boolean
  /** 是否已登录（决定出 Key 选择器 or 游客引导） */
  hasUser: boolean
  n: number
  eligible: EligibleKey[]
  selectedKeyId: number | null
  model: string
  selectableModels: string[]
  updateSpec: (patch: Partial<LocalSpec>) => void
  onChangeN: (n: number) => void
  onChangeKey: (id: number) => void
  onChangeModel: (model: string) => void
}

/** 创作票据展开的配置区：模型/比例（形状/自动/自定义宽高）/画质档/张数/Key/游客提示。
 *  从 Composer.tsx 抽出；状态仍在 Composer，本组件只做展示与回调。 */
export function SpecPanel(props: SpecPanelProps) {
  const reduceMotion = useReducedMotion()
  const { spec, sizeText, showEmptyState, hasUser } = props

  const ratioBtnClass = (active: boolean) =>
    `relative flex items-center gap-1.5 whitespace-nowrap rounded-xl border px-3 py-1.5 text-[12.5px] transition-colors duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 disabled:cursor-not-allowed disabled:opacity-60 ${
      active
        ? 'border-transparent font-medium text-white'
        : 'border-snb-hairline bg-snb-elv/60 text-snb-t2 hover:border-snb-t3 hover:text-snb-t1'
    }`

  const ratioInk = (active: boolean) =>
    active &&
    (reduceMotion ? (
      <span aria-hidden="true" className="absolute inset-0 rounded-xl bg-primary-500" />
    ) : (
      <motion.span
        aria-hidden="true"
        layoutId="chip-ink-ratio"
        transition={{ type: 'spring', stiffness: 520, damping: 40 }}
        className="absolute inset-0 rounded-xl bg-primary-500"
      />
    ))

  if (showEmptyState) {
    return (
      <div className="px-4 pt-4 sm:px-5">
        <p className="text-sm font-medium text-snb-t1">{t('playground.empty.title')}</p>
        <p className="mt-1.5 text-[13px] leading-relaxed text-snb-t3">
          {t('playground.empty.description')}
        </p>
      </div>
    )
  }

  // 该比例下有效的分辨率档（custom 不适用具体档，全禁用、只读高亮归档结果）
  const validTiers = spec.ratio === 'custom' ? [] : validTiersForRatio(spec.ratio)

  return (
    <motion.div
      className="max-h-[min(60vh,calc(100dvh-380px))] overflow-y-auto px-4 pt-2.5 sm:px-5"
      initial={reduceMotion ? false : 'hidden'}
      animate="show"
      variants={reduceMotion ? undefined : { show: { transition: { staggerChildren: 0.045 } } }}
    >
      {/* 模型：按所选 key 动态列出可选生图模型（家族友好名） */}
      {hasUser && props.selectableModels.length > 0 && (
        <ConfigRow label={t('playground.form.model')}>
          <OptionChips
            groupId="model"
            aria-label={t('playground.form.model')}
            options={props.selectableModels.map((m) => ({ value: m, label: displayName(m) }))}
            value={props.model}
            onSelect={(v) => props.onChangeModel(v)}
          />
        </ConfigRow>
      )}

      {/* 尺寸/画质档仅 gpt-image 家族适用；grok 固定 1024²、无这两轴时整段隐藏 */}
      {sizeModeOf(props.model) === 'free' && (
        <>
          {/* 比例：形状直选 + 自动 + 自定义宽高 */}
          <ConfigRow label={t('studio.composer.ratio')}>
            <div
              role="radiogroup"
              aria-label={t('studio.composer.ratio')}
              className="flex flex-wrap gap-1.5"
            >
              {RATIO_OPTIONS.map((r) => {
                const active = spec.ratio === r.value
                return (
                  <button
                    key={r.value}
                    type="button"
                    role="radio"
                    aria-checked={active}
                    onClick={() => props.updateSpec({ ratio: r.value })}
                    className={ratioBtnClass(active)}
                  >
                    {ratioInk(active)}
                    <RatioIcon w={r.w} h={r.h} />
                    <span className="relative z-[1]">{r.value}</span>
                  </button>
                )
              })}
              <button
                type="button"
                role="radio"
                aria-checked={spec.ratio === 'custom'}
                onClick={() => props.updateSpec({ ratio: 'custom' })}
                className={ratioBtnClass(spec.ratio === 'custom')}
              >
                {ratioInk(spec.ratio === 'custom')}
                <span
                  aria-hidden="true"
                  className="relative z-[1] inline-block h-[13px] w-[13px] rounded-[3px] border-[1.5px] border-dashed border-current"
                />
                <span className="relative z-[1]">{t('studio.composer.ratioCustom')}</span>
              </button>
            </div>

            {spec.ratio === 'custom' && (
              <div className="mt-2.5 flex flex-wrap items-center gap-2 text-[12.5px] text-snb-t3">
                <label className="flex items-center gap-1.5">
                  {t('studio.composer.width')}
                  <input
                    type="number"
                    min={128}
                    max={4096}
                    step={64}
                    value={spec.w}
                    onChange={(e) => props.updateSpec({ w: e.target.value })}
                    className="w-[80px] rounded-lg border border-snb-hairline bg-snb-elv/60 px-2 py-1 font-mono text-[12.5px] text-snb-t1 outline-none transition-colors focus:border-primary-500"
                  />
                </label>
                <span aria-hidden="true">×</span>
                <label className="flex items-center gap-1.5">
                  {t('studio.composer.height')}
                  <input
                    type="number"
                    min={128}
                    max={4096}
                    step={64}
                    value={spec.h}
                    onChange={(e) => props.updateSpec({ h: e.target.value })}
                    className="w-[80px] rounded-lg border border-snb-hairline bg-snb-elv/60 px-2 py-1 font-mono text-[12.5px] text-snb-t1 outline-none transition-colors focus:border-primary-500"
                  />
                </label>
                <span>px</span>
                {!isValidGptImageSize(`${spec.w}x${spec.h}`) && (
                  <p className="mt-1.5 w-full text-[12px] text-red-500">{t('studio.composer.customInvalid')}</p>
                )}
              </div>
            )}
          </ConfigRow>

          {/* 画质：1K/2K/4K 独立一档；自动尺寸不适用，自定义时高亮按宽高归的档（只读） */}
          <ConfigRow label={t('studio.composer.resolution')}>
            <OptionChips
              groupId="tier"
              aria-label={t('studio.composer.resolution')}
              // 始终列全三档；该比例下越界的档显示但禁用（custom 时全档禁用、只读高亮归档结果）
              options={(['1K', '2K', '4K'] as const).map((v) => ({
                value: v,
                label: v,
                disabled: spec.ratio === 'custom' || !validTiers.includes(v),
                title:
                  spec.ratio !== 'custom' && !validTiers.includes(v) ? t('studio.composer.tierUnavailable') : undefined,
              }))}
              value={spec.ratio === 'custom' ? billingTierOrDefault(sizeText) : spec.tier}
              onSelect={(v) => props.updateSpec({ tier: v as BillingTier })}
            />
          </ConfigRow>
        </>
      )}

      <ConfigRow label={t('playground.form.count')}>
        <OptionChips
          groupId="count"
          aria-label={t('playground.form.count')}
          options={[1, 2, 3, 4].map((v) => ({
            value: String(v),
            label: t('studio.editor.countUnit', { n: v }),
          }))}
          value={String(props.n)}
          onSelect={(v) => props.onChangeN(Number(v))}
        />
      </ConfigRow>

      {hasUser && (
        <ConfigRow label={t('playground.form.key')}>
          <OptionChips
            groupId="key"
            aria-label={t('playground.form.key')}
            truncate
            options={props.eligible.map((e) => ({
              value: String(e.key.id),
              label: `${e.key.name} · ${e.group.name}`,
            }))}
            value={String(props.selectedKeyId ?? '')}
            onSelect={(v) => props.onChangeKey(Number(v))}
          />
        </ConfigRow>
      )}

      {!hasUser && (
        <motion.p
          variants={rowVariants}
          className="py-2.5 text-[13px] leading-relaxed text-snb-t3"
        >
          {t('studio.editor.guestLead')}
        </motion.p>
      )}
    </motion.div>
  )
}
