import { lazy, Suspense, useEffect, useState } from 'react'
import { AnimatePresence } from 'motion/react'
import { AmbientBackground, Lightbox, Tabs, ThemeScope } from '@super-nb/ui'
import { TopBar } from './studio/TopBar'
import { Composer } from './studio/Composer'
import { ResultsTray } from './studio/ResultsTray'
import { GalleryTab } from './studio/GalleryTab'
// 懒加载：History 首次进入才拉 chunk（HistoryTab 是具名导出，适配成 default）
const HistoryTab = lazy(() => import('./studio/HistoryTab').then((m) => ({ default: m.HistoryTab })))
// 懒加载：Favorites 首次进入才拉 chunk（FavoritesTab 是具名导出，适配成 default）
const FavoritesTab = lazy(() => import('./studio/FavoritesTab').then((m) => ({ default: m.FavoritesTab })))
import { useGenerationQueue } from './studio/useGenerationQueue'
import { useRefImages } from './studio/useRefImages'
import { TAB_ITEMS, type TabId } from './studio/tabs'
import { useEligibleKeys } from './keys/useEligibleKeys'
import { useAuthUser } from './auth/useAuth'
import { useTheme } from './theme'
import { estimateCost } from './lib/cost'
import { SIZE_PRESETS, sizeForRatio } from './lib/sizes'
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

export default function App() {
  const user = useAuthUser()
  const { loading: keysLoading, keys: eligible, rates } = useEligibleKeys()
  const queue = useGenerationQueue()
  const [theme, toggleTheme] = useTheme()

  const [prompt, setPrompt] = useState('')
  // 默认 9:16 竖图 · 2K（站长 2026-07-05 拍板；sizeForRatio('9:16','2K')=1152x2048）
  const [size, setSize] = useState(() => sizeForRatio('9:16', '2K'))
  const [n, setN] = useState(1)
  const [quality, setQuality] = useState<Quality>('medium')
  const [selectedKeyId, setSelectedKeyId] = useState<number | null>(null)
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
    patch: Partial<{ prompt: string; size: string; n: number; quality: Quality; selectedKeyId: number | null }>
  ): void {
    if (patch.prompt !== undefined) setPrompt(patch.prompt)
    if (patch.size !== undefined) setSize(patch.size)
    if (patch.n !== undefined) setN(patch.n)
    if (patch.quality !== undefined) setQuality(patch.quality)
    if (patch.selectedKeyId !== undefined) setSelectedKeyId(patch.selectedKeyId)
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
  const canGenerate =
    user !== null && !queue.queueFull && prompt.trim() !== '' && selectedEntry !== null

  function submit(): void {
    const entry = eligible.find((e) => e.key.id === selectedKeyId)
    if (!entry) return
    setTrayOpen(true)
    const estimate = estimateCost(entry.group, size, n, rates[entry.group.id])
    // 只发已就绪的参考图：加载中的骨架还没有 File，本次生成不带它
    const readyFiles = refs.filter((r) => r.status === 'ready' && r.file).map((r) => r.file as File)
    queue.enqueue({
      apiKey: entry.key.key,
      keyId: entry.key.id,
      groupName: entry.group.name,
      prompt: prompt.trim(),
      size,
      n,
      quality,
      images: readyFiles.length > 0 ? readyFiles : undefined,
      cost: estimate,
    })
  }

  const showEmptyState = user !== null && !keysLoading && eligible.length === 0

  return (
    <ThemeScope theme={theme} className="min-h-screen">
      {theme === 'dark' && <AmbientBackground variant="hero" />}
      <div className="relative z-[1] flex min-h-screen flex-col">
        <TopBar theme={theme} onToggleTheme={toggleTheme} />

        <main className="w-full flex-1">
          {/* 画墙即页面主体：近满屏宽（1760 封顶防超宽屏失控），创作面板收进底部悬浮票据。
              标题降格为眉行——墙本身才是 hero。 */}
          <section className="mx-auto w-full max-w-[1760px] px-5 pb-10 pt-10 sm:px-8">
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
              <h1 className="font-display text-[24px] font-semibold tracking-[0.01em] text-snb-t1">
                {t('studio.hero.title')}
              </h1>
              <p className="text-sm text-snb-t3">{t('studio.hero.subtitle')}</p>
            </div>

            {/* 两面板保持同时挂载，用 hidden 切换以保留滚动与筛选状态 */}
            <Tabs
              className="mb-6 mt-5"
              items={TAB_ITEMS.map((x) => ({ id: x.id, label: t(x.labelKey) }))}
              active={activeTab}
              onSelect={(id) => {
                if (id === 'history') setHistoryVisited(true)
                if (id === 'favorites') setFavoritesVisited(true)
                setActiveTab(id as TabId)
              }}
            />
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
          </section>
        </main>

        <footer className="border-t border-snb-hairline">
          <div className="mx-auto flex max-w-[1760px] flex-wrap justify-between gap-4 px-5 py-5 text-xs text-snb-t3 sm:px-8">
            <span>{t('studio.footer.brand')}</span>
            <span>{t('studio.footer.notice')}</span>
          </div>
        </footer>
        {/* 页尾净空：滚到底时页脚与最后一排内容不被悬浮票据盖住。
            只留票据静息高（≈113px）+底距 14px+一点呼吸位——多留会在页尾堆出大片空白（2026-07-05 反馈） */}
        <div aria-hidden="true" className="h-[max(150px,calc(130px+env(safe-area-inset-bottom)))]" />
      </div>

      {/* 悬浮创作栈：本次生成托盘 + 创作票据，固定视口底部居中。
          外层 pointer-events-none 让票据两侧的墙面仍可点击 */}
      <div className="pointer-events-none fixed inset-x-0 bottom-0 z-40 px-3 pb-[max(14px,env(safe-area-inset-bottom))] sm:px-6">
        <div className="mx-auto flex w-full max-w-[820px] flex-col gap-2.5">
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
          <div className="pointer-events-auto">
            <Composer
              prompt={prompt}
              size={size}
              n={n}
              quality={quality}
              selectedKeyId={selectedKeyId}
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
          </div>
        </div>
      </div>

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
