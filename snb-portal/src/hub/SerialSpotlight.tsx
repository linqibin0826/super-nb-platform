import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { t } from '../i18n'
import { listArticles, type ArticleSummary } from './api'
import { loadProgress } from './bookShared'

/** 连载专栏位：电子书不作类目示人（站长 2026-07-12 拍板），整本连载在卡片墙顶部以横幅登场，进度感知续读。 */
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
      <div className="hub-serial-main">
        <span className="hub-serial-eyebrow">{t('hub.book.serial')}</span>
        <h2 className="hub-serial-title">{book.title}</h2>
        <p className="hub-serial-deck">{book.summary}</p>
      </div>
      <span className="hub-serial-cta">{t(resumed ? 'hub.list.serialResume' : 'hub.list.serialStart')}</span>
    </Link>
  )
}
