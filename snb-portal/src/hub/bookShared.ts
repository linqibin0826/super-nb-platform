import { t } from '../i18n'
import type { BookChapter } from './api'

/** 讲次编号（行首大字）：序 / 01..10 / A..C。 */
export function numLabel(c: BookChapter): string {
  return c.kind === 'preface' ? t('hub.book.preface') : (c.num ?? '')
}

/** 讲次称呼：序 / 第 N 讲 / 附录 X。 */
export function lessonLabel(c: BookChapter): string {
  if (c.kind === 'preface') return t('hub.book.preface')
  if (c.kind === 'appendix') return t('hub.book.appendixN', { x: c.num ?? '' })
  return t('hub.book.lesson', { n: Number(c.num) })
}

/** 所属幕名：开篇 / {眉标}篇 / 附录。 */
export function actName(c: BookChapter): string {
  if (c.kind === 'preface') return t('hub.book.opening')
  if (c.kind === 'appendix') return t('hub.book.appendix')
  return t('hub.book.actSuffix', { e: c.eyebrow })
}

/** 读进度：pos = 上次读到哪讲哪个位置（at 0..1 滚动比）；read = 读完过的讲。 */
export interface BookProgress {
  pos: { index: number; at: number } | null
  read: Set<number>
}

const posKey = (slug: string) => `hub-book-pos:${slug}`
const readKey = (slug: string) => `hub-book-read:${slug}`

/** localStorage 不可用/脏数据一律回退空进度。 */
export function loadProgress(slug: string): BookProgress {
  try {
    const pos: unknown = JSON.parse(localStorage.getItem(posKey(slug)) || 'null')
    const read: unknown = JSON.parse(localStorage.getItem(readKey(slug)) || '[]')
    const p = pos as { index?: unknown; at?: unknown } | null
    return {
      pos: p && typeof p.index === 'number' ? { index: p.index, at: typeof p.at === 'number' ? p.at : 0 } : null,
      read: new Set(Array.isArray(read) ? read.filter((n): n is number => typeof n === 'number') : []),
    }
  } catch {
    return { pos: null, read: new Set() }
  }
}

export function savePos(slug: string, index: number, at: number): void {
  try {
    localStorage.setItem(posKey(slug), JSON.stringify({ index, at: Math.round(at * 100) / 100 }))
  } catch {
    /* localStorage 不可用忽略 */
  }
}

/** 进讲即记位置；同一讲保留旧滚动比，换讲归零。 */
export function touchPos(slug: string, index: number): void {
  const cur = loadProgress(slug).pos
  savePos(slug, index, cur && cur.index === index ? cur.at : 0)
}

export function markRead(slug: string, index: number): void {
  try {
    const read = loadProgress(slug).read
    if (read.has(index)) return
    read.add(index)
    localStorage.setItem(readKey(slug), JSON.stringify([...read]))
  } catch {
    /* localStorage 不可用忽略 */
  }
}
