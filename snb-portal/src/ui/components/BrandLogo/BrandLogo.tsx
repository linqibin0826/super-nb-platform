import { cx } from '../../lib/cx'

export interface BrandLogoProps {
  href?: string
  size?: 'md' | 'lg'
  className?: string
}

/** SUPER·NB 字标：700 字重 + 0.06em 字距（主站顶栏/指南站同款） */
export function BrandLogo({ href, size = 'md', className }: BrandLogoProps) {
  const cls = cx(
    'font-bold tracking-[0.06em] text-snb-t1 no-underline',
    size === 'md' ? 'text-xl' : 'text-2xl',
    className
  )
  if (href) {
    return (
      <a href={href} className={cls}>
        SUPER·NB
      </a>
    )
  }
  return <span className={cls}>SUPER·NB</span>
}
