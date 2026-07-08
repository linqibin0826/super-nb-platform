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

/** 直选胶囊组（radiogroup）：选中态是一块赤陶「墨」，切换时用 layoutId 在组内滑动过去 */
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
            className={`relative whitespace-nowrap rounded-full border px-3 py-1.5 text-[12.5px] transition-colors duration-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 disabled:cursor-not-allowed disabled:opacity-60 ${
              active
                ? 'border-transparent font-medium text-white'
                : 'border-snb-hairline bg-snb-elv/60 text-snb-t2 hover:border-snb-t3 hover:text-snb-t1'
            }`}
          >
            {active &&
              (reduceMotion ? (
                <span aria-hidden="true" className="absolute inset-0 rounded-full bg-primary-500" />
              ) : (
                <motion.span
                  aria-hidden="true"
                  layoutId={`chip-ink-${props.groupId}`}
                  transition={{ type: 'spring', stiffness: 520, damping: 40 }}
                  className="absolute inset-0 rounded-full bg-primary-500"
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
