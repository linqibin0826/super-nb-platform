import './styles.css'

export { cx } from './lib/cx'
// 明暗主题契约（跨子域名唯一真源，改必四边同步——见 src/theme/snbTheme.ts 文件头）
export {
  THEME_COOKIE,
  THEME_COOKIE_DOMAIN,
  THEME_COOKIE_MAX_AGE,
  THEME_BOOT_SNIPPET,
  LEGACY_THEME_KEYS,
  themeCookieDomainAttr,
  readThemeCookie,
  systemTheme,
  resolveTheme,
  applyTheme,
  setThemePref,
  setTheme,
  toggleTheme,
  purgeLegacyThemeState,
  initTheme,
  type ThemeChoice,
  type ThemePref,
  type InitThemeOptions,
} from './theme/snbTheme'
export { useTheme, type UseThemeResult } from './theme/useTheme'
export { ThemeToggle, type ThemeToggleProps } from './components/ThemeToggle/ThemeToggle'
// 链接版按钮配方（<a> 用）：值与 Button 的 primary/secondary/ghost 同源
export { ctaAnchorClass, secondaryAnchorClass, ghostAnchorClass } from './lib/cta'
export { ThemeScope, type ThemeScopeProps } from './components/ThemeScope/ThemeScope'
export { Button, type ButtonProps, type ButtonVariant, type ButtonSize } from './components/Button/Button'
export { Input, type InputProps } from './components/Input/Input'
export { Textarea, type TextareaProps } from './components/Textarea/Textarea'
export { TicketSelect, type TicketSelectProps, type TicketSelectOption } from './components/TicketSelect/TicketSelect'
export { Chip, type ChipProps } from './components/Chip/Chip'
export { Badge, type BadgeProps, type BadgeTone } from './components/Badge/Badge'
export { Card, CardHeader, CardBody, CardFooter, type CardProps } from './components/Card/Card'
export { GlassCard, type GlassCardProps } from './components/GlassCard/GlassCard'
export { StatCard, type StatCardProps, type StatTone } from './components/StatCard/StatCard'
export { Modal, type ModalProps } from './components/Modal/Modal'
export { Table, type TableProps, type TableColumn } from './components/Table/Table'
export { Alert, type AlertProps, type AlertTone } from './components/Alert/Alert'
export { Tabs, type TabsProps, type TabItem } from './components/Tabs/Tabs'
export { NavCapsule, type NavCapsuleProps, type NavCapsuleItem } from './components/NavCapsule/NavCapsule'
export { Skeleton, type SkeletonProps } from './components/Skeleton/Skeleton'
export { QuoteLine, type QuoteLineProps } from './components/QuoteLine/QuoteLine'
export { BrandLogo, type BrandLogoProps } from './components/BrandLogo/BrandLogo'
export { AppHeader, type AppHeaderProps } from './components/AppHeader/AppHeader'
export { HeaderAccount, type HeaderAccountProps } from './components/AppHeader/HeaderAccount'
export { SITE_NAV_ITEMS, siteNavItems, type SiteKey, type SiteNavItem } from './components/AppHeader/nav'
// GlobalParts v3 三件新公用件：状态灯唯一版 / 访客态 / 空态错误态配对件
export { StatusLamp, type StatusLampProps, type StatusLampState } from './components/StatusLamp/StatusLamp'
export { GuestGate, type GuestGateProps, type GuestGatePreset } from './components/GuestGate/GuestGate'
export { EmptyState, type EmptyStateProps } from './components/Feedback/EmptyState'
export { ErrorState, type ErrorStateProps } from './components/Feedback/ErrorState'
export { MasonryGrid, MasonryCard, type MasonryGridProps, type MasonryCardProps } from './components/MasonryCard/MasonryCard'
export { Lightbox, type LightboxProps } from './components/Lightbox/Lightbox'
export { AmbientBackground, type AmbientBackgroundProps, type AmbientVariant } from './components/AmbientBackground/AmbientBackground'
