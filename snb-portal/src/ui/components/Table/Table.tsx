import type { ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface TableColumn {
  key: string
  title: ReactNode
}

export interface TableProps {
  columns: TableColumn[]
  rows: Array<Record<string, ReactNode>>
  rowKey?: (row: Record<string, ReactNode>, index: number) => string
  className?: string
}

export function Table({ columns, rows, rowKey, className }: TableProps) {
  return (
    <div className={cx('overflow-x-auto rounded-xl border border-snb-hairline-strong', className)}>
      <table className="w-full text-sm [&_tbody_tr:last-child_td]:border-b-0">
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                className="border-b border-snb-hairline-strong bg-black/[0.03] px-4 py-3 text-left font-medium text-snb-t2 dark:bg-white/[0.07]"
              >
                {c.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr
              key={rowKey ? rowKey(row, i) : i}
              className="transition-colors duration-150 hover:bg-black/[0.02] dark:hover:bg-white/[0.04]"
            >
              {columns.map((c) => (
                <td key={c.key} className="border-b border-snb-hairline px-4 py-3 text-snb-t2">
                  {row[c.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
