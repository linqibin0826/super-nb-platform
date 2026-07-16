import { useEffect, useState } from 'react'
import { Button } from '../ui'
import { t } from '../i18n'

/** 首次进站「开票须知」单:三步办事顺序 + 关键规则。
 *  确认(=永久已读)的唯一出口是「知道了」按钮——盖「已阅」章后回调 onConfirm;
 *  点遮罩不关闭(防误触永久已读),Esc = onSkip 临时跳过(不落库,下次再提醒)。 */
export function FirstVisitGuide({ onConfirm, onSkip }: { onConfirm: () => void; onSkip: () => void }) {
  const [closing, setClosing] = useState(false)

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
    <div className="iv-guide-mask" role="presentation">
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
            <div className="iv-guide-rule">
              <span className="iv-stamp-mian" aria-hidden="true">免</span>
              <span>
                <b className="main">{t('invoice.guide.ruleMain')}</b>
                <span className="sub">{t('invoice.guide.ruleSub')}</span>
              </span>
            </div>
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
