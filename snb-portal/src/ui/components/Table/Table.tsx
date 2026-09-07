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
    <div className={cx('overflow-x-auto rounded-2xl border border-snb-hairline', className)}>
      <table className="w-full text-sm [&_tbody_tr:last-child_td]:border-b-0">
        <thead>
          <tr>
            {columns.map((c) => (
              <th
                key={c.key}
                // 表头压 well：t3 压 well 浅色只有 4.27:1，一律用 t2
                className="border-b border-snb-hairline bg-snb-well px-4 py-3 text-left text-[13px] font-medium text-snb-t2"
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
              className="transition-colors duration-quick ease-snb-quick hover:bg-snb-well/60"
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
