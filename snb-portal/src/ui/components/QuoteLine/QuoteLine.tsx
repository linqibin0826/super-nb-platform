import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface QuoteLineProps {
  label: ReactNode
  value: ReactNode
  note?: ReactNode
  className?: string
}

/** 报价行：衬线点引线 + 等宽大字（studio 生图合计行同款） */
export function QuoteLine({ label, value, note, className }: QuoteLineProps) {
  return (
    <p className={cx('flex min-w-0 flex-1 items-baseline text-sm text-snb-t3', className)}>
      <span className="shrink-0">{label}</span>
      <span
        className="mx-3 hidden min-w-[2rem] flex-1 self-end border-b-2 border-dotted border-snb-hairline-strong sm:block"
        style={{ marginBottom: '0.4em' }}
        aria-hidden="true"
      />
      <span className="ml-2 shrink-0 font-mono text-xl font-semibold tracking-tight text-snb-t1 sm:ml-0">
        {value}
      </span>
      {note && <span className="ml-2 hidden shrink-0 text-xs text-snb-t3 md:inline">{note}</span>}
    </p>
  )
}
