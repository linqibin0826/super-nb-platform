import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  /** 悬停浮起（fork .card-hover） */
  hover?: boolean
}

export function Card({ hover = false, className, ...rest }: CardProps) {
  return (
    <div
      className={cx(
        'rounded-2xl border border-snb-hairline bg-snb-panel shadow-card transition-all duration-300',
        hover && 'hover:-translate-y-0.5 hover:shadow-card-hover',
        className
      )}
      {...rest}
    />
  )
}

export function CardHeader({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx('border-b border-snb-hairline px-6 py-4', className)} {...rest} />
}

export function CardBody({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx('p-6', className)} {...rest} />
}

export function CardFooter({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx('border-t border-snb-hairline px-6 py-4', className)} {...rest} />
}
