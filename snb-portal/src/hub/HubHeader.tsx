import { AppHeader, ThemeSwitch } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { t } from '../i18n'
import type { Theme } from '../themeCookie'

/** hub 顶栏：统一 AppHeader（规范 v1）+ 场景槽（主题切换 → 登录态）。照 studio TopBar 裁剪。 */
export function HubHeader({ theme, onToggleTheme }: { theme: Theme; onToggleTheme: () => void }) {
  const user = useAuthUser()
  return (
    <AppHeader site="hub" subtitle={t('hub.title')}>
      <ThemeSwitch
        dark={theme === 'dark'}
        onToggle={onToggleTheme}
        aria-label={t('hub.nav.theme')}
        title={t('hub.nav.theme')}
      />
      {user ? (
        <span className="hidden text-sm text-snb-t2 sm:inline" title={user.email}>
          {user.email}
        </span>
      ) : (
        <a className="text-sm text-snb-t2 underline-offset-4 hover:text-snb-t1 hover:underline" href={loginUrl()}>
          {t('hub.nav.login')}
        </a>
      )}
    </AppHeader>
  )
}
