import { motion, useReducedMotion } from 'motion/react'

export interface ChipOption {
  value: string
  label: string
  title?: string
  /** 单档禁用（如某比例下越界的分辨率档）：显示但置灰不可点 */
  disabled?: boolean
}

/** 配置行渐次入场（随父容器 staggerChildren 依次浮现） */
export const rowVariants = { hidden: { opacity: 0, y: 6 }, show: { opacity: 1, y: 0 } }

/** 直选胶囊组（radiogroup）：选中态是一块纸白「墨」，切换时用 layoutId 在组内滑动过去。
 *  🚨 2026-07-29 起选中一律纸白填充 + 沥青字——白字压橙实测 2.61:1 读不出，
 *  而且这些胶囊表达的多半只是**默认值**，比真正的行动按钮还响是错的层级。 */
export function OptionChips(props: {
  groupId: string
  options: ChipOption[]
  value: string
  disabled?: boolean
  onSelect: (value: string) => void
  'aria-label': string
  /** 长文案选项（如 API Key 名）截断 */
  truncate?: boolean
}) {
  const reduceMotion = useReducedMotion()
  return (
    <div role="radiogroup" aria-label={props['aria-label']} className="flex flex-wrap gap-1.5">
      {props.options.map((o) => {
        const active = o.value === props.value
        return (
          <button
            key={o.value}
            type="button"
            role="radio"
            aria-checked={active}
            disabled={o.disabled ?? props.disabled}
            title={o.title ?? (props.truncate ? o.label : undefined)}
            onClick={() => props.onSelect(o.value)}
            className={`relative whitespace-nowrap rounded-full border px-3 py-1.5 text-[12.5px] transition-colors duration-[120ms] ease-[cubic-bezier(0.2,0,0,1)] focus:outline-none focus-visible:ring-2 focus-visible:ring-paper/60 disabled:cursor-not-allowed disabled:opacity-60 ${
              active
                ? 'border-transparent font-semibold text-snb-cta-fg'
                : 'border-snb-hairline-strong text-snb-t2 hover:border-[rgba(239,235,228,0.4)] hover:text-snb-t1'
            }`}
          >
            {active &&
              (reduceMotion ? (
                <span aria-hidden="true" className="absolute inset-0 rounded-full bg-snb-cta" />
              ) : (
                <motion.span
                  aria-hidden="true"
                  layoutId={`chip-ink-${props.groupId}`}
                  transition={{ type: 'spring', stiffness: 520, damping: 40 }}
                  className="absolute inset-0 rounded-full bg-snb-cta"
                />
              ))}
            <span className={`relative z-[1] ${props.truncate ? 'inline-block max-w-[220px] truncate align-top' : ''}`}>
              {o.label}
            </span>
          </button>
        )
      })}
    </div>
  )
}
