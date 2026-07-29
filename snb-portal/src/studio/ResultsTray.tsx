import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { TaskCard } from './TaskCard'
import { queueLabel } from './queueLabel'
import { t } from '../i18n'
import { st } from './i18nStudio'
import { StatusLamp } from './parts'
import { resolveTheme } from '../themeCookie'
import type { GenerationQueue } from './useGenerationQueue'

/** 彩纸是 canvas 画的，吃不到 CSS 变量；放的那一刻现问一次当前档即可（不需要订阅） */
const isDarkNow = () => resolveTheme() === 'dark'

interface Props {
  queue: GenerationQueue
  onPreview: (images: string[], index: number) => void
  onClose: () => void
}

/** 生成队列托盘：从悬浮票据上方滑出的玻璃面板，任务卡片栈（旧在上、新在下）。
 *  恒有一张展开卡；新提交自动展开、完成不抢焦点。任何状态都可收起（收起只藏面板不打断任务）。 */
export function ResultsTray({ queue, onPreview, onClose }: Props) {
  const reduceMotion = useReducedMotion()
  const { tasks } = queue

  // 展开互斥：默认/新任务出现时展开最新一张；点展开中的卡可收起（允许全部收起）。
  // expandedId === null = 用户主动收起（保持全收）；非空但已不在列表 = 该卡被取消/淘汰，焦点回落最新
  const newestId = tasks.length > 0 ? tasks[tasks.length - 1].id : null
  const [expandedId, setExpandedId] = useState<string | null>(newestId)
  const prevNewest = useRef(newestId)
  useEffect(() => {
    if (newestId !== null && newestId !== prevNewest.current) setExpandedId(newestId)
    prevNewest.current = newestId
  }, [newestId])
  const effectiveExpanded =
    expandedId === null ? null : tasks.some((task) => task.id === expandedId) ? expandedId : newestId

  // 完成庆祝：每个批次落地放一发品牌彩纸（reduced-motion 由库自禁；jsdom 无 2d 上下文跳过）
  const celebrated = useRef(new Set<string>())
  useEffect(() => {
    const fresh = tasks.filter(
      (task) => task.status === 'done' && task.batch && task.batch.images.length > 0 && !celebrated.current.has(task.id)
    )
    if (fresh.length === 0) return
    for (const task of fresh) celebrated.current.add(task.id)
    if (!document.createElement('canvas').getContext('2d')) return
    void import('canvas-confetti')
      .then(({ default: confetti }) =>
        confetti({
          particleCount: 56,
          spread: 75,
          startVelocity: 26,
          gravity: 0.9,
          scalar: 0.8,
          ticks: 130,
          disableForReducedMotion: true,
          // 🚨 彩纸落在**页面底**上，两档必须各配一套：深色档那三支浅橙（#FFE4D3/#FFC7A6）
          // 压到白天档的纸上几乎看不见（最浅那支对 #F2EEE6 只有 1.1:1，等于没放）。
          // 白天档整体压深一档：熔铁橙 + 两支中深橙 + 一支近墨的深棕橙。
          colors: isDarkNow()
            ? ['#FF5C00', '#FFE4D3', '#FFC7A6', '#B04000']
            : ['#BA4400', '#E06A1F', '#8C3200', '#5C2100'],
          origin: { x: 0.5, y: 0.78 },
        })
      )
      .catch(() => {})
  }, [tasks])

  const queuedIds = tasks.filter((task) => task.status === 'queued').map((task) => task.id)

  return (
    <motion.div
      initial={reduceMotion ? false : { y: 20, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      exit={reduceMotion ? { opacity: 0 } : { y: 16, opacity: 0 }}
      transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 360, damping: 34 }}
      className="pointer-events-auto overflow-hidden rounded-[20px] border border-snb-hairline-strong bg-snb-panel shadow-[0_16px_40px_-10px_rgba(70,50,38,0.30)] dark:shadow-[0_18px_48px_-10px_rgba(0,0,0,0.6)]"
    >
      {/* 60vh 上限配合卡片 40vh 封顶；100dvh-380px 给底下票根留位，矮屏整栈不超视口 */}
      <div className="max-h-[min(60vh,640px,calc(100dvh-380px))] overflow-y-auto p-5">
        {/* 节头：标题 + 聚合 meta + 收起（带字，不然会被读成「取消任务」） */}
        <div className="flex items-center justify-between gap-4">
          <h2 className="font-sans text-[19px] font-semibold text-snb-t1">{t('studio.queue.title')}</h2>
          <div className="flex items-center gap-3">
            <span className="font-mono text-xs text-snb-t3">
              {queueLabel(queue.runningCount, queue.queuedCount, queue.finishedCount)}
            </span>
            <button
              type="button"
              onClick={onClose}
              title={t('studio.results.close')}
              className="inline-flex shrink-0 items-center gap-1 whitespace-nowrap rounded-full border border-snb-hairline bg-snb-elv px-2.5 py-1 text-xs text-snb-t3 transition-colors hover:border-snb-t3 hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-snb-focus"
            >
              {t('studio.results.collapse')}
              <svg
                width="11"
                height="11"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M6 9l6 6 6-6" />
              </svg>
            </button>
          </div>
        </div>

        {/* 票据栈：旧在上、新在下，状态原位变化不重排 */}
        <div className="mt-2">
          <AnimatePresence initial={false}>
            {tasks.map((task) => (
              <motion.div
                key={task.id}
                initial={reduceMotion ? false : { height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={reduceMotion ? { opacity: 0 } : { height: 0, opacity: 0 }}
                transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 360, damping: 34 }}
                className="overflow-hidden"
              >
                <TaskCard
                  task={task}
                  expanded={task.id === effectiveExpanded}
                  now={queue.now}
                  position={task.status === 'queued' ? queuedIds.indexOf(task.id) + 1 : null}
                  canRetry={!queue.queueFull}
                  onToggle={() => setExpandedId(task.id === effectiveExpanded ? null : task.id)}
                  onCancel={() => queue.cancelTask(task.id)}
                  onRetry={() => queue.retryTask(task.id)}
                  onPreview={onPreview}
                />
              </motion.div>
            ))}
          </AnimatePresence>
        </div>

        {/* 「别关标签页」提醒：队列是纯前端的，页面一卸载就静默 abort——钱花了图没了。
            这句在这种地方只能保守，不能替后端许诺「离开也照跑」 */}
        {queue.runningCount > 0 && (
          <div className="mt-3 flex items-start gap-2 rounded-lg border border-snb-hairline-strong bg-snb-well px-3 py-2.5">
            <span className="mt-[5px] flex">
              <StatusLamp state="pending" />
            </span>
            <span className="font-mono text-[11.5px] leading-[1.75] text-snb-t2">
              {st('studio.wait.stayHint')}
            </span>
          </div>
        )}
      </div>
    </motion.div>
  )
}
