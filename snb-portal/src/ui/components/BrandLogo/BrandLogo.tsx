import { cx } from '../../lib/cx'

export interface BrandLogoProps {
  href?: string
  size?: 'md' | 'lg'
  className?: string
}

/** SUPER·NB 字标：系统字 700 + 负字距（🪦 v2 招牌字与 .07em 宽字距随网吧退役）；间隔号品牌原色。
 *  与 templates/app-header.html 的 .snb-hdr__wordmark(-dot) 契约同参数。 */
export function BrandLogo({ href, size = 'md', className }: BrandLogoProps) {
  const cls = cx(
    'font-sans font-bold tracking-[-0.02em] text-snb-t1 no-underline',
    size === 'md' ? 'text-xl' : 'text-2xl',
    className
  )
  const mark = (
    <>
      SUPER<span className="text-snb-brand">·</span>NB
    </>
  )
  if (href) {
    return (
      <a href={href} className={cls}>
        {mark}
      </a>
    )
  }
  return <span className={cls}>{mark}</span>
}
