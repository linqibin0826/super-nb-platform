import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface TabItem {
  id: string
  label: ReactNode
}

export interface TabsProps {
  items: TabItem[]
  active: string
  onSelect: (id: string) => void
  className?: string
}

export function Tabs({ items, active, onSelect, className }: TabsProps) {
  return (
    <div role="tablist" className={cx('flex border-b border-snb-hairline-strong', className)}>
      {items.map((item) => (
        <button
          key={item.id}
          role="tab"
          aria-selected={active === item.id}
          className={cx(
            '-mb-px border-b-2 px-6 py-3.5 font-display text-base transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50',
            active === item.id
              ? 'border-primary-500 text-primary-600 dark:text-primary-400'
              : 'border-transparent text-snb-t3 hover:text-snb-t2'
          )}
          onClick={() => onSelect(item.id)}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
