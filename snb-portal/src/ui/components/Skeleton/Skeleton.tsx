import type { HTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface SkeletonProps extends HTMLAttributes<HTMLDivElement> {}

export function Skeleton({ className, ...rest }: SkeletonProps) {
  return (
    <div
      // 骨架色 = 抬升面 elv（深 #242A33 / 浅 #E5DFD3，压卡片面 1.26–1.28 的亮度差：
      // 看得见形状但绝不抢眼）。🪦 gray-200 是恒暗前的遗产，与暖纸色系不合
      //
      // 🪦 2026-07-30 摘掉 animate-pulse（站长拍板）：**骨架不闪**是本系统的纪律
      // （「数据不闪，骨架也不闪」），同套里的 GuestGate 一直是静态块、还有断言钉着。
      // 两个组件对同一件事给出相反答案，是设计系统里最坏的一种不一致——用哪个全看
      // 抄到了谁。现在统一到不闪：骨架靠**形状**占位，不靠动来提示「在加载」。
      className={cx('rounded-xl bg-snb-elv', className)}
      aria-hidden="true"
      {...rest}
    />
  )
}
