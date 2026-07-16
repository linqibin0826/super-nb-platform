/** 通用「首次引导」hook:服务端已读为真源(换浏览器不再弹),localStorage 兜底缓存。
 *  判定流程(mount 一次):
 *  - 服务端已 ack → 不弹,顺手写本地缓存
 *  - 服务端未 ack 但本地读过(曾未登录时 dismiss) → 不弹,静默补写服务端
 *  - 都没有 → 弹
 *  - 未登录(401) → 本地读过才不弹;dismiss 只落本地,登录后下次进站自动补写
 *  - 其他错误 → 引导非关键,静默不弹
 *  以后别的站/别的引导:换个 key(站点.场景.版本)即接入。 */
import { useCallback, useEffect, useState } from 'react'
import { getGuideAcks, GuideAuthError, postGuideAck } from './api'

const LS_PREFIX = 'snb_guide_ack:'

function localSeen(key: string): boolean {
  try {
    return !!localStorage.getItem(LS_PREFIX + key)
  } catch {
    return false
  }
}

function markLocal(key: string): void {
  try {
    localStorage.setItem(LS_PREFIX + key, '1')
  } catch {
    /* 存储不可用就只靠服务端 */
  }
}

export function useGuideAck(key: string): { show: boolean; ack: () => void } {
  const [show, setShow] = useState(false)

  useEffect(() => {
    let alive = true
    const seen = localSeen(key)
    getGuideAcks()
      .then((keys) => {
        if (keys.includes(key)) {
          markLocal(key)
          return
        }
        if (seen) {
          postGuideAck(key).catch(() => {}) // 本地已读 → 迁移进服务端
          return
        }
        if (alive) setShow(true)
      })
      .catch((e) => {
        if (e instanceof GuideAuthError && !seen && alive) setShow(true)
      })
    return () => {
      alive = false
    }
  }, [key])

  const ack = useCallback(() => {
    setShow(false)
    markLocal(key)
    postGuideAck(key).catch(() => {}) // 未登录/瞬断静默,靠下次进站对账补写
  }, [key])

  return { show, ack }
}
