import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { t } from '../i18n'
import { listArticles, type ArticleSummary } from './api'
import { loadProgress } from './bookShared'

/**
 * 连载书封（bespoke 手绘 SVG，非 AI 生成）：墨底书壳固定不随主题翻转（封面是物件不是页面），
 * 靠容器 hairline 与浅暗两底分离。按 slug 出专属封面，未登记的书走通用终端书壳。
 */
function SerialJacket({ slug }: { slug: string }) {
  const codex = slug === 'codex-complete-guide-zh'
  return (
    <svg viewBox="0 0 112 150" role="img" aria-hidden="true">
      <defs>
        <linearGradient id="sj-bg" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#262019" />
          <stop offset="1" stopColor="#16120E" />
        </linearGradient>
      </defs>
      <rect width="112" height="150" fill="url(#sj-bg)" />
      <rect x="6" y="6" width="100" height="138" rx="8" fill="none" stroke="#F0E9DD" strokeOpacity="0.1" />
      <text x="14" y="27" fontFamily="ui-monospace, Menlo, monospace" fontSize="9" letterSpacing="1" fill="#D9A35C">
        {codex ? '❯ codex' : '❯ _'}
      </text>
      <text x="56" y="76" textAnchor="middle" fontFamily="Georgia, 'Songti SC', serif" fontSize="23" fill="#F0E9DD">
        {codex ? 'Codex' : '连载'}
      </text>
      <text x="56" y="93" textAnchor="middle" fontFamily="Georgia, 'Songti SC', serif" fontSize="8.5" fill="#F0E9DD" fillOpacity="0.72">
        {codex ? '从入门到精通' : '成体系长读'}
      </text>
      {/* 底部信号带：呼应目录页五形态带的节奏 */}
      <rect x="14" y="112" width="56" height="3" rx="1.5" fill="#CC785C" />
      <rect x="14" y="119" width="40" height="3" rx="1.5" fill="#D9A35C" fillOpacity="0.8" />
      <rect x="14" y="126" width="66" height="3" rx="1.5" fill="#CC785C" fillOpacity="0.6" />
      <rect x="14" y="133" width="30" height="3" rx="1.5" fill="#D9A35C" fillOpacity="0.4" />
      {codex && (
        <text x="98" y="136" textAnchor="end" fontFamily="ui-monospace, Menlo, monospace" fontSize="7" fill="#F0E9DD" fillOpacity="0.55">
          13 讲
        </text>
      )}
    </svg>
  )
}

/** 连载专栏位（主打位）：电子书不作类目示人（站长 2026-07-12 拍板），整本连载在卡片墙顶部以横幅登场，书封配图+进度感知续读。 */
export function SerialSpotlight() {
  const [book, setBook] = useState<ArticleSummary | null>(null)

  useEffect(() => {
    listArticles({ page: 1, pageSize: 1, category: 'ebooks' })
      .then((p) => setBook(p.items[0] ?? null))
      .catch(() => setBook(null))
  }, [])

  if (!book) return null
  const resumed = loadProgress(book.slug).pos != null
  return (
    <Link className="hub-serial" to={`/a/${book.slug}`} data-testid="hub-serial">
      <span className="hub-serial-art" data-testid="hub-serial-art">
        <SerialJacket slug={book.slug} />
      </span>
      <div className="hub-serial-main">
        <span className="hub-serial-eyebrow">{t('hub.book.serial')}</span>
        <h2 className="hub-serial-title">{book.title}</h2>
        <p className="hub-serial-deck">{book.summary}</p>
      </div>
      <span className="hub-serial-cta">{t(resumed ? 'hub.list.serialResume' : 'hub.list.serialStart')}</span>
    </Link>
  )
}
