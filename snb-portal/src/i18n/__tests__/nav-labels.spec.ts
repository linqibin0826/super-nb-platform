/**
 * 顶栏导航文案覆盖（2026-09-07 批 5-portal 修复轮 1 立约）。
 *
 * 🚨 为什么需要：两个站的顶栏文案都是**按 vendor 的 `SiteNavItem.key` 拼出来的**——
 *    hub 拼 `hub.nav.<key>`，studio 查 `NAV_LABEL_KEYS[key]`。super-nb-ui 那边改一次
 *    key（2026-09-07 「新手指南」`hub` → `help` 就是），这里不跟着改的后果是**静默的**：
 *    createT 缺 key 原样返回，顶栏第三项会显示字面量「hub.nav.help」当中文文案，
 *    没有任何报错、构建也绿。消费方现在都加了「缺键回退 item.label」的兜底，
 *    但兜底只是把丑降级成中文原文，**该红的还得靠这支表驱动测试红**。
 *
 * 断言两件事：
 *   ① 四个 key × zh/en 两本字典，`hub.nav.<key>` 都存在且非空；
 *   ② `NAV_LABEL_KEYS` 覆盖同样四个 key，且每个值都能在两本字典里解析出非空文案。
 */
import { describe, expect, it } from 'vitest'
import { createT } from '../core'
import { messages } from '../messages'
import { SITE_NAV_ITEMS } from '../../ui'
import { NAV_LABEL_KEYS } from '../../studio/TopBar'

const LOCALES = ['zh', 'en'] as const
const NAV_KEYS = SITE_NAV_ITEMS.map((i) => i.key)

/** 解析成功 = 拿到非空字符串且不等于 key 本身（createT 缺 key 就把 key 原样吐回来） */
const resolves = (locale: (typeof LOCALES)[number], path: string) => {
  const label = createT(messages, locale)(path)
  return label !== path && label.trim().length > 0
}

describe('顶栏导航文案覆盖（key 改名必红）', () => {
  it('vendor 的四项 key 就是 console / studio / help / activity（改了这行要一并核消费方）', () => {
    expect(NAV_KEYS).toEqual(['console', 'studio', 'help', 'activity'])
  })

  for (const locale of LOCALES) {
    for (const key of NAV_KEYS) {
      it(`hub 顶栏 [${locale}] hub.nav.${key} 有文案`, () => {
        expect(resolves(locale, `hub.nav.${key}`), `缺 hub.nav.${key}`).toBe(true)
      })
    }
  }

  it('studio 顶栏 NAV_LABEL_KEYS 四个 key 一个不落', () => {
    expect(Object.keys(NAV_LABEL_KEYS).sort()).toEqual([...NAV_KEYS].sort())
  })

  for (const locale of LOCALES) {
    for (const key of NAV_KEYS) {
      it(`studio 顶栏 [${locale}] ${key} → 文案可解析`, () => {
        const path = NAV_LABEL_KEYS[key]
        expect(path, `NAV_LABEL_KEYS 缺 ${key}`).toBeTruthy()
        expect(resolves(locale, path!), `${path} 在 ${locale} 里解析不出文案`).toBe(true)
      })
    }
  }
})
