import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {}

export function Skeleton({ className, ...rest }: SkeletonProps) {
  return (
    <div
      // 骨架色 = 抬升面 elv（深 #242A33 / 浅 #E5DFD3，压卡片面 1.26–1.28 的亮度差：
      // 看得见形状但绝不抢眼）。🪦 gray-200 是恒暗前的遗产，与暖纸色系不合
      className={cx('animate-pulse rounded-xl bg-snb-elv motion-reduce:animate-none', className)}
      aria-hidden="true"
      {...rest}
    />
  )
}
