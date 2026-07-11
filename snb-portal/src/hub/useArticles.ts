import { useCallback, useEffect, useRef, useState } from 'react'
import { listArticles, type ArticleSummary } from './api'

interface State {
  items: ArticleSummary[]
  page: number
  pages: number
  loading: boolean
  error: boolean
}

const INITIAL: State = { items: [], page: 0, pages: 0, loading: true, error: false }

/**
 * 列表分页状态机：filters（category）变更即重置重拉第 1 页，loadMore 追加下一页。
 * StrictMode 双跑防护：进行中请求用序号 ref 守卫，过期响应直接丢弃（studio 队列同款教训）。
 */
export function useArticles(category: string | null) {
  const [state, setState] = useState<State>(INITIAL)
  const requestSeq = useRef(0)

  const fetchPage = useCallback(
    (page: number, append: boolean) => {
      const seq = ++requestSeq.current
      setState((s) => ({ ...s, loading: true, error: false }))
      listArticles({ page, category: category ?? undefined })
        .then((env) => {
          if (seq !== requestSeq.current) return // 过期响应（filters 已变/更快的请求已回）
          setState((s) => ({
            items: append ? [...s.items, ...env.items] : env.items,
            page: env.page,
            pages: env.pages,
            loading: false,
            error: false,
          }))
        })
        .catch(() => {
          if (seq !== requestSeq.current) return
          setState((s) => ({ ...s, loading: false, error: true }))
        })
    },
    [category],
  )

  useEffect(() => {
    setState(INITIAL)
    fetchPage(1, false)
  }, [fetchPage])

  const loadMore = useCallback(() => fetchPage(state.page + 1, true), [fetchPage, state.page])
  const retry = useCallback(() => fetchPage(state.page === 0 ? 1 : state.page, state.page > 1), [fetchPage, state.page])

  return {
    items: state.items,
    loading: state.loading,
    error: state.error,
    hasMore: state.page > 0 && state.page < state.pages,
    initialLoading: state.loading && state.items.length === 0,
    loadMore,
    retry,
  }
}
