import { t } from '../i18n'

/** 队列聚合文案：托盘节头 meta 与票根胶囊共用同一措辞（非零项 · 连接） */
export function queueLabel(running: number, queued: number, finished: number): string {
  const parts: string[] = []
  if (running > 0) parts.push(t('studio.queue.metaRunning', { count: running }))
  if (queued > 0) parts.push(t('studio.queue.metaQueued', { count: queued }))
  if (finished > 0) parts.push(t('studio.queue.metaDone', { count: finished }))
  return parts.join(' · ')
}
