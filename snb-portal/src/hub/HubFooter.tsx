import { t } from '../i18n'

/** hub 页脚：整理来源声明（内容整理自互联网须注明出处 + 侵权联删，2026-07-12 站长拍板），全路由可见。 */
export function HubFooter() {
  return (
    <footer className="hub-foot" data-testid="hub-foot">
      <p>{t('hub.foot.disclaimer')}</p>
    </footer>
  )
}
