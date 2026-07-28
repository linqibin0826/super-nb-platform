import { useEffect, useRef, useState } from 'react'
import type { AuthUser } from './tokens'

export interface UserMenuProps {
  user: AuthUser
  /** 我的机位链接（studio 本地 dev 走同源相对路径，生产直链控制台域） */
  dashboardHref: string
  /** 退出链接：一律直链 fork /logout 登出单点（墓碑协议唯一真源，子站绝不自己碰 cookie） */
  logoutHref: string
  dashboardLabel: string
  logoutLabel: string
  ariaLabel: string
}

/** 已登录用户区（用户区契约 2026-07-28）：头像一枚，点开账户下拉——
 *  用户头（email）/ 我的机位 / 退出（破坏性操作走功能红）。
 *  浮卡与 AppHeader 菜单钮浮卡同材质（panel + hairline + 纯黑投影）。
 *  studio / hub 共用；Esc 与点外任一关闭。 */
export function UserMenu({ user, dashboardHref, logoutHref, dashboardLabel, logoutLabel, ariaLabel }: UserMenuProps) {
  const [open, setOpen] = useState(false)
  const wrapRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open) return
    const onPointerDown = (e: PointerEvent) => {
      if (!wrapRef.current?.contains(e.target as Node)) setOpen(false)
    }
    const onKeydown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeydown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeydown)
    }
  }, [open])

  return (
    <div ref={wrapRef} className="relative">
      <button
        type="button"
        aria-label={ariaLabel}
        aria-expanded={open}
        onClick={() => setOpen((v) => !v)}
        className="flex h-[30px] w-[30px] flex-none cursor-pointer items-center justify-center rounded-full border border-snb-hairline-strong bg-snb-elv p-0 text-xs font-semibold text-snb-t2 transition-colors hover:text-snb-t1 aria-expanded:text-snb-t1"
      >
        {user.email.charAt(0).toUpperCase()}
      </button>
      {open && (
        <nav
          aria-label={ariaLabel}
          className="absolute right-0 top-[calc(100%+8px)] z-50 flex min-w-[210px] flex-col gap-0.5 rounded-[10px] border border-snb-hairline-strong bg-snb-panel p-2 shadow-[0_18px_50px_rgb(0_0_0/0.5)]"
        >
          <div className="mb-1 border-b border-snb-hairline-strong px-3.5 pb-2.5 pt-2">
            <div className="max-w-[220px] truncate text-[12px] text-snb-t2">{user.email}</div>
          </div>
          <a
            href={dashboardHref}
            className="flex items-center gap-2.5 rounded-md px-3.5 py-2.5 text-sm font-medium text-snb-t2 no-underline transition-colors hover:bg-snb-t1/[0.06] hover:text-snb-t1"
          >
            <svg className="h-[15px] w-[15px] flex-none opacity-85" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>
            {dashboardLabel}
          </a>
          <a
            href={logoutHref}
            className="flex items-center gap-2.5 rounded-md px-3.5 py-2.5 text-sm font-medium text-snb-danger no-underline transition-colors hover:bg-snb-danger/[0.08]"
          >
            <svg className="h-[15px] w-[15px] flex-none opacity-85" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5" /><path d="M21 12H9" /></svg>
            {logoutLabel}
          </a>
        </nav>
      )}
    </div>
  )
}
