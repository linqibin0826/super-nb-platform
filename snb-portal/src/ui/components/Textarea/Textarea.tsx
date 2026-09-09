import { forwardRef, useId, type TextareaHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string
  hint?: string
  error?: string
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { label, hint, error, className, id, ...rest },
  ref
) {
  const autoId = useId()
  const inputId = id ?? autoId
  return (
    <div className={className}>
      {label && (
        <label htmlFor={inputId} className="mb-1.5 block text-sm font-medium text-snb-t2">
          {label}
        </label>
      )}
      <textarea
        ref={ref}
        id={inputId}
        className={cx(
          // ⚠️ 与 Input 逐字同源（改必两边同步）：well 灰底；t3 压 well 浅色只有 4.27:1，
          //    占位符两档都用 t2；错误色走 --snb-danger
          'w-full resize-y rounded-xl border bg-snb-well px-4 py-2.5 text-[15px] text-snb-t1 placeholder:text-snb-t2 transition-[border-color,box-shadow] duration-quick ease-snb-quick focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:opacity-60',
          error
            ? 'border-snb-danger focus:border-snb-danger focus:ring-snb-danger/50'
            : 'border-snb-hairline-strong focus:border-snb-safety focus:ring-snb-focus'
        )}
        {...rest}
      />
      {error ? (
        <p className="mt-1 text-xs text-snb-danger">{error}</p>
      ) : (
        hint && <p className="mt-1 text-xs text-snb-t3">{hint}</p>
      )}
    </div>
  )
})
