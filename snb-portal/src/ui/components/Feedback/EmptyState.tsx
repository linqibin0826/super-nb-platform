import type { ReactNode } from 'react'
import { StatePanel } from './panel'

export interface EmptyStateProps {
  title: ReactNode
  /** 一句人话：说清这里将来会装什么 */
  description?: ReactNode
  /** 图标，默认空集号 ∅ */
  icon?: ReactNode
  /**
   * 动作区：必须指向「能把这里填满的那个动作」（如「去接第一单」）。
   * 🚨 不许只写「暂无数据」了事——那是把死路当结论。
   */
  action?: ReactNode
  className?: string
}

/**
 * 空态（GlobalParts v3 §05，与 ErrorState 配对）。
 * 判定铁律：**请求成功且结果为空** → 空态；请求失败/超时/解析失败 → ErrorState。
 * 二者不可互换——接口挂了显示「暂无数据」是全站最危险的一类谎报。
 */
export function EmptyState({ title, description, icon = '∅', action, className }: EmptyStateProps) {
  return (
    <StatePanel
      tone="empty"
      icon={icon}
      title={title}
      description={description}
      actions={action}
      className={className}
    />
  )
}
