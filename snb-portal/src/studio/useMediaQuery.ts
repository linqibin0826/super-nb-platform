import { useEffect, useState } from 'react'

/** 断点订阅：拿不到 matchMedia（jsdom/SSR）时恒 false，调用方一律退回窄档形态。
 *  studio 用它做两件事：票据坐左栏还是浮底部（≥1280）、大图走浮层还是底部抽屉。 */
export function useMediaQuery(query: string): boolean {
  const read = (): boolean =>
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia(query).matches

  const [matches, setMatches] = useState(read)

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
    const mq = window.matchMedia(query)
    const onChange = () => setMatches(mq.matches)
    onChange()
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [query])

  return matches
}
