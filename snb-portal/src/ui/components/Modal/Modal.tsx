import { useEffect, type ReactNode } from 'react'
import { cx } from '../../lib/cx'

export interface ModalProps {
  open: boolean
  onClose: () => void
  title?: ReactNode
  footer?: ReactNode
  children: ReactNode
  className?: string
}

export function Modal({ open, onClose, title, footer, children, className }: ModalProps) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null
  return (
    <div
      // 🚨 遮罩两档都是暗的：白天档不等于遮罩变白（变白 = 底下页面看着像没关掉）。
      // 全屏对话框比导航浮卡压得更重，故保留纯黑 60%，不走 --snb-mask
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-2 sm:p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        className={cx(
          // 对话框面：深色用抬升面 elv（比 panel 更亮=浮起），浅色用 panel #FBF9F5
          // （浅色的 elv 比纸暗，当浮起面会读成「压下去」——方向相反，必须换槽不是换值）
          'flex max-h-[90vh] w-full max-w-lg animate-scale-in flex-col rounded-2xl border border-snb-hairline bg-snb-panel shadow-2xl motion-reduce:animate-none dark:bg-snb-elv',
          className
        )}
        onClick={(e) => e.stopPropagation()}
      >
        {title != null && (
          <div className="flex flex-shrink-0 items-center justify-between border-b border-snb-hairline px-6 py-4">
            <h3 className="text-lg font-semibold text-snb-t1">{title}</h3>
            <button
              className="rounded-lg px-2 py-1 text-snb-t3 transition-colors hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus"
              aria-label="关闭"
              onClick={onClose}
            >
              ✕
            </button>
          </div>
        )}
        <div className="flex-1 overflow-y-auto px-6 py-4">{children}</div>
        {footer != null && (
          <div className="flex flex-shrink-0 items-center justify-end gap-3 border-t border-snb-hairline px-6 py-4">
            {footer}
          </div>
        )}
      </div>
    </div>
  )
}
