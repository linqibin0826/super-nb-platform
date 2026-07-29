import { t } from '../i18n'
import { th } from './hubMessages'
import { CONSOLE_ORIGIN } from '../config'

/**
 * hub 页脚（2026-07-29 定稿：三栏内容纪律版）。
 * 🚨「整理声明」和「一手出处纪律」在这里有固定位置——它们是内容纪律不是装饰件，
 * 不许为了版面干净折叠、合并或删掉。底栏那句免责声明沿用现页原文（勿改写）。
 * 全路由可见（App 层 flex 钉底）。
 */
export function HubFooter() {
  return (
    <footer className="hub-foot" data-testid="hub-foot">
      <div className="hub-foot-grid">
        <div>
          <div className="lb">{th('foot.declLabel')}</div>
          <p>{th('foot.decl')}</p>
        </div>
        <div>
          <div className="lb">{th('foot.srcLabel')}</div>
          <p>{th('foot.src')}</p>
        </div>
        <div>
          <div className="lb">{th('foot.goLabel')}</div>
          <div className="links">
            <a href="/">{th('foot.goHome')}</a>
            <a href="https://help.super-nb.me/">{th('foot.goManual')}</a>
            <a href={`${CONSOLE_ORIGIN}/dashboard`}>{th('foot.goConsole')}</a>
          </div>
        </div>
      </div>
      <div className="hub-foot-bar">
        {/* 免责声明沿用现页原文（业务事实，不改写） */}
        <div>
          {th('foot.brand')}
          {'　|　'}
          {t('hub.foot.disclaimer')}
        </div>
      </div>
    </footer>
  )
}
