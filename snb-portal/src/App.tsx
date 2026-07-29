import { lazy, Suspense, useEffect, useState } from 'react'
import { AnimatePresence } from 'motion/react'
import { AmbientBackground, Lightbox, Tabs, ThemeScope } from './ui'
import { TopBar } from './studio/TopBar'
import { Composer } from './studio/Composer'
import { ResultsTray } from './studio/ResultsTray'
import { GalleryTab } from './studio/GalleryTab'
// 懒加载：History 首次进入才拉 chunk（HistoryTab 是具名导出，适配成 default）
const HistoryTab = lazy(() => import('./studio/HistoryTab').then((m) => ({ default: m.HistoryTab })))
// 懒加载：Favorites 首次进入才拉 chunk（FavoritesTab 是具名导出，适配成 default）
const FavoritesTab = lazy(() => import('./studio/FavoritesTab').then((m) => ({ default: m.FavoritesTab })))
import { MAX_CONCURRENT, useGenerationQueue } from './studio/useGenerationQueue'
import { useRefImages } from './studio/useRefImages'
import { GuestSpecBoard } from './studio/GuestSpecBoard'
import { StatusLamp } from './studio/parts'
import { useMediaQuery } from './studio/useMediaQuery'
import { st } from './studio/i18nStudio'
import { TAB_ITEMS, type TabId } from './studio/tabs'
import { useEligibleKeys } from './keys/useEligibleKeys'
import { useAuthUser } from './auth/useAuth'
import { estimateCost } from './lib/cost'
import { SIZE_PRESETS, sizeForRatio } from './lib/sizes'
import { useSelectableModels } from './studio/useSelectableModels'
import { normalizeGrokSize, sizeModeOf } from './lib/modelFamilies'
import { downloadImage } from './lib/downloadImage'
import { t } from './i18n'

export const QUALITIES = ['medium', 'high', 'low', 'auto'] as const
export type Quality = (typeof QUALITIES)[number]

export interface ApplyPayload {
  prompt: string
  params?: { size?: string; quality?: string }
}

// 参考图类型 + 加载态逻辑收在 useRefImages（含选图即落的 loading 骨架）；此处再导出保持既有 import 路径
export type { RefImage } from './studio/useRefImages'

export type { TabId }

const KEY_STORAGE = 'snb-playground-key-id'
const MODEL_STORAGE = 'snb-playground-model'

export default function App() {
  const user = useAuthUser()
  const { loading: keysLoading, keys: eligible, rates } = useEligibleKeys()
  const queue = useGenerationQueue()

  const [prompt, setPrompt] = useState('')
  // 默认 9:16 竖图 · 2K（站长 2026-07-05 拍板；sizeForRatio('9:16','2K')=1152x2048）
  const [size, setSize] = useState(() => sizeForRatio('9:16', '2K'))
  const [n, setN] = useState(1)
  const [quality, setQuality] = useState<Quality>('medium')
  const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null)
  const [model, setModel] = useState<string>('gpt-image-2')
  // 参考图集合（含加载中骨架）+ 会话「最近上传」，加载态流转收在 hook 里
  const { refs, recentUploads, addFiles: onAddRefs, remove: onRemoveRef } = useRefImages()
  const [preview, setPreview] = useState<{ images: string[]; index: number } | null>(null)
  const [activeTab, setActiveTab] = useState<TabId>('gallery')
  const [trayOpen, setTrayOpen] = useState(false)
  // 灵感库「直接使用」的计数信号：Composer 侧据此聚焦输入框 + 光晕脉冲
  const [applySignal, setApplySignal] = useState(0)
  // History 面板首访前不挂载（省首屏 chunk）；访问过即常驻、hidden 切换保活
  const [historyVisited, setHistoryVisited] = useState(false)
  const [favoritesVisited, setFavoritesVisited] = useState(false)

  // keys 就绪后：优先恢复上次选择，否则选第一个（fork watch 同逻辑）
  useEffect(() => {
    if (eligible.length === 0) {
      setSelectedKeyId(null)
      return
    }
    const saved = Number(localStorage.getItem(KEY_STORAGE))
    const restored = eligible.find((e) => e.key.id === saved)
    setSelectedKeyId((restored ?? eligible[0]).key.id)
  }, [eligible])

  useEffect(() => {
    if (selectedKeyId !== null) localStorage.setItem(KEY_STORAGE, String(selectedKeyId))
  }, [selectedKeyId])

  function applyPrompt(item: ApplyPayload): void {
    setPrompt(item.prompt)
    if (item.params?.size && SIZE_PRESETS.some((p) => p.value === item.params?.size)) {
      setSize(item.params.size)
    }
    if (item.params?.quality && (QUALITIES as readonly string[]).includes(item.params.quality)) {
      setQuality(item.params.quality as Quality)
    }
    // 创作栏固定在视口底部、随时可见，无需滚动定位；发信号让它聚焦+脉冲即可
    setApplySignal((s) => s + 1)
  }

  function applyHistory(payload: { prompt: string; size: string; quality: string; n: number }): void {
    applyPrompt({ prompt: payload.prompt, params: { size: payload.size, quality: payload.quality } })
    setN(payload.n)
  }

  function openPreview(images: string[], index: number): void {
    setPreview({ images, index })
  }

  function onChange(
    patch: Partial<{ prompt: string; size: string; n: number; quality: Quality; selectedKeyId: number | null; model: string }>
  ): void {
    if (patch.prompt !== undefined) setPrompt(patch.prompt)
    if (patch.size !== undefined) setSize(patch.size)
    if (patch.n !== undefined) setN(patch.n)
    if (patch.quality !== undefined) setQuality(patch.quality)
    if (patch.selectedKeyId !== undefined) setSelectedKeyId(patch.selectedKeyId)
    if (patch.model !== undefined) setModel(patch.model)
  }

  // Cmd/Ctrl+V 粘贴图片直接作参考图（截图后直接贴，图生图最顺手的动作）。
  // 只在剪贴板确有图片文件时拦截，纯文本粘贴照常进输入框。
  useEffect(() => {
    const onPaste = (e: ClipboardEvent) => {
      const imgs = Array.from(e.clipboardData?.files ?? []).filter((f) => f.type.startsWith('image/'))
      if (imgs.length === 0) return
      e.preventDefault()
      onAddRefs(imgs)
    }
    document.addEventListener('paste', onPaste)
    return () => document.removeEventListener('paste', onPaste)
  }, [onAddRefs])

  const selectedEntry = eligible.find((e) => e.key.id === selectedKeyId) ?? null
  const { models: selectableModels } = useSelectableModels(selectedEntry?.key.key ?? null)

  // 清单就绪后校正选中 model：优先上次、否则第一个；当前 model 不在清单则回落第一个
  useEffect(() => {
    if (selectableModels.length === 0) return
    setModel((cur) => {
      if (selectableModels.includes(cur)) return cur
      const saved = localStorage.getItem(MODEL_STORAGE)
      return saved && selectableModels.includes(saved) ? saved : selectableModels[0]
    })
  }, [selectableModels])

  useEffect(() => {
    localStorage.setItem(MODEL_STORAGE, model)
  }, [model])

  const canGenerate =
    user !== null && !queue.queueFull && prompt.trim() !== '' && selectedEntry !== null

  function submit(): void {
    const entry = eligible.find((e) => e.key.id === selectedKeyId)
    if (!entry) return
    setTrayOpen(true)
    // grok 家族认特定标准 size 值（上游对其余回退）；归一到支持的预设档，同时让预估价对齐
    const effectiveSize = sizeModeOf(model) === 'grokPreset' ? normalizeGrokSize(size) : size
    const estimate = estimateCost(entry.group, effectiveSize, n, rates[entry.group.id])
    // 只发已就绪的参考图：加载中的骨架还没有 File，本次生成不带它
    const readyFiles = refs.filter((r) => r.status === 'ready' && r.file).map((r) => r.file as File)
    queue.enqueue({
      apiKey: entry.key.key,
      keyId: entry.key.id,
      groupName: entry.group.name,
      model,
      prompt: prompt.trim(),
      size: effectiveSize,
      n,
      quality,
      images: readyFiles.length > 0 ? readyFiles : undefined,
      cost: estimate,
    })
  }

  const showEmptyState = user !== null && !keysLoading && eligible.length === 0

  // 票据的落位：≥1280 坐进右栏「柜台格子」（永不压图），窄屏仍是底部浮动票根。
  // 🪦 原来无论多宽都是 820px 硬浮，1920 屏上只占 43%、不透明面板硬边直接切过一张卡片
  const railMode = useMediaQuery('(min-width: 1280px)')
  // 访客规格牌的落位：≥768 坐在画墙之上（评估「值不值得充」的人一进来就看得到）；
  // 手机上它有 500+px 高，压在墙上等于把第一张图又推下去——改挂在墙尾，首屏留给图
  const boardOnTop = useMediaQuery('(min-width: 768px)')
  const guestBoard = user === null ? <GuestSpecBoard /> : null

  // 托盘与票据在两种落位下是同一份 JSX，只换父容器（状态都在 App，跨断点不丢）
  const trayNode = (
    <AnimatePresence>
      {trayOpen && queue.tasks.length > 0 && (
        <ResultsTray
          key="results-tray"
          queue={queue}
          onPreview={openPreview}
          onClose={() => setTrayOpen(false)}
        />
      )}
    </AnimatePresence>
  )

  const ticketNode = (
    <Composer
      prompt={prompt}
      size={size}
      n={n}
      quality={quality}
      selectedKeyId={selectedKeyId}
      model={model}
      selectableModels={selectableModels}
      onChange={onChange}
      eligible={eligible}
      rates={rates}
      runningCount={queue.runningCount}
      queuedCount={queue.queuedCount}
      finishedCount={queue.finishedCount}
      queueFull={queue.queueFull}
      canGenerate={canGenerate}
      onSubmit={submit}
      refs={refs}
      onAddRefs={onAddRefs}
      onRemoveRef={onRemoveRef}
      showEmptyState={showEmptyState}
      applySignal={applySignal}
      showTrayChip={!trayOpen && queue.tasks.length > 0}
      onOpenTray={() => setTrayOpen(true)}
      onCloseTray={() => setTrayOpen(false)}
      recentUploads={recentUploads}
    />
  )

  const panels = (
    <>
      {/* 两面板保持同时挂载，用 hidden 切换以保留滚动与筛选状态 */}
      <div className={activeTab === 'gallery' ? '' : 'hidden'}>
        <GalleryTab onApply={applyPrompt} />
      </div>
      {favoritesVisited && (
        <div className={activeTab === 'favorites' ? '' : 'hidden'}>
          <Suspense fallback={<div className="py-16 text-center text-sm text-snb-t3">…</div>}>
            <FavoritesTab onApply={applyPrompt} active={activeTab === 'favorites'} />
          </Suspense>
        </div>
      )}
      {historyVisited && (
        <div className={activeTab === 'history' ? '' : 'hidden'}>
          <Suspense fallback={<div className="py-16 text-center text-sm text-snb-t3">…</div>}>
            <HistoryTab
              refreshToken={queue.historyVersion}
              onApply={applyHistory}
              onPreview={openPreview}
              onGoGallery={() => setActiveTab('gallery')}
            />
          </Suspense>
        </div>
      )}
    </>
  )

  return (
    <ThemeScope theme="dark" className="min-h-screen">
      <AmbientBackground variant="hero" />
      <div className="relative z-[1] flex min-h-screen flex-col">
        <TopBar />

        <main className="w-full flex-1">
          {/* 画墙即页面主体：近满屏宽（1760 封顶防超宽屏失控）。标题降格为眉行——墙本身才是 hero。 */}
          <section className="mx-auto w-full max-w-[1760px] px-5 pb-10 pt-7 sm:px-8">
            {/* 眉行：机位名 + 一句分寸说明；右侧两枚真数（灵感库条数 / 队列并发）*/}
            <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2.5 pb-3">
              <div className="flex flex-wrap items-baseline gap-x-3.5 gap-y-1">
                <h1 className="font-sans text-[clamp(20px,1.6vw,26px)] font-bold tracking-[0.01em] text-snb-t1">
                  {t('studio.title')}
                </h1>
                <p className="text-[13.5px] text-snb-t2">{st('studio.hero.subtitle')}</p>
              </div>
              <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
                <span className="font-mono text-[12.5px] text-snb-t3">{st('studio.hero.libCount')}</span>
                <span className="flex items-center gap-2.5">
                  {/* 状态灯：真在出图才实心橙 + 呼吸环（「正在发生」），闲着是空心（待开） */}
                  <StatusLamp state={queue.runningCount > 0 ? 'live' : 'pending'} />
                  <span className="text-[13px] text-snb-t2">
                    {st('studio.hero.queueLight', { n: MAX_CONCURRENT })}
                  </span>
                </span>
              </div>
            </div>

            {/* 访客版规格牌：登录后这三格会被真的模型清单与预估填上 */}
            {boardOnTop && guestBoard}

            <Tabs
              className="mb-5 mt-1"
              items={TAB_ITEMS.map((x) => ({ id: x.id, label: t(x.labelKey) }))}
              active={activeTab}
              onSelect={(id) => {
                if (id === 'history') setHistoryVisited(true)
                if (id === 'favorites') setFavoritesVisited(true)
                setActiveTab(id as TabId)
              }}
            />

            {railMode ? (
              /* ≥1280：画墙 + 票据右栏。票据在自己的柜台格子里坐实，永不压图 */
              <div className="grid grid-cols-[minmax(0,1fr)_clamp(340px,26vw,430px)] items-start gap-[clamp(16px,2vw,28px)]">
                <div className="min-w-0">{panels}</div>
                <aside className="sticky top-[84px] min-w-0">
                  <div className="flex flex-col gap-2.5">
                    {trayNode}
                    {ticketNode}
                  </div>
                  <p className="mt-2.5 font-mono text-[11.5px] leading-[1.8] text-snb-t3">
                    {st('studio.rail.note')}
                  </p>
                </aside>
              </div>
            ) : (
              panels
            )}

            {/* 手机档：规格牌挂墙尾（首屏让给图，逛完再看要花多少） */}
            {!boardOnTop && <div className="mt-6">{guestBoard}</div>}
          </section>
        </main>

        <footer className="border-t border-snb-hairline">
          <div className="mx-auto flex max-w-[1760px] flex-wrap justify-between gap-4 px-5 py-5 text-xs text-snb-t3 sm:px-8">
            <span>{t('studio.footer.brand')}</span>
            <span>{t('studio.footer.notice')}</span>
          </div>
        </footer>
        {/* 页尾净空：滚到底时页脚与最后一排内容不被悬浮票据盖住。
            只留票据静息高（≈113px）+底距 14px+一点呼吸位——多留会在页尾堆出大片空白（2026-07-05 反馈）。
            票据坐右栏时不浮在内容上，这块净空就不要了 */}
        {!railMode && (
          <div aria-hidden="true" className="h-[max(150px,calc(130px+env(safe-area-inset-bottom)))]" />
        )}
      </div>

      {/* <1280 的落位：悬浮创作栈固定视口底部，横向铺到与画墙同宽（不再 820 硬浮）。
          外层 pointer-events-none 让票据两侧的墙面仍可点击 */}
      {!railMode && (
        <div className="pointer-events-none fixed inset-x-0 bottom-0 z-40 px-3 pb-[max(14px,env(safe-area-inset-bottom))] sm:px-6">
          <div className="mx-auto flex w-full max-w-[1100px] flex-col gap-2.5">
            {trayNode}
            <div className="pointer-events-auto">{ticketNode}</div>
          </div>
        </div>
      )}

      {preview !== null && (
        <Lightbox
          images={preview.images}
          index={preview.index}
          onIndexChange={(index) => setPreview((p) => (p ? { ...p, index } : p))}
          onClose={() => setPreview(null)}
          onDownload={(index) =>
            downloadImage(preview.images[index], `snb-img-${Date.now()}-${index + 1}.png`)
          }
          alt={(index) => t('studio.results.alt', { index: index + 1 })}
          prevLabel={t('studio.lightbox.prev')}
          nextLabel={t('studio.lightbox.next')}
          closeLabel={t('studio.lightbox.close')}
          downloadLabel={t('studio.results.download')}
        />
      )}
    </ThemeScope>
  )
}
