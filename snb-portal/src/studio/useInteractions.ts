// 灵感库点赞/收藏交互态：本批 my-state 增量回填 + 乐观翻转 + 服务端计数校正 + 失败回滚。
// 登录判断交给调用方：enabled=false 时 toggle 直接 onRequireLogin、不发请求。
// seed：消费方声明的初始已知状态（如「我的收藏」页每张都已收藏），避免首帧显示反。
// id 全程字符串（平台新契约：雪花 id 超 JS 安全整数，前端当不透明串）。
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  fetchInteractions,
  GalleryAuthError,
  toggleFavorite,
  toggleLike,
} from '../lib/galleryApi'

export interface InteractionSeed {
  liked?: string[]
  favorited?: string[]
}

export function useInteractions(
  ids: string[],
  enabled: boolean,
  onRequireLogin: () => void,
  seed?: InteractionSeed
) {
  const [liked, setLiked] = useState<Set<string>>(() => new Set(seed?.liked ?? []))
  const [favorited, setFavorited] = useState<Set<string>>(() => new Set(seed?.favorited ?? []))
  const [likeCounts, setLikeCounts] = useState<Map<string, number>>(new Map())
  const [favCounts, setFavCounts] = useState<Map<string, number>>(new Map())
  const pendingRef = useRef<Set<string>>(new Set()) // `${kind}:${id}` 防连点
  const settledRef = useRef<Set<string>>(new Set()) // 已请求回填的 id（增量去重，防无界增长/撞 100 上限）
  const likeToggledRef = useRef<Set<string>>(new Set()) // 用户已 toggle like 的 id（回填不得覆盖）
  const favToggledRef = useRef<Set<string>>(new Set())

  // 增量回填：只拉从没请求过的 id；请求前先登记，避免 ids 增长时重复请求同一批
  useEffect(() => {
    if (!enabled) return
    const fresh = ids.filter((id) => !settledRef.current.has(id))
    if (fresh.length === 0) return
    fresh.forEach((id) => settledRef.current.add(id))
    // 不设 cancelled 丢弃：settledRef 保证每批 id 只被请求一次、无新旧覆盖冲突，被后续 ids 变化
    // 「超车」的成功响应仍是这批 id 的正确 my-state，必须应用（否则 settled 已登记、后续不再拉，
    // 状态永久丢失）。toggle 过的 id 由 toggledRef 跳过；卸载后 setState 在 React 18+ 是无害 no-op。
    fetchInteractions(fresh)
      .then((r) => {
        setLiked((prev) => {
          const next = new Set(prev)
          for (const id of r.liked) if (!likeToggledRef.current.has(id)) next.add(id)
          return next
        })
        setFavorited((prev) => {
          const next = new Set(prev)
          for (const id of r.favorited) if (!favToggledRef.current.has(id)) next.add(id)
          return next
        })
      })
      .catch(() => {
        // 失败：把这批移出已登记，下次 ids 变化时可重试
        fresh.forEach((id) => settledRef.current.delete(id))
      })
    // ids.join(',') 是稳定指纹；enabled 变化也重评
  }, [enabled, ids.join(',')]) // eslint-disable-line react-hooks/exhaustive-deps

  // 消费方声明的已知状态（如「我的收藏」页每张都已收藏）随 ids 异步加载而增补——
  // useState init 只覆盖首帧同步已知的；这里让异步增长的 seed 也并进来(不覆盖用户已 toggle 的)
  const seedLikedKey = (seed?.liked ?? []).join(',')
  const seedFavKey = (seed?.favorited ?? []).join(',')
  useEffect(() => {
    if (seed?.liked?.length) {
      setLiked((prev) => {
        const next = new Set(prev)
        for (const id of seed.liked!) if (!likeToggledRef.current.has(id)) next.add(id)
        return next
      })
    }
    if (seed?.favorited?.length) {
      setFavorited((prev) => {
        const next = new Set(prev)
        for (const id of seed.favorited!) if (!favToggledRef.current.has(id)) next.add(id)
        return next
      })
    }
  }, [seedLikedKey, seedFavKey]) // eslint-disable-line react-hooks/exhaustive-deps

  const toggle = useCallback(
    async (kind: 'like' | 'favorite', id: string) => {
      if (!enabled) {
        onRequireLogin()
        return
      }
      const k = `${kind}:${id}`
      if (pendingRef.current.has(k)) return
      pendingRef.current.add(k)

      const isLike = kind === 'like'
      ;(isLike ? likeToggledRef : favToggledRef).current.add(id) // 本地已定，回填别覆盖
      const setFilled = isLike ? setLiked : setFavorited
      const setCount = isLike ? setLikeCounts : setFavCounts
      const current = isLike ? liked : favorited
      const on = !current.has(id)

      setFilled((prev) => {
        const next = new Set(prev)
        if (on) next.add(id)
        else next.delete(id)
        return next
      })
      try {
        const res = await (isLike ? toggleLike : toggleFavorite)(id, on)
        const count = isLike ? res.likeCount : res.favCount
        if (typeof count === 'number') setCount((prev) => new Map(prev).set(id, count))
      } catch (e) {
        setFilled((prev) => {
          const next = new Set(prev)
          if (on) next.delete(id)
          else next.add(id)
          return next
        })
        if (e instanceof GalleryAuthError) onRequireLogin()
      } finally {
        pendingRef.current.delete(k)
      }
    },
    [enabled, liked, favorited, onRequireLogin]
  )

  return { liked, favorited, likeCounts, favCounts, toggle }
}
