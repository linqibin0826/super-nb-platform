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
          'w-full rounded-xl border bg-snb-elv px-4 py-2.5 text-sm text-snb-t1 placeholder:text-snb-t3 transition-all duration-200 focus:outline-none focus:ring-2 disabled:cursor-not-allowed disabled:opacity-60',
          error
            ? 'border-red-500 focus:border-red-500 focus:ring-red-500/30'
            : 'border-snb-hairline-strong focus:border-primary-500 focus:ring-primary-500/30'
        )}
        {...rest}
      />
      {error ? (
        <p className="mt-1 text-xs text-red-500">{error}</p>
      ) : (
        hint && <p className="mt-1 text-xs text-snb-t3">{hint}</p>
      )}
    </div>
  )
})
