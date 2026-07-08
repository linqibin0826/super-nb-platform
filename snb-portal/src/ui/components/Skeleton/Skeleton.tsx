import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {}

export function Skeleton({ className, ...rest }: SkeletonProps) {
  return (
    <div
      className={cx('animate-pulse rounded-xl bg-gray-200 motion-reduce:animate-none dark:bg-dark-700', className)}
      aria-hidden="true"
      {...rest}
    />
  )
}
