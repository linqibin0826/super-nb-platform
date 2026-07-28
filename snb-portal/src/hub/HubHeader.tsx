import { AppHeader } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { CONSOLE_ORIGIN } from '../config'
import { t } from '../i18n'

/** hub 顶栏：统一 AppHeader（规范 v1）+ 场景槽（登录态）。照 studio TopBar 裁剪。
 *  用户区契约（2026-07-28 全站统一）：已登录 = 头像 chip（→我的机位）+ 退出幽灵钮
 *  （→fork /logout 登出单点）；未登录 = 登录幽灵 + 开卡上机纸白实心。 */
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
        <>
          <a
            href={`${CONSOLE_ORIGIN}/dashboard`}
            title={user.email}
            className="flex h-[30px] w-[30px] flex-none items-center justify-center rounded-full border border-snb-hairline-strong bg-snb-elv text-xs font-semibold text-snb-t2 no-underline transition-colors hover:text-snb-t1"
          >
            {user.email.charAt(0).toUpperCase()}
          </a>
          <a
            href={`${CONSOLE_ORIGIN}/logout`}
            className="inline-flex items-center whitespace-nowrap rounded-full bg-transparent px-3 py-1.5 text-xs font-medium text-snb-t2 transition-colors hover:text-snb-t1"
          >
            {t('hub.nav.logout')}
          </a>
        </>
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
