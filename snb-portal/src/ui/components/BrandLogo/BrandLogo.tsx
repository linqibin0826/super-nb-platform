import { cx } from '../../lib/cx'

export interface BrandLogoProps {
  href?: string
  size?: 'md' | 'lg'
  className?: string
}

/** SUPER·NB 字标：Jost 700 + 0.07em 字距 + 间隔号安全橙——与 templates/app-header.html
 *  的 .snb-hdr__wordmark(-dot) 契约同参数（2026-07-28 全局 review #2：此前是无 Jost、
 *  无橙点的裸 font-bold，契约单边漂移；@font-face 同批补进 styles.css）。 */
export function BrandLogo({ href, size = 'md', className }: BrandLogoProps) {
  const cls = cx(
    'font-sign font-bold tracking-[0.07em] text-snb-t1 no-underline',
    size === 'md' ? 'text-xl' : 'text-2xl',
    className
  )
  const mark = (
    <>
      SUPER<span className="text-snb-safety">·</span>NB
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
