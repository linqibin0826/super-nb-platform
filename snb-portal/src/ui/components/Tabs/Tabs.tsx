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
            '-mb-px border-b-2 px-6 py-3.5 font-sans text-base transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus',
            active === item.id
              ? // 浅色档橙必须走压深版 --snb-safety：primary-600 #DB4F00 压纸只有 3.54:1，
                // 当不了 16px 正文；深色档保持 primary-400 原样
                'border-snb-safety text-snb-safety dark:border-primary-500 dark:text-primary-400'
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
