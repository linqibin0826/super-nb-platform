import { useEffect, useRef, useState, type DragEvent as ReactDragEvent } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { Button } from '../ui'
import { RefPicker } from './RefPicker'
import { RefStrip } from './RefStrip'
import { MAX_REFS } from './useRefImages'
import { SpecPanel } from './composer/SpecPanel'
import { queueLabel } from './queueLabel'
import { composeSize, deriveFromSize, type LocalSpec } from './composer/spec'
import { estimateCost } from '../lib/cost'
import { billingTierOrDefault, clampTierForRatio, isValidGptImageSize } from '../lib/sizes'
import { t } from '../i18n'
import { keysUrl, loginUrl } from '../auth/apiFetch'
import { useAuthUser } from '../auth/useAuth'
import type { EligibleKey } from '../types'
import type { Quality, RefImage } from '../App'

/** 输入框自动增高上限（约 6 行），超出转内部滚动 */
const MAX_INPUT_HEIGHT = 168

interface ComposerProps {
  prompt: string
  size: string
  n: number
  quality: Quality
  selectedKeyId: number | null
  onChange: (
    patch: Partial<{ prompt: string; size: string; n: number; quality: Quality; selectedKeyId: number | null }>
  ) => void
  eligible: EligibleKey[]
  rates: Record<number, number>
  /** 生成中任务数：>0 时票据呼吸光环 + 胶囊余烬点 */
  runningCount: number
  queuedCount: number
  finishedCount: number
  /** 队列满（排队+生成中 ≥ 10）：主按钮禁用 + 满员提示 */
  queueFull: boolean
  canGenerate: boolean
  onSubmit: () => void
  refs: RefImage[]
  onAddRefs: (files: FileList | File[]) => void
  onRemoveRef: (id: string) => void
  /** 登录但无可用 Key：配置区换引导文案，主按钮换成去密钥页 */
  showEmptyState: boolean
  /** 灵感库「直接使用」的计数信号：每次 +1 触发输入框聚焦 + 赤陶光晕脉冲 */
  applySignal: number
  /** 托盘已收起且有可回看内容（生成中/完成批次/报错）：显示「本次生成」唤出胶囊。
   *  生成中必须给入口——展开配置会互斥收托盘，没胶囊进度就找不回来了 */
  showTrayChip: boolean
  onOpenTray: () => void
  /** 手动展开配置区时收起托盘：两块同时展开在矮屏会把托盘顶出视口 */
  onCloseTray: () => void
  /** 本次会话上传过的参考图（App 留存），供选择器一键复用 */
  recentUploads: RefImage[]
}

/** 悬浮创作票据：固定视口底部；收起 = 一行票根（摘要胶囊+输入+价格+生成），
 *  展开 = 配置区从虚线撕票口上方展开（比例/画质/张数/参考图/Key，直选无分隔线）。 */
export function Composer(p: ComposerProps) {
  const user = useAuthUser()
  const reduceMotion = useReducedMotion()
  const taRef = useRef<HTMLTextAreaElement>(null)
  const tileRef = useRef<HTMLButtonElement>(null)
  const pickerWrapRef = useRef<HTMLDivElement>(null)
  const [expanded, setExpanded] = useState(false)
  const [pulsing, setPulsing] = useState(false)
  const [pickerOpen, setPickerOpen] = useState(false)
  const [dragCount, setDragCount] = useState(0)
  const [spec, setSpec] = useState<LocalSpec>(() => deriveFromSize(p.size))
  const lastEmitted = useRef(p.size)

  const entry = p.eligible.find((e) => e.key.id === p.selectedKeyId) ?? null
  const estimate = entry ? estimateCost(entry.group, p.size, p.n, p.rates[entry.group.id]) : null

  // 外部改 size（灵感库套用规格 / 历史再次生成）→ 反推比例/画质本地态
  useEffect(() => {
    if (p.size === lastEmitted.current) return
    lastEmitted.current = p.size
    setSpec(deriveFromSize(p.size))
  }, [p.size])

  // 生成即收起展开的配置区：把屏幕让位给上方滑出的结果托盘（叠着不收会把托盘顶出矮屏视口）
  function submit(): void {
    setExpanded(false)
    p.onSubmit()
  }

  // custom 非法（越界/非16倍数/半输入）不可生成；固定比例档由 validTiersForRatio 保证合法
  const canSubmit = p.canGenerate && (spec.ratio !== 'custom' || isValidGptImageSize(composeSize(spec) ?? ''))

  function updateSpec(patch: Partial<LocalSpec>): void {
    const next = { ...spec, ...patch }
    // 切比例后当前画质档若在新比例下越界（如切到方形时的 4K 超像素），自动落到最近的有效档
    if (patch.ratio && patch.ratio !== 'custom') {
      next.tier = clampTierForRatio(patch.ratio, next.tier)
    }
    setSpec(next)
    const composed = composeSize(next)
    if (composed !== null && composed !== lastEmitted.current) {
      lastEmitted.current = composed
      p.onChange({ size: composed })
    }
  }

  // 输入框随内容自动增高（到上限转滚动）。除内容变化外，宽度变化也要重算——
  // 同行的胶囊/按钮出现或消失（如「本次生成」「取消」）会挤窄输入框、换行数改变，
  // 只依赖 prompt 会把新换出来的行裁掉，故加 ResizeObserver
  useEffect(() => {
    const ta = taRef.current
    if (!ta) return
    const grow = () => {
      ta.style.height = 'auto'
      ta.style.height = `${Math.min(ta.scrollHeight, MAX_INPUT_HEIGHT)}px`
    }
    grow()
    const observer = new ResizeObserver(grow)
    observer.observe(ta)
    return () => observer.disconnect()
  }, [p.prompt])

  // 灵感库「直接使用」：聚焦输入框 + 光晕脉冲提示「填到这里了」
  const lastSignal = useRef(p.applySignal)
  useEffect(() => {
    if (p.applySignal === lastSignal.current) return
    lastSignal.current = p.applySignal
    taRef.current?.focus()
    setPulsing(true)
  }, [p.applySignal])

  // 无可用 Key 时自动展开，让引导文案直接可见。
  // 延迟 400ms：useEligibleKeys 的 loading 初始为 false，首帧 showEmptyState 会短暂为 true，
  // 立即展开会被这一帧误触发；密钥在窗口期内到位则取消
  useEffect(() => {
    if (!p.showEmptyState) return
    const timer = setTimeout(() => setExpanded(true), 400)
    return () => clearTimeout(timer)
  }, [p.showEmptyState])

  // 选择器：点外面 / Esc 关闭（瓦片与面板自身除外——瓦片点击自己负责开关）
  useEffect(() => {
    if (!pickerOpen) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setPickerOpen(false)
    }
    const onDown = (e: PointerEvent) => {
      const target = e.target as Node
      if (pickerWrapRef.current?.contains(target) || tileRef.current?.contains(target)) return
      setPickerOpen(false)
    }
    document.addEventListener('keydown', onKey)
    document.addEventListener('pointerdown', onDown)
    return () => {
      document.removeEventListener('keydown', onKey)
      document.removeEventListener('pointerdown', onDown)
    }
  }, [pickerOpen])

  // 整卡拖拽收图：dragenter/leave 成对计数（子元素间穿梭会连环触发）
  const dragging = dragCount > 0
  function hasFiles(e: ReactDragEvent): boolean {
    return Array.from(e.dataTransfer.types).includes('Files')
  }

  // 摘要胶囊：张数 · 形状（比例/自动/自定义宽×高）· 画质档 · 图×N
  const shapeLabel = spec.ratio === 'custom' ? p.size.replace('x', '×') : spec.ratio
  const tierLabel = spec.ratio === 'custom' ? billingTierOrDefault(p.size) : spec.tier
  const summaryParts = [t('studio.editor.countUnit', { n: p.n }), shapeLabel]
  if (tierLabel) summaryParts.push(tierLabel)
  if (p.refs.length > 0) summaryParts.push(t('studio.composer.refCount', { count: p.refs.length }))
  const summary = summaryParts.join(' · ')

  const expandTransition = reduceMotion
    ? { duration: 0 }
    : { type: 'spring' as const, stiffness: 420, damping: 38 }

  return (
    <motion.div
      initial={reduceMotion ? false : { y: 28, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 300, damping: 30 }}
      className="relative"
      onDragEnter={(e) => {
        if (!hasFiles(e)) return
        e.preventDefault()
        setDragCount((c) => c + 1)
      }}
      onDragOver={(e) => {
        if (hasFiles(e)) e.preventDefault()
      }}
      onDragLeave={(e) => {
        if (!hasFiles(e)) return
        setDragCount((c) => Math.max(0, c - 1))
      }}
      onDrop={(e) => {
        if (!hasFiles(e)) return
        e.preventDefault()
        setDragCount(0)
        if (e.dataTransfer.files.length) p.onAddRefs(e.dataTransfer.files)
      }}
    >
      {/* 参考图选择器：挂在票据面板外（面板 overflow-hidden 会裁掉向上弹出的浮层） */}
      <AnimatePresence>
        {pickerOpen && (
          <div ref={pickerWrapRef}>
            <RefPicker
              onClose={() => setPickerOpen(false)}
              onAddFiles={(files) => p.onAddRefs(files)}
              recentUploads={p.recentUploads}
            />
          </div>
        )}
      </AnimatePresence>

      {/* 「直接使用」后的一次性赤陶光晕：独立覆盖层做 box-shadow 扩散，不与票据自身投影打架 */}
      {pulsing && (
        <span
          aria-hidden="true"
          onAnimationEnd={() => setPulsing(false)}
          className="pointer-events-none absolute inset-0 rounded-[22px] animate-[snbComposerPulse_0.9s_ease-out] motion-reduce:animate-none"
        />
      )}
      {/* 生成中的呼吸光环：复用品牌 snbDotPulse（2.2s 无限），同样走独立覆盖层 */}
      {p.runningCount > 0 && (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 rounded-[22px] animate-snb-dot motion-reduce:animate-none"
        />
      )}
      <span className="sr-only" aria-live="polite">
        {pulsing ? t('studio.composer.applied') : ''}
      </span>

      <div className="relative overflow-hidden rounded-[22px] border border-snb-hairline-strong bg-snb-panel/90 shadow-[0_16px_40px_-10px_rgba(70,50,38,0.30)] backdrop-blur-xl dark:shadow-[0_18px_48px_-10px_rgba(0,0,0,0.6)]">
        {/* 拖拽悬停提示：整卡任意位置松手即加参考图 */}
        {dragging && (
          <div className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center rounded-[22px] border-2 border-dashed border-primary-500 bg-primary-500/10">
            <span className="rounded-full bg-snb-panel/90 px-4 py-2 text-sm text-snb-t1 shadow-card">
              {t('studio.composer.dropHint')}
            </span>
          </div>
        )}
        {/* 展开的配置区：从撕票口上方展开 */}
        <AnimatePresence initial={false}>
          {expanded && (
            <motion.div
              key="config"
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={expandTransition}
              className="overflow-hidden"
            >
              <SpecPanel
                spec={spec}
                sizeText={p.size}
                showEmptyState={p.showEmptyState}
                hasUser={!!user}
                n={p.n}
                eligible={p.eligible}
                selectedKeyId={p.selectedKeyId}
                updateSpec={updateSpec}
                onChangeN={(n) => p.onChange({ n })}
                onChangeKey={(id) => p.onChange({ selectedKeyId: id })}
              />

              {/* 撕票口：虚线 + 两端缺口（overflow-hidden 恰好裁出半圆缺口），
                  分隔「展开的配置」与「常驻的票根输入行」 */}
              <div className="relative mt-1.5 border-t border-dashed border-snb-hairline-strong" aria-hidden="true">
                <span className="absolute left-0 top-0 h-2.5 w-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-snb-bg ring-1 ring-inset ring-snb-hairline-strong" />
                <span className="absolute right-0 top-0 h-2.5 w-2.5 translate-x-1/2 -translate-y-1/2 rounded-full bg-snb-bg ring-1 ring-inset ring-snb-hairline-strong" />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* 票根第一行：参考图入口（=选择器开关，常驻）+ 大输入框。已选样片下沉到下方样片行 */}
        <div className="flex items-start gap-2.5 px-3.5 pt-3 sm:px-4">
          <button
            ref={tileRef}
            type="button"
            onClick={() => setPickerOpen((v) => !v)}
            disabled={p.refs.length >= MAX_REFS}
            aria-haspopup="dialog"
            aria-expanded={pickerOpen}
            aria-label={t('studio.composer.pickerTitle')}
            title={
              p.refs.length >= MAX_REFS
                ? t('studio.composer.refFull', { max: MAX_REFS })
                : t('studio.composer.pickerTitle')
            }
            className="flex h-14 w-14 flex-none flex-col items-center justify-center gap-0.5 rounded-xl border border-dashed border-snb-hairline-strong text-snb-t3 transition-colors duration-200 hover:border-snb-t3 hover:text-snb-t2 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              aria-hidden="true"
            >
              <path d="M12 5v14M5 12h14" />
            </svg>
            <span className="text-[10px]">{t('studio.editor.refAdd')}</span>
          </button>

          <textarea
            ref={taRef}
            rows={2}
            value={p.prompt}
            placeholder={t('studio.editor.promptPlaceholder')}
            aria-label={t('playground.form.prompt')}
            onChange={(e) => p.onChange({ prompt: e.target.value })}
            onKeyDown={(e) => {
              if ((e.metaKey || e.ctrlKey) && e.key === 'Enter' && canSubmit) {
                e.preventDefault()
                submit()
              }
            }}
            className="max-h-[168px] min-h-[58px] min-w-0 flex-1 resize-none overflow-y-auto border-0 bg-transparent px-1 py-1.5 text-[15px] leading-[1.6] text-snb-t1 outline-none placeholder:text-snb-t3 focus:ring-0"
          />
        </div>

        {/* 参考图样片行：有图才展开（高度动画），横向铺开、可滑、行尾计数 */}
        <AnimatePresence initial={false}>
          {p.refs.length > 0 && (
            <motion.div
              key="refstrip"
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={expandTransition}
              className="overflow-hidden px-3.5 sm:px-4"
            >
              <div className="pt-2">
                <RefStrip refs={p.refs} onRemove={p.onRemoveRef} max={MAX_REFS} />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* 票根第二行：摘要胶囊（=展开开关）+ 唤回托盘 + 价格 + 主操作 */}
        <div className="flex items-center gap-2.5 px-3.5 pb-3 pt-1.5 sm:px-4">
          <button
            type="button"
            onClick={() => {
              const next = !expanded
              setExpanded(next)
              if (next) p.onCloseTray()
            }}
            aria-expanded={expanded}
            aria-label={expanded ? t('studio.composer.collapse') : t('studio.composer.expand')}
            title={expanded ? t('studio.composer.collapse') : t('studio.composer.expand')}
            className="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border border-snb-hairline bg-snb-elv/80 px-3 py-1.5 text-xs text-snb-t2 transition-colors hover:border-snb-t3 hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
          >
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              aria-hidden="true"
            >
              <path d="M4 8h10M18 8h2M4 16h4M12 16h8" />
              <circle cx="16" cy="8" r="2" />
              <circle cx="10" cy="16" r="2" />
            </svg>
            {!p.showEmptyState && <span className="hidden sm:inline">{summary}</span>}
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
              className={`transition-transform duration-200 ${expanded ? 'rotate-180' : ''}`}
            >
              <path d="m18 15-6-6-6 6" />
            </svg>
          </button>

          <div className="min-w-0 flex-1" />

          {p.showTrayChip && (
            <button
              type="button"
              onClick={p.onOpenTray}
              className="flex shrink-0 items-center gap-1.5 whitespace-nowrap rounded-full border border-snb-hairline bg-snb-elv/80 px-3 py-1.5 text-xs text-snb-t2 transition-colors hover:border-snb-t3 hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
            >
              {/* 生成中收着托盘：余烬呼吸点提示「里面正在生成」 */}
              {p.runningCount > 0 && (
                <span
                  aria-hidden="true"
                  className="h-2 w-2 animate-pulse rounded-full bg-primary-500 motion-reduce:animate-none"
                />
              )}
              {queueLabel(p.runningCount, p.queuedCount, p.finishedCount) || t('studio.queue.title')}
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
                <path d="m18 15-6-6-6 6" />
              </svg>
            </button>
          )}

          {/* 本单价格：带「预估」小字提醒口径，悬停见完整说明（按官方价计） */}
          {user !== null && !p.showEmptyState && estimate !== null && (
            <span
              title={t('studio.editor.estimateNote')}
              className="shrink-0 cursor-default text-snb-t3"
            >
              <span className="mr-1 text-[11px]">{t('studio.composer.estimateLabel')}</span>
              <span className="font-mono text-[13px] tabular-nums">
                ${estimate.toFixed(2)}
              </span>
            </span>
          )}

          {user === null ? (
            <Button
              variant="primary"
              size="md"
              className="shrink-0"
              onClick={() => {
                window.location.href = loginUrl()
              }}
            >
              {t('studio.loginToCreate')}
            </Button>
          ) : p.showEmptyState ? (
            <Button
              variant="secondary"
              size="md"
              className="shrink-0"
              onClick={() => {
                window.location.href = keysUrl()
              }}
            >
              {t('playground.empty.goKeys')}
            </Button>
          ) : (
            /* 主 CTA：赤陶键（签名按压）。生成中不变身取消——队列时代随时可再提交，取消进托盘卡片 */
            <Button
              variant="primary"
              size="md"
              className="shrink-0"
              disabled={!canSubmit}
              onClick={submit}
              title={p.queueFull ? t('studio.queue.fullHint') : undefined}
            >
              {t('studio.editor.generate')}
            </Button>
          )}
        </div>
      </div>
    </motion.div>
  )
}
