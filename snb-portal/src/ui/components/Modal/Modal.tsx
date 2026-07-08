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
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-2 backdrop-blur-sm sm:p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        className={cx(
          'flex max-h-[90vh] w-full max-w-lg animate-scale-in flex-col rounded-2xl border border-snb-hairline bg-snb-elv shadow-2xl motion-reduce:animate-none',
          className
        )}
        onClick={(e) => e.stopPropagation()}
      >
        {title != null && (
          <div className="flex flex-shrink-0 items-center justify-between border-b border-snb-hairline px-6 py-4">
            <h3 className="text-lg font-semibold text-snb-t1">{title}</h3>
            <button
              className="rounded-lg px-2 py-1 text-snb-t3 transition-colors hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
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
