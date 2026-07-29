import { lazy, Suspense, useState } from 'react'
import { Alert, Button, MasonryCard } from '../ui'
// 懒加载：走纸描线只在展开的 running 卡挂载，收起的多任务不堆 canvas
// （🪦 余烬画布 PaintingCanvas 2026-07-29 退役：加法混合 + 径向光晕 + 闪烁粒子，
//   撞「零发光」与「粒子招牌全毙」两条红线，还偏偏出现在注意力最高的那两分钟）
const PaperTrace = lazy(() => import('./PaperTrace').then((m) => ({ default: m.PaperTrace })))
import { t } from '../i18n'
import { st } from './i18nStudio'
import { SIZE_PRESETS } from '../lib/sizes'
import { pngDimensions } from '../lib/imageSize'
import { downloadImage } from '../lib/downloadImage'
import { elapsedSecondsOf, type GenTask } from './useGenerationQueue'

interface TaskCardProps {
  task: GenTask
  expanded: boolean
  now: number
  /** queued 任务的队列位次（1 起）；其他状态传 null */
  position: number | null
  canRetry: boolean
  onToggle: () => void
  onCancel: () => void
  onRetry: () => void
  onPreview: (images: string[], index: number) => void
}

// 生成中的阶段文案轮播：每 7 秒换一句，循环（studio.results.stage1..8，一轮 56 秒）。
// 文案 2026-07-29 从画室人格换成机房腔（i18nStudio 覆盖层里是逐字终稿），
// 节奏与「越往后越接近完工」的递进照旧——换的是人格，不是机制。
const STAGE_KEYS = [
  'stage1', 'stage2', 'stage3', 'stage4', 'stage5', 'stage6', 'stage7', 'stage8',
] as const

function gridClass(count: number): string {
  if (count === 1) return 'grid-cols-1'
  if (count === 2) return 'grid-cols-2'
  return 'grid-cols-2 md:grid-cols-4'
}

/** 卡片高度封顶：竖图不按整宽算高，单张/单排稳稳落在托盘视口内（原 ResultsPanel 同款） */
function cardMaxWidth(width: number, height: number): string {
  return `min(100%, calc(min(40vh, 440px) * ${(width / height).toFixed(4)}))`
}

/** size 形如 1024x1536；auto / 非法一律按 1024×1024 兜底 */
function parseSize(size: string): { width: number; height: number } {
  const match = /^(\d+)x(\d+)$/.exec(size.trim())
  if (!match) return { width: 1024, height: 1024 }
  return { width: Number(match[1]), height: Number(match[2]) }
}

function formatClock(seconds: number): string {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

/** 状态记号：running 余烬呼吸点 / queued 空心点 / done ✓ / error ✗ */
function StatusGlyph({ status }: { status: GenTask['status'] }) {
  if (status === 'running') {
    return (
      <span
        aria-hidden="true"
        className="h-2 w-2 flex-none animate-pulse rounded-full bg-primary-500 motion-reduce:animate-none"
      />
    )
  }
  if (status === 'queued') {
    return <span aria-hidden="true" className="h-2 w-2 flex-none rounded-full border border-snb-t3/60" />
  }
  const isDone = status === 'done'
  return (
    <svg
      width="12"
      height="12"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={`flex-none ${isDone ? 'text-primary-500' : 'text-red-700 dark:text-red-400'}`}
    >
      {isDone ? <path d="M20 6 9 17l-5-5" /> : <path d="M6 18 18 6M6 6l12 12" />}
    </svg>
  )
}

/** 收起行右侧的小动作钮（取消/重试）：与行头 button 平级，不嵌套 */
function RowAction({ label, onClick, disabled }: { label: string; onClick: () => void; disabled?: boolean }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="shrink-0 whitespace-nowrap rounded-full border border-snb-hairline bg-snb-elv px-2.5 py-1 text-xs text-snb-t3 transition-colors hover:border-snb-t3 hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50 disabled:cursor-not-allowed disabled:opacity-50"
    >
      {label}
    </button>
  )
}

/** 单任务卡：收起行（记号+prompt+状态徽标+动作）+ 展开体（按状态承载完整暗房显影体验） */
export function TaskCard(p: TaskCardProps) {
  const { task } = p
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null)
  const elapsed = elapsedSecondsOf(task, p.now)
  const { width, height } = parseSize(task.input.size)
  const sizePreset = SIZE_PRESETS.find((preset) => preset.value === task.input.size)
  const sizeLabel = sizePreset ? t(sizePreset.labelKey) : task.input.size
  const qualityLabel = t(`playground.form.qualitiesShort.${task.input.quality}`)

  // 批次全部图 dataUrl：整批传给 Lightbox 做批次内切换对比
  const allDataUrls = task.batch?.images.map((image) => image.dataUrl) ?? []

  async function copyPrompt(index: number) {
    try {
      await navigator.clipboard.writeText(task.input.prompt)
      setCopiedIndex(index)
      setTimeout(() => setCopiedIndex(null), 1500)
    } catch {
      // clipboard 不可用静默
    }
  }

  return (
    <div className="border-t border-snb-hairline first:border-t-0">
      {/* 收起行：行头（记号+prompt+状态徽标）可点切换展开；动作钮平级在右 */}
      <div className="flex items-center gap-2.5 py-2.5">
        <button
          type="button"
          onClick={p.onToggle}
          aria-expanded={p.expanded}
          aria-label={task.input.prompt}
          className="flex min-w-0 flex-1 items-center gap-2.5 rounded-lg px-1 py-1 text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
        >
          <StatusGlyph status={task.status} />
          <span className="min-w-0 flex-1 truncate text-[13.5px] text-snb-t1">{task.input.prompt}</span>
          {task.status === 'running' && (
            <span className="shrink-0 font-mono text-xs tabular-nums text-snb-t3">{formatClock(elapsed)}</span>
          )}
          {task.status === 'queued' && p.position !== null && (
            <span className="shrink-0 rounded-full border border-snb-hairline px-2 py-0.5 text-[11px] text-snb-t3">
              {t('studio.queue.position', { pos: p.position })}
            </span>
          )}
          {task.status === 'error' && (
            <span className="shrink-0 text-xs text-snb-t3">{t('studio.queue.failShort')}</span>
          )}
          {task.status === 'done' && !p.expanded && task.batch && (
            <span className="flex shrink-0 items-center gap-1">
              {task.batch.images.slice(0, 4).map((image, index) => (
                <img
                  key={index}
                  src={image.dataUrl}
                  alt={t('studio.results.alt', { index: index + 1 })}
                  className="h-9 w-9 rounded-md border border-snb-hairline object-cover"
                />
              ))}
            </span>
          )}
          {/* 收展指示：翻转箭头，让每张卡都明显可点收起（站长 2026-07-05 反馈） */}
          <svg
            width="13"
            height="13"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
            className={`shrink-0 text-snb-t3 transition-transform duration-200 ${p.expanded ? 'rotate-180' : ''}`}
          >
            <path d="m6 9 6 6 6-6" />
          </svg>
        </button>
        {(task.status === 'queued' || task.status === 'running') && (
          <RowAction label={t('studio.queue.cancel')} onClick={p.onCancel} />
        )}
        {task.status === 'error' && (
          <RowAction label={t('studio.queue.retry')} onClick={p.onRetry} disabled={!p.canRetry} />
        )}
      </div>

      {/* 展开体：按状态承载完整体验 */}
      {p.expanded && (
        <div className="pb-4">
          <div className="flex justify-end">
            <span className="font-mono text-xs text-snb-t3">
              {/* done 显示实际返回张数（上游可能少给，如请求 2 只回 1）；未完成显示请求 n */}
              {t('studio.results.meta', {
                n: task.status === 'done' && task.batch ? task.batch.images.length : task.input.n,
                size: sizeLabel,
                quality: qualityLabel,
              })}
            </span>
          </div>

          {task.status === 'queued' && (
            <div className="py-6 text-center">
              <span className="inline-flex items-center gap-2 rounded-full border border-snb-hairline bg-snb-elv/60 px-3.5 py-1.5 text-xs text-snb-t3">
                {p.position !== null && (
                  <b className="font-semibold text-snb-t2">{t('studio.queue.position', { pos: p.position })}</b>
                )}
                {t('studio.queue.waiting')}
              </span>
            </div>
          )}

          {task.status === 'running' && (
            <>
              {/* 走纸条的抬头：说清这条线是什么、不是什么（它不代表剩余时间） */}
              <div className="mt-2 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                <span className="font-mono text-[11px] tracking-[0.14em] text-snb-t3">
                  {st('studio.wait.paperLabel')}
                </span>
                <span className="font-mono text-[11.5px] text-snb-t3">{st('studio.wait.traceNote')}</span>
              </div>
              <div className={`mt-2 grid gap-3.5 ${gridClass(task.input.n)}`}>
                {Array.from({ length: task.input.n }, (_, i) => (
                  <div
                    key={i}
                    className="relative mx-auto w-full overflow-hidden rounded-2xl border border-snb-hairline-strong bg-snb-bg"
                    style={{ aspectRatio: `${width} / ${height}`, maxWidth: cardMaxWidth(width, height) }}
                  >
                    {/* 走纸描线：沥青底上一条纸白单色描线向左走纸，像热敏打印机/机房示波器。
                        零发光零粒子零加法混合；只在展开卡挂载 */}
                    <Suspense fallback={null}>
                      <PaperTrace seed={i + 1} />
                    </Suspense>
                    {/* reduced-motion 兜底：画布不启动，只留静态状态点 */}
                    <span
                      aria-hidden="true"
                      className="absolute left-1/2 top-1/2 hidden h-2.5 w-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary-500 motion-reduce:block"
                    />
                  </div>
                ))}
              </div>
              {/* 渐近式进度：按耗时逼近 92% 不封顶——只传达「在推进」，不假装知道剩余时长。
                  机制原样保留（八段 7 秒一换 + 92% 渐近），2026-07-29 只换了材质与人格 */}
              <div className="mt-4">
                <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                  <span className="text-[15px] font-semibold leading-[1.5] text-snb-t1">
                    {st(`studio.results.${STAGE_KEYS[Math.floor(elapsed / 7) % STAGE_KEYS.length]}`)}
                  </span>
                  <span className="font-mono text-xs tabular-nums text-snb-t3">
                    {st('studio.wait.step', { step: (Math.floor(elapsed / 7) % STAGE_KEYS.length) + 1 })}
                  </span>
                </div>
                <div className="mt-2.5 h-2 overflow-hidden rounded-full bg-snb-elv">
                  <div
                    className="h-full rounded-full bg-paper transition-[width] duration-[420ms] ease-[cubic-bezier(0.2,0,0,1)]"
                    style={{ width: `${Math.min(92, Math.round(92 * (1 - Math.exp(-elapsed / 45))))}%` }}
                  />
                </div>
                <p className="mt-2.5 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 text-xs text-snb-t3">
                  <span className="font-mono tabular-nums text-snb-t2">
                    {t('playground.results.elapsed', { seconds: elapsed })} ·{' '}
                    {t('playground.results.expectHint')}
                  </span>
                  <span className="font-mono">{st('studio.wait.note')}</span>
                </p>
              </div>
            </>
          )}

          {task.status === 'error' && task.error && (
            <Alert className="mt-2" tone="danger" title={t('studio.results.failTitle')}>
              <p className="m-0 text-[13.5px] leading-relaxed">{t(`playground.errors.${task.error.key}`)}</p>
              <div className="mt-3">
                <Button variant="secondary" size="sm" onClick={p.onRetry} disabled={!p.canRetry}>
                  {t('studio.results.retry')}
                </Button>
              </div>
            </Alert>
          )}

          {task.status === 'done' && task.batch && task.batch.images.length > 0 && (
            <div className={`mt-2 grid gap-3.5 ${gridClass(task.batch.images.length)}`}>
              {task.batch.images.map((image, index) => {
                // 上游常不认请求 size（如 1:1 4K → 实回 3:2），按图片真实尺寸定比例，
                // 否则 MasonryCard object-cover 会按错误比例把两侧裁掉（2026-07-05 生产坑）；
                // 解析失败（非 PNG）才回退请求 size
                const dim = pngDimensions(image.b64) ?? { width, height }
                return (
                <div
                  key={index}
                  className="mx-auto w-full cursor-zoom-in animate-[fadeUp_0.3s_ease-out_backwards] motion-reduce:animate-none"
                  style={{ animationDelay: `${index * 70}ms`, maxWidth: cardMaxWidth(dim.width, dim.height) }}
                  onClick={() => p.onPreview(allDataUrls, index)}
                >
                  <MasonryCard
                    src={image.dataUrl}
                    alt={t('studio.results.alt', { index: index + 1 })}
                    width={dim.width}
                    height={dim.height}
                    overlay={
                      // min-w-0：flex 子项默认不缩小到内容宽度以下，4 图横排卡窄到「复制提示词」
                      // 5 字排不下时会硬换行、把整行撑到近半张图高（2026-07-05 反馈）；配 truncate 兜底裁字
                      <div className="flex gap-2">
                        <Button
                          variant="primary"
                          size="sm"
                          className="min-w-0 flex-1 truncate"
                          onClick={(event) => {
                            event.stopPropagation()
                            downloadImage(image.dataUrl, `snb-img-${Date.now()}-${index + 1}.png`)
                          }}
                        >
                          {t('studio.results.download')}
                        </Button>
                        <Button
                          variant="overlay"
                          size="sm"
                          className="min-w-0 flex-1 truncate"
                          onClick={(event) => {
                            event.stopPropagation()
                            void copyPrompt(index)
                          }}
                        >
                          {copiedIndex === index ? t('studio.results.copied') : t('studio.results.copy')}
                        </Button>
                      </div>
                    }
                  />
                </div>
                )
              })}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
