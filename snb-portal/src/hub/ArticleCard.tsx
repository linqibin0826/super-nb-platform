import { Link } from 'react-router-dom'
import { Badge, Card, Chip, cx } from '../ui'
import { t } from '../i18n'
import type { ArticleSummary } from './api'

/** 卡片整卡可点：一律进文章页（电子书同版式，2026-07-11 起无独立阅读页）。 */
function hrefOf(a: ArticleSummary): string {
  return `/a/${a.slug}`
}

function formatDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 内容卡：封面（可缺省走纯文字变体）+ 分类徽标 + 标题 + 摘要 + 标签 + 日期/来源。 */
export function ArticleCard({ article }: { article: ArticleSummary }) {
  return (
    <Link to={hrefOf(article)} className="block h-full" aria-label={article.title}>
      <Card hover className="flex h-full flex-col overflow-hidden">
        {article.coverUrl && (
          <div className="aspect-[16/9] w-full overflow-hidden bg-snb-well">
            <img
              src={article.coverUrl}
              alt=""
              loading="lazy"
              className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-[1.02]"
            />
          </div>
        )}
        <div className="flex flex-1 flex-col gap-2.5 p-4">
          <div className="flex items-center gap-2">
            <Chip>{article.categoryName}</Chip>
            {/* 分类本身是「电子书」时徽标同名冗余，只在其他分类下亮 */}
            {article.type === 'ebook' && article.categorySlug !== 'ebooks' && <Badge>{t('hub.list.ebook')}</Badge>}
          </div>
          <h3 className="text-[15px] font-semibold leading-snug text-snb-t1">{article.title}</h3>
          <p className={cx('text-[13px] leading-relaxed text-snb-t2', article.coverUrl ? 'line-clamp-2' : 'line-clamp-4')}>
            {article.summary}
          </p>
          {/* 左标签溢出裁切、右来源整体不折行——防长来源名把标签挤成竖排 */}
          <div className="mt-auto flex items-center justify-between gap-2 pt-1 text-xs text-snb-t3">
            <span className="flex min-w-0 items-center gap-1.5 overflow-hidden [mask-image:linear-gradient(to_right,black_calc(100%-16px),transparent)]">
              {article.tags.slice(0, 3).map((tag) => (
                <span key={tag} className="whitespace-nowrap rounded bg-snb-t1/[0.05] px-1.5 py-0.5">
                  {tag}
                </span>
              ))}
            </span>
            <span className="shrink-0 whitespace-nowrap">
              {article.sourceName ? `${article.sourceName} · ` : ''}
              {formatDate(article.publishedAt)}
            </span>
          </div>
        </div>
      </Card>
    </Link>
  )
}
