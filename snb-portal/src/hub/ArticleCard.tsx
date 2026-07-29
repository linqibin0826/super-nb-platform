import { useState } from 'react'
import { Link } from 'react-router-dom'
import { th } from './hubMessages'
import type { ArticleSummary } from './api'

/** 卡片整卡可点：一律进文章页（电子书同版式，2026-07-11 起无独立阅读页）。 */
function hrefOf(a: ArticleSummary): string {
  return `/a/${a.slug}`
}

/** 卡上只给「月-日」——年份在杂志架里是噪音，出处名才是这一行的主角。 */
function shortDate(iso: string): string {
  const d = new Date(iso)
  return `${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * 封面槽三态（定稿问题⑤的解法）：
 * 有图 = 直接铺；没图 = 类目字标平色板；加载中 = 静态骨架色块（不闪不呼吸）。
 * 三态共用同一个 16:9 槽 ⇒ 同一行三个类目药丸永远落在同一高度，扫描基线立得住；
 * aspect-ratio 预占位 ⇒ 图到位不推版（CLS）。
 */
function CardCover({ article }: { article: ArticleSummary }) {
  const [loaded, setLoaded] = useState(false)
  const [failed, setFailed] = useState(false)

  if (!article.coverUrl || failed) {
    return (
      <div className="hub-card-cover" aria-hidden="true">
        <span className="word">{article.categoryName}</span>
      </div>
    )
  }
  return (
    <div className={`hub-card-cover has-img${loaded ? ' loaded' : ''}`} aria-hidden="true">
      {!loaded && (
        <span className="skel">
          <i />
          <i />
          <span>{th('mag.coverLoading')}</span>
        </span>
      )}
      <img
        src={article.coverUrl}
        alt=""
        loading="lazy"
        decoding="async"
        onLoad={() => setLoaded(true)}
        onError={() => setFailed(true)}
      />
    </div>
  )
}

/**
 * 内容卡：结构恒定「封面槽 → 类目药丸 → 标题 → 摘要 → 出处行」。
 * 标题 18px/600（定稿问题⑥：杂志架的标题不该比正文还小）；
 * 最后一行固定挂出处名——出处是内容纪律，不是可省的装饰。
 */
export function ArticleCard({ article }: { article: ArticleSummary }) {
  return (
    <Link to={hrefOf(article)} className="hub-card" aria-label={article.title}>
      <CardCover article={article} />
      <div className="hub-card-body">
        <div className="hub-card-meta">
          <span className="cat">{article.categoryName}</span>
        </div>
        <h3 className="hub-card-title">{article.title}</h3>
        <p className="hub-card-sum">{article.summary}</p>
        <div className="hub-card-src">
          {shortDate(article.publishedAt)}
          {article.sourceName ? ` · ${th('mag.sourcePrefix')}${article.sourceName}` : ''}
        </div>
      </div>
    </Link>
  )
}
