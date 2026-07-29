import { forwardRef, useId, type InputHTMLAttributes } from 'react'
import { cx } from '../../lib/cx'

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  hint?: string
  error?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
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
      <input
        ref={ref}
        id={inputId}
        className={cx(
          // ⚠️ 浅色档纪律：输入框底是抬升面 elv，**t3 压 elv 只有 4.00:1**——
          // 占位符在白天档必须降级到 t2（5.43:1）；深色档保持 t3 原样
          'w-full rounded-xl border bg-snb-elv px-4 py-2.5 text-sm text-snb-t1 placeholder:text-snb-t2 transition-all duration-quick ease-snb focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:opacity-60 dark:placeholder:text-snb-t3',
          error
            ? // 错误色归到语义槽 --snb-danger：Tailwind 默认 red-500 压纸只有 3.25:1
              'border-snb-danger focus:border-snb-danger focus:ring-snb-danger/50'
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
