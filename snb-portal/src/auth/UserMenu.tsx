import { useEffect, useRef, useState } from 'react'
import type { AuthUser } from './tokens'

export interface UserMenuProps {
  user: AuthUser
  /** 控制台链接（studio 本地 dev 走同源相对路径，生产直链控制台域） */
  dashboardHref: string
  /** 退出链接：一律直链 fork /logout 登出单点（墓碑协议唯一真源，子站绝不自己碰 cookie） */
  logoutHref: string
  dashboardLabel: string
  logoutLabel: string
  ariaLabel: string
}

/** 已登录用户区（用户区契约 2026-07-28）：34px 圆头像，点开账户下拉——
 *  用户头（email）/ 控制台 / 退出（破坏性操作走功能红）。
 *  浮卡与 AppHeader 菜单浮卡同材质（苹果式 v3：玻璃 + 发丝线 + 浮层双层投影）。
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
        className="grid h-[34px] w-[34px] flex-none cursor-pointer place-items-center rounded-full border-0 bg-snb-elv p-0 text-[13px] font-semibold text-snb-t2 transition-colors duration-quick ease-snb-quick hover:text-snb-t1 aria-expanded:text-snb-t1"
      >
        {user.email.charAt(0).toUpperCase()}
      </button>
      {open && (
        <nav
          aria-label={ariaLabel}
          /* 玻璃浮层（.snb-glass 来自 vendored styles.css）：sheet 圆角 + 浮层双层投影 + 400ms 物化。
             ⚠️ 浮卡挂在头像下方用 absolute，绝不用 fixed——顶栏带 backdrop-filter 时它会成为
                fixed 后代的包含块（三仓库各撞过一次）。
             ⚠️ 同一条 backdrop-filter 还让本浮卡落在顶栏这个 Backdrop Root 里，自己的毛玻璃
                滤镜**不生效**（批 1 已裁决）：所以底色不指望 blur，显式压到 .92 两档各一份，
                盖住 .snb-glass 的 .72（工具类在 utilities 层，稳压 components 层的 .snb-glass）。 */
          className="snb-glass absolute right-0 top-[calc(100%+8px)] z-50 flex min-w-[210px] animate-snb-materialize flex-col gap-0.5 rounded-xl border border-snb-hairline bg-[rgba(255,255,255,0.92)] p-2 shadow-glass motion-reduce:animate-none dark:bg-[rgba(29,29,31,0.92)]"
        >
          <div className="mb-1 border-b border-snb-hairline px-3.5 pb-2.5 pt-2">
            <div className="max-w-[220px] truncate text-[12px] text-snb-t2">{user.email}</div>
          </div>
          <a
            href={dashboardHref}
            className="flex items-center gap-2.5 rounded-lg px-3.5 py-2.5 text-sm font-medium text-snb-t2 no-underline transition-colors duration-quick ease-snb-quick hover:bg-snb-t1/[0.06] hover:text-snb-t1"
          >
            <svg className="h-[15px] w-[15px] flex-none opacity-85" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></svg>
            {dashboardLabel}
          </a>
          <a
            href={logoutHref}
            className="flex items-center gap-2.5 rounded-lg px-3.5 py-2.5 text-sm font-medium text-snb-danger no-underline transition-colors duration-quick ease-snb-quick hover:bg-snb-danger/[0.08]"
          >
            <svg className="h-[15px] w-[15px] flex-none opacity-85" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" /><path d="M16 17l5-5-5-5" /><path d="M21 12H9" /></svg>
            {logoutLabel}
          </a>
        </nav>
      )}
    </div>
  )
}
