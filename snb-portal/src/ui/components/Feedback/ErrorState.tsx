import type { ReactNode } from 'react'
import { Button } from '../Button/Button'
import { StatePanel } from './panel'

export interface ErrorStateProps {
  title: ReactNode
  /**
   * 文案三要素，缺一不可：哪件事失败了 / 责任在谁 / 用户的东西丢没丢。
   * 例：「刚才请求上机记录失败了，是机房这边的问题，你的数据没丢。」
   */
  description?: ReactNode
  /** 图标，默认叹号 */
  icon?: ReactNode
  /** 🚨 必填：错误态一律带「再试一次」，没有重试出口的错误态不许上线 */
  onRetry: () => void
  retryLabel?: ReactNode
  /** 追加的次要出口（如「看机房公告」），排在重试右侧 */
  action?: ReactNode
  className?: string
}

/**
 * 错误态（GlobalParts v3 §05，与 EmptyState 配对、同骨架异色）。
 * 请求失败/超时/解析失败一律走这里，绝不能降级成 EmptyState 的「暂无数据」。
 */
export function ErrorState({
  title,
  description,
  icon = '!',
  onRetry,
  retryLabel = '再试一次',
  action,
  className,
}: ErrorStateProps) {
  return (
    <StatePanel
      tone="error"
      icon={icon}
      title={title}
      description={description}
      actions={
        <>
          <Button variant="secondary" onClick={onRetry}>
            {retryLabel}
          </Button>
          {action}
        </>
      }
      className={className}
    />
  )
}
