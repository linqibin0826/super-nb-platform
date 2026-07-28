import { useEffect, useState } from 'react'
import { AppHeader, type SiteNavItem } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { apiFetch, loginUrl } from '../auth/apiFetch'
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

/** 顶栏 = 统一 AppHeader（规范 v1）+ studio 场景槽（主题切换 → 余额 → 头像/登录） */
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
    >
      {/* 主题开关已下线（港风霓虹改造 2026-07-27，全站恒暗、浅色退役） */}

      {user ? (
        <>
          {balance !== null && (
            <div className="flex items-baseline gap-1.5">
              <span className="text-[11px] text-snb-t3">{t('studio.nav.balance')}</span>
              <span className="font-mono text-[13.5px] font-semibold text-snb-t1">
                ${balance.toFixed(2)}
              </span>
            </div>
          )}
          <a
            aria-label={t('studio.nav.avatar')}
            href={consoleHref('/dashboard')}
            className="flex h-[30px] w-[30px] flex-none items-center justify-center rounded-full border border-snb-hairline-strong bg-snb-elv text-xs font-semibold text-snb-t2 no-underline transition-colors hover:text-snb-t1"
          >
            {user.email.charAt(0).toUpperCase()}
          </a>
          <a
            href={consoleHref('/logout')}
            className="inline-flex items-center whitespace-nowrap rounded-full bg-transparent px-3 py-1.5 text-xs font-medium text-snb-t2 transition-colors hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
          >
            {t('studio.nav.logout')}
          </a>
        </>
      ) : (
        // ui Button 不支持 as/href（ButtonHTMLAttributes），用 <a> 内联复刻 ghost/primary sm 观感
        <>
          <a
            href={loginUrl()}
            className="inline-flex items-center whitespace-nowrap rounded-full bg-transparent px-3 py-1.5 text-xs font-medium text-snb-t2 transition-colors hover:bg-snb-t1/5 hover:text-snb-t1 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary-500/50"
          >
            {t('studio.nav.login')}
          </a>
          <a
            href={consoleHref('/register')}
            className="inline-flex items-center whitespace-nowrap rounded-full bg-paper px-3.5 py-1.5 text-xs font-semibold text-asphalt no-underline transition-colors hover:bg-[#E2DDD3]"
          >
            {t('studio.nav.signup')}
          </a>
        </>
      )}
    </AppHeader>
  )
}
