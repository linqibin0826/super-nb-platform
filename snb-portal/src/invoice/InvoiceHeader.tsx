import { AppHeader, ThemeSwitch } from '../ui'
import { useAuthUser } from '../auth/useAuth'
import { loginUrl } from '../auth/apiFetch'
import { t } from '../i18n'
import type { Theme } from '../themeCookie'

/** 发票中心顶栏:统一 AppHeader(规范 v2;本站不进一级导航,不传 site)+ 主题开关 + 登录态。 */
export function InvoiceHeader({ theme, onToggleTheme }: { theme: Theme; onToggleTheme: () => void }) {
  const user = useAuthUser()
  return (
    <AppHeader subtitle={t('invoice.title')} labelFor={(item) => t(`invoice.nav.${item.key}`)}>
      <ThemeSwitch
        dark={theme === 'dark'}
        onToggle={onToggleTheme}
        aria-label={t('invoice.nav.theme')}
        title={t('invoice.nav.theme')}
      />
      {user ? (
        <a
          className="hidden text-sm text-snb-t2 underline-offset-4 hover:text-snb-t1 hover:underline sm:inline"
          href="https://super-nb.me/dashboard"
          title={user.email}
        >
          {user.email}
        </a>
      ) : (
        <a className="text-sm text-snb-t2 hover:text-snb-t1" href={loginUrl()}>
          {t('invoice.nav.login')}
        </a>
      )}
    </AppHeader>
  )
}
