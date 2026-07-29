import { useEffect, useRef, useState } from 'react'
import { Button } from '../ui'
import { t } from '../i18n'
import { ti } from './copy'

/** 首次进站「开票须知」单:三步办事顺序 + 关键规则(规则讲三遍是这一站最值钱的东西,一句不减)。
 *  出口三个(2026-07-29 定稿,原先只有一个「知道了」——手机没有 Esc 键,等于没有出口):
 *  - 「知道了,开始填票」= 永久已读,盖「已阅」章后回调 onConfirm(服务端记,换设备也不弹);
 *  - 「稍后再看」/ 右上角 ✕ 44×44 / 点遮罩 / Esc = onSkip,只跳过这一次,不落任何存储。
 *  拦路范围也从三条路由收成一条(只在「申请开票」弹),由 App.tsx 按路由控制。 */
export function FirstVisitGuide({ onConfirm, onSkip }: { onConfirm: () => void; onSkip: () => void }) {
  const [closing, setClosing] = useState(false)
  const cardRef = useRef<HTMLDivElement>(null)

  const close = () => {
    if (closing) return
    setClosing(true)
    window.setTimeout(onConfirm, 320)
  }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !closing) onSkip()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [closing])

  return (
    <div className="iv-guide-mask" role="presentation" onClick={() => !closing && onSkip()}>
      <div
        ref={cardRef}
        className="iv-guide-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="iv-guide-title"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => {
          // 焦点陷阱:Tab/Shift+Tab 在卡内循环,防键盘穿透到遮罩背后操作看不见的元素
          if (e.key !== 'Tab') return
          const nodes = cardRef.current?.querySelectorAll<HTMLElement>(
            'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])',
          )
          if (!nodes || nodes.length === 0) return
          const first = nodes[0]
          const last = nodes[nodes.length - 1]
          if (e.shiftKey && document.activeElement === first) {
            e.preventDefault()
            last.focus()
          } else if (!e.shiftKey && document.activeElement === last) {
            e.preventDefault()
            first.focus()
          }
        }}
      >
        <div className="iv-fapiao px-6 py-6">
          {!closing && (
            <button
              type="button"
              className="iv-guide-x"
              aria-label={ti('invoice.guide.close')}
              onClick={onSkip}
            >
              ✕
            </button>
          )}
          <div className={`iv-guide-dim ${closing ? 'off' : ''}`}>
            <div className="iv-fp-title" id="iv-guide-title">{t('invoice.guide.title')}</div>
            <div className="iv-fp-title-rule" />
            <div className="mt-4">
              {(['step1', 'step2', 'step3'] as const).map((k, i) => (
                <div key={k} className="iv-guide-step">
                  <span className="num">{'壹贰叁'[i]}</span>
                  <span>{t(`invoice.guide.${k}`)}</span>
                </div>
              ))}
            </div>
            <div className="iv-guide-rule">
              <span className="iv-stamp-mian" aria-hidden="true">免</span>
              <span>
                <b className="main">{t('invoice.guide.ruleMain')}</b>
                <span className="sub">{t('invoice.guide.ruleSub')}</span>
              </span>
            </div>
            <div className="iv-guide-foot">
              <Button
                variant="primary"
                autoFocus
                className="h-12 w-full sm:h-11 sm:w-auto"
                onClick={close}
              >
                {t('invoice.guide.cta')}
              </Button>
              <Button variant="ghost" className="h-11 w-full sm:w-auto" onClick={onSkip}>
                {ti('invoice.guide.later')}
              </Button>
              <span className="iv-guide-exit">{ti('invoice.guide.exitNote')}</span>
            </div>
          </div>
          {closing && (
            <div className="iv-guide-seal" aria-hidden="true">
              <span className="box">{t('invoice.guide.read')}</span>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
