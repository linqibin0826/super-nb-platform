import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import DOMPurify from 'dompurify'
import { t } from '../../i18n'
import { th } from '../hubMessages'
import { keysUrl } from '../../auth/apiFetch'
import { getArticle, listArticles, NotFoundError, type ArticleDetail, type ArticleSummary } from '../api'
import { ReadingProgress } from '../ReadingProgress'
import { ArticleToc, ArticleTocBar, useHeadings, useReadingScroll } from '../ArticleToc'
import { useCodeBlocks } from '../codeBlocks'
import { readingMinutes } from '../readingTime'
import { EbookLongRead } from '../EbookLongRead'

type State =
  | { kind: 'loading' }
  | { kind: 'ready'; article: ArticleDetail }
  | { kind: 'notFound' }
  | { kind: 'error' }

function formatDate(iso: string): string {
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** 正文净字数（去标签去实体去空白）——右栏「字数」与目录「全文 N 字」用同一个口径。 */
function charCount(html: string): number {
  return html
    .replace(/<[^>]+>/g, '')
    .replace(/&[a-z#0-9]+;/gi, ' ')
    .replace(/\s+/g, '').length
}

/** 正文里的代码块个数（右栏信息用）。 */
function codeCount(html: string): number {
  return (html.match(/<pre[\s>]/gi) ?? []).length
}

/**
 * 杂志架索引：一次拉全站可见条目（pageSize 48 = 后端上限；现存 16 条一页装得下），
 * 文章页的「下一篇 / 上一篇 / 相关阅读 / 全部 N 篇 / 电子书直达」全从这一份里取——
 * 不加新端点、不改后端契约。拉失败就静默降级（章末导航退成「回杂志架」）。
 */
function useRackIndex(enabled: boolean) {
  const [rack, setRack] = useState<{ items: ArticleSummary[]; total: number }>({ items: [], total: 0 })
  useEffect(() => {
    if (!enabled) return
    let alive = true
    listArticles({ page: 1, pageSize: 48 })
      .then((env) => {
        if (!alive) return
        setRack({
          items: Array.isArray(env?.items) ? env.items : [],
          total: typeof env?.total === 'number' ? env.total : 0,
        })
      })
      .catch(() => {})
    return () => {
      alive = false
    }
  }, [enabled])
  return rack
}

/**
 * 文章详情页（2026-07-29 定稿落地）：三栏「迷你目录 | 纸柱 | 右栏」。
 * 正文=管线预渲染 HTML + DOMPurify 兜底（纵深防御，照公告口径默认白名单）。
 * 电子书=节目单阅读版（EbookLongRead 自持整页），走独立分支。
 * 🚨 正文 738px（18px × 41 汉字）任何断点都不加宽——要填的是两侧空边距，不是正文。
 */
export function ArticlePage() {
  const { slug = '' } = useParams()
  const [state, setState] = useState<State>({ kind: 'loading' })
  const [reload, setReload] = useState(0)
  const [coverFailed, setCoverFailed] = useState(false)
  const proseRef = useRef<HTMLDivElement>(null)

  // 换文回到页顶（路由变化 React Router 不自动滚）
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [slug])

  useEffect(() => {
    let alive = true
    setState({ kind: 'loading' })
    setCoverFailed(false) // 换文重置封面兜底
    getArticle(slug)
      .then((article) => alive && setState({ kind: 'ready', article }))
      .catch((e) => alive && setState({ kind: e instanceof NotFoundError ? 'notFound' : 'error' }))
    return () => {
      alive = false
    }
  }, [slug, reload])

  useEffect(() => {
    if (state.kind === 'ready') {
      document.title = `${state.article.title} · ${t('hub.title')}`
    }
  }, [state])

  const article = state.kind === 'ready' ? state.article : null
  const isArticle = article != null && article.type !== 'ebook'
  const bodyHtml = isArticle ? (article.bodyHtml ?? '') : ''

  // hooks 一律无条件调用，分支只决定它们做不做事
  const rack = useRackIndex(isArticle)
  // 🚨 这个对象的身份必须稳住：React 19 见 prop 身份变了就无条件重设 innerHTML（不比字符串值），
  // 每渲染新建字面量 = 每次重渲染冲掉 useCodeBlocks 装配好的机箱壳（详见 codeBlocks.ts 注）
  const proseHtml = useMemo(() => ({ __html: DOMPurify.sanitize(bodyHtml) }), [bodyHtml])
  const headings = useHeadings(proseRef, [bodyHtml])
  const { active, pct } = useReadingScroll(headings.length)
  useCodeBlocks(proseRef, [bodyHtml])

  if (state.kind === 'loading') {
    // 骨架几何与正式态对齐（同一套三栏 + 同一根纸柱），加载完不整页跳版
    return (
      <main className="hub-article" data-testid="hub-article">
        <div className="hub-article-grid">
          <aside className="hub-toc" />
          <article className="hub-paper">
            <div className="hub-paper-col hub-artskel" aria-hidden="true">
              <i style={{ height: 22, width: 180 }} />
              <i style={{ height: 46, marginTop: 16 }} />
              <i style={{ height: 46, width: '62%', marginTop: 10 }} />
              <i style={{ height: 108, marginTop: 34 }} />
              <i style={{ height: 260, marginTop: 32 }} />
            </div>
          </article>
          <aside className="hub-rail" />
        </div>
      </main>
    )
  }

  if (state.kind === 'notFound' || state.kind === 'error') {
    const isErr = state.kind === 'error'
    return (
      <main className="hub-article" data-testid="hub-article">
        <div className="hub-article-grid">
          <aside className="hub-toc" />
          <article className="hub-paper">
            <div className="hub-paper-col">
              <section
                className={isErr ? 'hub-state err' : 'hub-state'}
                data-testid={isErr ? 'hub-error' : 'hub-not-found'}
              >
                <span className="ic" aria-hidden="true">
                  {isErr ? '!' : '∅'}
                </span>
                <div className="ti">{isErr ? th('art.errTitle') : t('hub.article.notFound')}</div>
                <div className="ds">{isErr ? th('art.errDesc') : th('art.nfDesc')}</div>
                <div className="ac">
                  {isErr && (
                    <button type="button" className="hub-btn2" onClick={() => setReload((n) => n + 1)}>
                      {th('mag.retry')}
                    </button>
                  )}
                  <Link className={isErr ? 'hub-btn3' : 'hub-btn2'} to="/">
                    {t('hub.article.backHome')}
                  </Link>
                </div>
              </section>
            </div>
          </article>
          <aside className="hub-rail" />
        </div>
      </main>
    )
  }

  const a = state.article
  if (a.type === 'ebook') {
    return <EbookLongRead slug={a.slug} path={a.ebookPath ?? ''} />
  }

  const minutes = readingMinutes(bodyHtml)
  const chars = charCount(bodyHtml)
  const codes = codeCount(bodyHtml)

  // 章末导航 / 相关阅读的料：全从索引里取，电子书不参与文章前后链（它走专栏位）
  const posts = rack.items.filter((x) => x.type !== 'ebook')
  const idx = posts.findIndex((x) => x.slug === a.slug)
  const next = idx >= 0 && idx + 1 < posts.length ? posts[idx + 1] : null
  const prev = idx > 0 ? posts[idx - 1] : null
  const related = posts.filter((x) => x.categorySlug === a.categorySlug && x.slug !== a.slug).slice(0, 3)
  const ebook = rack.items.find((x) => x.type === 'ebook') ?? null

  return (
    <main className="hub-article" data-testid="hub-article">
      <ReadingProgress />
      <ArticleTocBar items={headings} active={active} />

      <div className="hub-article-grid">
        <ArticleToc items={headings} active={active} pct={pct} chars={chars} />

        <article className="hub-paper">
          <div className="hub-paper-col">
            {/* 眉行只有「类目 · 日期 · 时长」——这个站没有作者字段，署名走文末出处区 */}
            <div className="hub-eyebrow-row" data-testid="hub-byline">
              <span className="cat">{a.categoryName}</span>
              <time dateTime={a.publishedAt}>{formatDate(a.publishedAt)}</time>
              <span aria-hidden="true">·</span>
              <span>{t('hub.article.readingTime', { n: minutes })}</span>
            </div>
            <h1 className="hub-headline">{a.title}</h1>

            {/* 封面：aspect-ratio 预占位（图到位不推版）；拉不到就整块收掉，
                别在正文顶上留一个 16:9 的空盒子 */}
            {a.coverUrl && !coverFailed && (
              <figure className="hub-article-cover">
                <img
                  alt=""
                  loading="lazy"
                  decoding="async"
                  src={a.coverUrl}
                  onError={() => setCoverFailed(true)}
                />
              </figure>
            )}

            {/* 速览：全页优先级最高的 60 个字，比正文大一档、最亮档主字（层级反转） */}
            {a.summary && (
              <aside className="hub-tldr" data-testid="hub-tldr">
                <span className="hub-tldr-tab">{t('hub.article.tldr')}</span>
                <p>{a.summary}</p>
              </aside>
            )}

            {/* 管线已预渲染并 sanitize；proseHtml 里 DOMPurify 默认白名单再兜一层（纵深防御） */}
            <div className="hub-prose" ref={proseRef} dangerouslySetInnerHTML={proseHtml} />

            {/* 一手出处：固定位置，不折叠、不省略——没有出处的稿子本来就不上架 */}
            {(a.sourceName || a.sourceUrl) && (
              <section className="hub-sources">
                <div className="lb">{th('art.sources')}</div>
                <ul>
                  <li data-testid="hub-source">
                    <span className="nm">{a.sourceName ?? a.sourceUrl}</span>
                    {a.sourceUrl && (
                      <a href={a.sourceUrl} target="_blank" rel="noopener noreferrer">
                        {t('hub.article.original')} ↗
                      </a>
                    )}
                  </li>
                </ul>
                <p className="note">{th('art.sourcesNote', { d: formatDate(a.publishedAt) })}</p>
              </section>
            )}

            {/* 章末导航：照搬电子书讲末那件现成件（.hub-part-next），换成「下一篇」 */}
            <nav className="hub-part-next hub-artnav" data-testid="hub-artnav">
              {next ? (
                <>
                  <div className="label">
                    {th('art.next')}
                    <span className="nmin">{next.categoryName}</span>
                  </div>
                  <Link className="next-link" to={`/a/${next.slug}`}>
                    <span className="nt">{next.title}</span>
                    <span className="na" aria-hidden="true">
                      →
                    </span>
                  </Link>
                  {next.summary && <div className="next-hook">{next.summary}</div>}
                </>
              ) : (
                <>
                  <div className="label">{th('art.nextDone')}</div>
                  <Link className="next-link" to="/">
                    <span className="nt">{th('art.nextBack')}</span>
                    <span className="na" aria-hidden="true">
                      →
                    </span>
                  </Link>
                </>
              )}
              <div className="hub-part-foot hub-artnav-foot">
                {prev ? (
                  <Link to={`/a/${prev.slug}`}>{th('art.prev', { t: prev.title })}</Link>
                ) : (
                  <span aria-hidden="true" />
                )}
                <Link to="/">{rack.total > 0 ? th('art.nextAll', { n: rack.total }) : t('hub.article.backHome')}</Link>
              </div>
            </nav>

            {(related.length > 0 || a.tags.length > 0) && (
              <section className="hub-related">
                {related.length > 0 && (
                  <>
                    <div className="lb">{th('art.related')}</div>
                    <div className="hub-related-list">
                      {related.map((r) => (
                        <Link key={r.slug} className="hub-related-row" to={`/a/${r.slug}`}>
                          <span className="rt">{r.title}</span>
                          <span className="rc">{r.categoryName}</span>
                        </Link>
                      ))}
                    </div>
                  </>
                )}
                {/* 标签：虚线 = 位置已留、未接线。站方接口没接之前不做落地页 */}
                {a.tags.length > 0 && (
                  <div className="hub-tags">
                    <span className="lb">{th('art.tags')}</span>
                    {a.tags.slice(0, 6).map((tag) => (
                      <span className="tg" key={tag}>
                        {tag}
                      </span>
                    ))}
                    <span className="off">{th('art.tagsOff')}</span>
                  </div>
                )}
              </section>
            )}
          </div>
        </article>

        <aside className="hub-rail">
          <div className="hub-rail-inner">
            <div>
              <div className="lb">{th('art.info')}</div>
              <dl>
                <dt>{th('art.infoPub')}</dt>
                <dd>{formatDate(a.publishedAt)}</dd>
                <dt>{th('art.infoCat')}</dt>
                <dd>{a.categoryName}</dd>
                <dt>{th('art.infoChars')}</dt>
                <dd>{chars}</dd>
                <dt>{th('art.infoCode')}</dt>
                <dd>{codes}</dd>
              </dl>
            </div>

            {ebook && (
              <div className="blk">
                <div className="lb">{th('art.ebook')}</div>
                <Link className="bk" to={`/a/${ebook.slug}`}>
                  <span className="bt">{ebook.title}</span>
                  <span className="bs">{th('art.ebookGo')}</span>
                </Link>
              </div>
            )}

            <div className="blk">
              <div className="lb">{th('art.action')}</div>
              <a className="hub-cta wide" href={keysUrl()}>
                {th('art.actionCta')}
              </a>
              <p className="note">{th('art.actionNote')}</p>
            </div>
          </div>
        </aside>
      </div>
    </main>
  )
}
