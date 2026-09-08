import { AppHeader, ctaAnchorClass, ghostAnchorClass } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { UserMenu } from '../auth/UserMenu'
import { CONSOLE_ORIGIN } from '../config'
import { t } from '../i18n'

/** hub 顶栏：统一 AppHeader（Header 规范 v2）+ 场景槽（登录态）。照 studio TopBar 裁剪。
 *  用户区契约（2026-07-28 全站统一）：已登录 = 头像一枚 + 账户下拉（用户头/控制台/
 *  退出→fork /logout 登出单点）；未登录 = 登录幽灵 + 免费注册强调色实心。 */
export function HubHeader() {
  const user = useAuthUser()
  return (
    <AppHeader
      site="hub"
      subtitle={t('hub.title')}
      labelFor={(item) => t(`hub.nav.${item.key}`)}
      // 主题开关：排在场景槽最前（见 AppHeader）。内容中心是长文站，浅色档尤其要有
      themeToggle
      // <1024 两钮收进导航浮卡（AppHeader 契约）：顶栏只留主题钮 + 菜单钮。
      // 390 宽实测过——44 高胶囊两枚留在顶栏会把菜单钮挤出视口、文档出横向滚动条。
      menuFooter={
        user ? undefined : (
          <>
            <a href={`${CONSOLE_ORIGIN}/register`} className={`${ctaAnchorClass} w-full`}>
              {t('hub.nav.signup')}
            </a>
            <a href={loginUrl()} className={`${ghostAnchorClass} w-full`}>
              {t('hub.nav.login')}
            </a>
          </>
        )
      }
    >
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
          <a href={loginUrl()} className={`${ghostAnchorClass} px-4 max-lg:hidden`}>
            {t('hub.nav.login')}
          </a>
          <a href={`${CONSOLE_ORIGIN}/register`} className={`${ctaAnchorClass} px-4 max-lg:hidden`}>
            {t('hub.nav.signup')}
          </a>
        </>
      )}
    </AppHeader>
  )
}
