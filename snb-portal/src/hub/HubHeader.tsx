import { AppHeader } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { t } from '../i18n'

/** hub 顶栏：统一 AppHeader（规范 v1）+ 场景槽（主题切换 → 登录态）。照 studio TopBar 裁剪。 */
export function HubHeader() {
  const user = useAuthUser()
  return (
    <AppHeader
      site="hub"
      subtitle={t('hub.title')}
      labelFor={(item) => t(`hub.nav.${item.key}`)}
    >
      {/* 主题开关已下线（港风霓虹改造 2026-07-27，全站恒暗、浅色退役） */}
      {user ? (
        <a
          className="hidden text-sm text-snb-t2 underline-offset-4 hover:text-snb-t1 hover:underline sm:inline"
          href="https://super-nb.me/dashboard"
          title={user.email}
        >
          {user.email}
        </a>
      ) : (
        <a className="text-sm text-snb-t2 underline-offset-4 hover:text-snb-t1 hover:underline" href={loginUrl()}>
          {t('hub.nav.login')}
        </a>
      )}
    </AppHeader>
  )
}
