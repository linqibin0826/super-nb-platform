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
    <div role="tablist" className={cx('flex border-b border-snb-hairline', className)}>
      {items.map((item) => (
        <button
          key={item.id}
          role="tab"
          aria-selected={active === item.id}
          className={cx(
            '-mb-px border-b-2 px-6 py-3.5 text-base transition-[color,border-color] duration-quick ease-snb-quick focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
            active === item.id
              ? 'border-snb-safety font-semibold text-snb-t1'
              : 'border-transparent text-snb-t3 hover:text-snb-t1'
          )}
          onClick={() => onSelect(item.id)}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
