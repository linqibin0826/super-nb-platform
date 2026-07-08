import { useRef } from 'react'
import { motion, useReducedMotion } from 'motion/react'
import { t } from '../i18n'
import type { RefImage } from '../App'

interface Props {
  onClose: () => void
  onAddFiles: (files: File[]) => void
  /** 本次会话上传过的图（App 侧留存），点击一键复用 */
  recentUploads: RefImage[]
}

/** 缩略格子：点击=用作参考图 */
function Thumb(props: { dataUrl: string; onUse: () => void }) {
  return (
    <button
      type="button"
      title={t('studio.composer.pickerUse')}
      onClick={props.onUse}
      className="group relative block aspect-square w-full overflow-hidden rounded-lg border border-snb-hairline p-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
    >
      <img
        src={props.dataUrl}
        alt=""
        loading="lazy"
        className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105 motion-reduce:transform-none"
      />
      <span className="absolute inset-0 bg-primary-500/0 transition-colors group-hover:bg-primary-500/15" />
    </button>
  )
}

/** 参考图选择器：上传区（点击/拖拽）+ 最近上传（本次会话）。
 *  从创作票据的参考图瓦片上方弹出。 */
export function RefPicker({ onClose, onAddFiles, recentUploads }: Props) {
  const reduceMotion = useReducedMotion()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const hasRecents = recentUploads.length > 0

  return (
    <motion.div
      initial={reduceMotion ? false : { y: 10, opacity: 0, scale: 0.98 }}
      animate={{ y: 0, opacity: 1, scale: 1 }}
      exit={reduceMotion ? { opacity: 0 } : { y: 8, opacity: 0, scale: 0.98 }}
      transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 420, damping: 34 }}
      className="absolute bottom-[calc(100%+10px)] left-0 z-20 w-full origin-bottom-left overflow-hidden rounded-[18px] border border-snb-hairline-strong bg-snb-panel/95 shadow-[0_16px_40px_-10px_rgba(70,50,38,0.30)] backdrop-blur-xl dark:shadow-[0_18px_48px_-10px_rgba(0,0,0,0.6)] sm:w-[580px]"
      role="dialog"
      aria-label={t('studio.composer.pickerTitle')}
    >
      <div className="max-h-[58vh] overflow-y-auto p-5">
        <div className="flex items-center justify-between">
          <p className="text-sm font-medium text-snb-t1">{t('studio.composer.pickerTitle')}</p>
          <button
            type="button"
            onClick={onClose}
            aria-label={t('studio.history.close')}
            className="inline-flex h-6 w-6 items-center justify-center rounded-full p-0 text-snb-t3 transition-colors hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
          >
            <svg
              width="11"
              height="11"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              aria-hidden="true"
            >
              <path d="M6 18 18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* 上传区：点击或拖拽 */}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          multiple
          className="hidden"
          aria-hidden="true"
          onChange={(e) => {
            if (e.target.files?.length) onAddFiles(Array.from(e.target.files))
            e.target.value = ''
          }}
        />
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          onDragOver={(e) => e.preventDefault()}
          onDrop={(e) => {
            e.preventDefault()
            if (e.dataTransfer.files.length) onAddFiles(Array.from(e.dataTransfer.files))
          }}
          className="mt-3 flex w-full flex-col items-center justify-center gap-1 rounded-[14px] border border-dashed border-snb-hairline-strong px-4 py-8 text-snb-t3 transition-colors hover:border-snb-t3 hover:text-snb-t2 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
        >
          <svg
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.8"
            strokeLinecap="round"
            strokeLinejoin="round"
            aria-hidden="true"
          >
            <path d="M12 16V4m0 0 4 4m-4-4-4 4" />
            <path d="M4 16v3a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-3" />
          </svg>
          <span className="text-[13.5px] text-snb-t2">{t('studio.composer.pickerUpload')}</span>
          <span className="text-[11.5px]">{t('studio.composer.pickerFormats')}</span>
        </button>

        {/* 最近上传（本次会话） */}
        {!hasRecents ? (
          <p className="mt-4 text-center text-xs leading-relaxed text-snb-t3">
            {t('studio.composer.pickerEmpty')}
          </p>
        ) : (
          <>
            <p className="mt-4 text-xs tracking-[0.06em] text-snb-t3">
              {t('studio.composer.pickerRecentUp')}
            </p>
            <div className="mt-2 grid grid-cols-4 gap-2 sm:grid-cols-5">
              {recentUploads.map((item) => (
                <Thumb
                  key={item.id}
                  dataUrl={item.url}
                  onUse={() => {
                    if (item.file) onAddFiles([item.file])
                  }}
                />
              ))}
            </div>
          </>
        )}
      </div>
    </motion.div>
  )
}
