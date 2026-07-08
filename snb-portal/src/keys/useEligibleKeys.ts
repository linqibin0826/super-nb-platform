import { useCallback, useEffect, useRef, useState } from 'react'
import { useAuthUser } from '../auth/useAuth'
import { fetchAllKeys, fetchGroups, fetchRates } from './api'
import { filterEligibleKeys } from './eligible'
import type { EligibleKey } from '../types'

/** 回到页面触发的重拉最短间隔：频繁切 tab 别打成筛子 */
const REVALIDATE_MIN_INTERVAL_MS = 5000

export function useEligibleKeys() {
  const user = useAuthUser()
  const [loading, setLoading] = useState(false)
  const [keys, setKeys] = useState<EligibleKey[]>([])
  const [rates, setRates] = useState<Record<number, number>>({})

  const reload = useCallback(async () => {
    setLoading(true)
    try {
      const [allKeys, groups, groupRates] = await Promise.all([
        fetchAllKeys(),
        fetchGroups().catch(() => []),
        fetchRates().catch(() => null),
      ])
      setRates(groupRates ?? {})
      setKeys(filterEligibleKeys(allKeys, groups))
    } catch {
      setKeys([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    if (user) void reload()
    else {
      setKeys([])
      setRates({})
    }
  }, [user, reload])

  // 回到页面自动重拉：典型场景是去 /keys 新建 Key 后切回本页（另一 tab 或 bfcache 后退），
  // 不刷新会一直停在「无可用 Key」引导态。focus/visibilitychange/pageshow 三管齐下，
  // 5s 节流；keys 就绪后 App 侧的选中逻辑会自动选上新 Key，无需用户手动关联
  const lastRevalidateRef = useRef(0)
  useEffect(() => {
    if (!user) return
    const revalidate = () => {
      if (document.visibilityState !== 'visible') return
      const now = Date.now()
      if (now - lastRevalidateRef.current < REVALIDATE_MIN_INTERVAL_MS) return
      lastRevalidateRef.current = now
      void reload()
    }
    window.addEventListener('focus', revalidate)
    window.addEventListener('pageshow', revalidate)
    document.addEventListener('visibilitychange', revalidate)
    return () => {
      window.removeEventListener('focus', revalidate)
      window.removeEventListener('pageshow', revalidate)
      document.removeEventListener('visibilitychange', revalidate)
    }
  }, [user, reload])

  return { loading, keys, rates, reload }
}
