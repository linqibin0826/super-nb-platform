import { AppHeader } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { UserMenu } from '../auth/UserMenu'
import { CONSOLE_ORIGIN } from '../config'
import { t } from '../i18n'

/** hub 顶栏：统一 AppHeader（规范 v1）+ 场景槽（登录态）。照 studio TopBar 裁剪。
 *  用户区契约（2026-07-28 全站统一）：已登录 = 头像一枚 + 账户下拉（用户头/我的机位/
 *  退出→fork /logout 登出单点）；未登录 = 登录幽灵 + 开卡上机纸白实心。 */
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
        <UserMenu
          user={user}
          dashboardHref={`${CONSOLE_ORIGIN}/dashboard`}
          logoutHref={`${CONSOLE_ORIGIN}/logout`}
          dashboardLabel={t('hub.nav.console')}
          logoutLabel={t('hub.nav.logout')}
          ariaLabel={user.email}
        />
      ) : (
        <>
          <a
            href={loginUrl()}
            className="inline-flex items-center whitespace-nowrap rounded-full bg-transparent px-3 py-1.5 text-xs font-medium text-snb-t2 transition-colors hover:bg-snb-t1/5 hover:text-snb-t1"
          >
            {t('hub.nav.login')}
          </a>
          <a
            href={`${CONSOLE_ORIGIN}/register`}
            className="inline-flex items-center whitespace-nowrap rounded-full bg-paper px-3.5 py-1.5 text-xs font-semibold text-asphalt no-underline transition-colors hover:bg-[#E2DDD3]"
          >
            {t('hub.nav.signup')}
          </a>
        </>
      )}
    </AppHeader>
  )
}
