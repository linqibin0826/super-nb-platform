import { useEffect, useState } from 'react'
import { Button } from '../ui'
import { t } from '../i18n'

/** 首次进站「开票须知」单:三步办事顺序 + 关键规则;
 *  点「知道了」盖「已阅」章(240ms)后关闭。弹与不弹由 useGuideAck 决定(服务端真源)。 */
export function FirstVisitGuide({ onDismiss }: { onDismiss: () => void }) {
  const [closing, setClosing] = useState(false)

  const close = () => {
    if (closing) return
    setClosing(true)
    window.setTimeout(onDismiss, 320)
  }

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') close()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [closing])

  return (
    <div className="iv-guide-mask" onClick={close} role="presentation">
      <div
        className="iv-guide-card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="iv-guide-title"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="iv-fapiao px-6 py-6">
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
            <div className="iv-guide-rule">{t('invoice.guide.rule')}</div>
            <div className="mt-5 text-center">
              <Button variant="primary" autoFocus onClick={close}>
                {t('invoice.guide.cta')}
              </Button>
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
