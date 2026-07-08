import { motion } from 'motion/react'
import type { ReactNode } from 'react'
import { rowVariants } from './OptionChips'

/** 配置区一行：左标签列 + 右控件区。留白分组，不画分隔线 */
export function ConfigRow({ label, children }: { label?: string; children: ReactNode }) {
  return (
    <motion.div variants={rowVariants} className="flex items-start gap-3.5 py-2.5">
      {label !== undefined && (
        <span className="w-[52px] shrink-0 pt-[7px] text-xs tracking-[0.06em] text-snb-t3">{label}</span>
      )}
      <div className="min-w-0 flex-1">{children}</div>
    </motion.div>
  )
}
