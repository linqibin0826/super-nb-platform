import { useEffect, useState } from 'react'
import { AppHeader, ctaAnchorClass, ghostAnchorClass, type SiteNavItem } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { apiFetch, loginUrl } from '../auth/apiFetch'
import { UserMenu } from '../auth/UserMenu'
import { CONSOLE_ORIGIN } from '../config'
import { t } from '../i18n'

/** GET /user/profile 返回体中顶栏只关心余额（后端 dto.User 的 `json:"balance"`，float64） */
interface ProfileBalance {
  balance?: number
}

// 本地开发/路径部署：导航走站内相对路径（生产子域名用 SITE_NAV_ITEMS 的绝对地址）
const DEV_HREFS: Record<string, string> = {
  console: '/dashboard',
  studio: '/studio/',
  hub: 'https://hub.super-nb.me/',
  activity: '/activity/all/',
}
const isLocalDev =
  typeof location !== 'undefined' &&
  (location.hostname === 'localhost' || location.hostname === '127.0.0.1')

/** 主站路径：本地开发同源直达（sub2api dev/本地栈），生产直链控制台域。
 *  登出必须走 fork /logout 单点（墓碑协议唯一真源），子站绝不自己碰 cookie。 */
const consoleHref = (path: string): string => (isLocalDev ? path : `${CONSOLE_ORIGIN}${path}`)

// SITE_NAV_ITEMS 文案是中文常量，双语站点由 i18n 覆盖
const NAV_LABEL_KEYS: Record<string, string> = {
  console: 'studio.nav.console',
  studio: 'studio.title',
  hub: 'studio.nav.hub',
  activity: 'studio.nav.activity',
}

/** 顶栏 = 统一 AppHeader（Header 规范 v2）+ studio 场景槽（主题开关 → 余额 → 头像/登录） */
export function TopBar() {
  const user = useAuthUser()
  const [balance, setBalance] = useState<number | null>(null)

  // 登录后拉真实余额；失败或字段缺失就不渲染余额行（只留头像），绝不摆假数字
  useEffect(() => {
    if (!user) {
      setBalance(null)
      return
    }
    let cancelled = false
    apiFetch<ProfileBalance>('/user/profile')
      .then((profile) => {
        if (!cancelled && typeof profile?.balance === 'number') setBalance(profile.balance)
      })
      .catch(() => {
        /* 静默降级：余额拉不到就不显示 */
      })
    return () => {
      cancelled = true
    }
  }, [user])

  return (
    <AppHeader
      site="studio"
      subtitle={t('studio.title')}
      homeHref={import.meta.env.BASE_URL}
      resolveHref={isLocalDev ? (item: SiteNavItem) => DEV_HREFS[item.key] ?? item.href : undefined}
      labelFor={(item: SiteNavItem) => t(NAV_LABEL_KEYS[item.key])}
      // 主题开关：AppHeader 把它排在场景槽最前（访客态就是「登录」左边）。
      // 组件自带契约接线，这里不用传档位也不用接回调。
      themeToggle
      // <1024 两钮收进导航浮卡（AppHeader 契约）：顶栏只留主题钮 + 菜单钮。
      // 390 宽实测过——44 高胶囊两枚留在顶栏会把菜单钮挤出视口、文档出横向滚动条。
      menuFooter={
        user ? undefined : (
          <>
            <a href={consoleHref('/register')} className={`${ctaAnchorClass} w-full`}>
              {t('studio.nav.signup')}
            </a>
            <a href={loginUrl()} className={`${ghostAnchorClass} w-full`}>
              {t('studio.nav.login')}
            </a>
          </>
        )
      }
    >
      {user ? (
        <>
          {balance !== null && (
            <div className="flex items-baseline gap-1.5">
              <span className="text-[13px] text-snb-t2">{t('studio.nav.balance')}</span>
              <span className="text-[13px] font-semibold tabular-nums text-snb-safety">
                ${balance.toFixed(2)}
              </span>
            </div>
          )}
          <UserMenu
            user={user}
            dashboardHref={consoleHref('/dashboard')}
            logoutHref={consoleHref('/logout')}
            dashboardLabel={t('studio.nav.console')}
            logoutLabel={t('studio.nav.logout')}
            ariaLabel={t('studio.nav.avatar')}
          />
        </>
      ) : (
        // 两条配方来自 vendored lib/cta.ts（与 Button 的 ghost/primary 逐字同源）；
        // 三条配方都不含横向内边距，顶栏内按契约补 px-4。
        // max-lg:hidden = <1024 收进浮卡（同两条在上面的 menuFooter 里整宽再出一份）。
        <>
          <a href={loginUrl()} className={`${ghostAnchorClass} px-4 max-lg:hidden`}>
            {t('studio.nav.login')}
          </a>
          <a href={consoleHref('/register')} className={`${ctaAnchorClass} px-4 max-lg:hidden`}>
            {t('studio.nav.signup')}
          </a>
        </>
      )}
    </AppHeader>
  )
}
