import type { ReactElement } from 'react'
import { loginUrl } from '../auth/apiFetch'
import { t } from '../i18n'
import { ti } from './copy'

export type GuestTab = 'apply' | 'requests' | 'profiles'

/** 骨架:静态色块,不闪不动(数据永远不闪,骨架也不闪)。三个页签只换这一块。 */
const BAR = 'block rounded-[6px] bg-snb-elv'

function SkeletonApply() {
  return (
    <div className="flex flex-col gap-2.5">
      <span className={`${BAR} h-2.5 w-[34%]`} />
      <div className="border-t border-snb-hairline" />
      {['80%', '66%', '74%'].map((w) => (
        <div key={w} className="grid grid-cols-[16px_1fr_76px] gap-2.5">
          <span className={`${BAR} h-4 rounded-[4px]`} />
          <span className={`${BAR} h-3 self-center`} style={{ width: w }} />
          <span className={`${BAR} h-3 self-center`} />
        </div>
      ))}
      <div className="mt-0.5 border-t border-dashed border-[rgba(239,235,228,0.28)]" />
      <div className="flex items-center justify-between gap-3">
        <span className={`${BAR} h-2.5 w-[22%]`} />
        <span className={`${BAR} h-5 w-[32%]`} />
      </div>
    </div>
  )
}

function SkeletonRequests() {
  return (
    <div className="flex flex-col gap-3">
      {[
        ['58%', '34%'],
        ['66%', '28%'],
        ['50%', '40%'],
      ].map(([a, b]) => (
        <div key={a} className="flex items-center gap-3">
          <span className="h-11 w-11 flex-none rounded-full border border-dashed border-[rgba(239,235,228,0.28)]" />
          <span className="flex flex-1 flex-col gap-2">
            <span className={`${BAR} h-3`} style={{ width: a }} />
            <span className={`${BAR} h-2.5`} style={{ width: b }} />
          </span>
        </div>
      ))}
    </div>
  )
}

function SkeletonProfiles() {
  return (
    <div className="flex flex-col gap-3">
      <span className={`${BAR} h-[52px] rounded-lg`} />
      {[
        ['46%', '62%'],
        ['38%', '54%'],
      ].map(([a, b]) => (
        <div key={a} className="flex flex-col gap-2">
          <span className={`${BAR} h-3`} style={{ width: a }} />
          <span className={`${BAR} h-2.5`} style={{ width: b }} />
        </div>
      ))}
    </div>
  )
}

const SKELETON: Record<GuestTab, () => ReactElement> = {
  apply: SkeletonApply,
  requests: SkeletonRequests,
  profiles: SkeletonProfiles,
}

/** 访客态公用件(公用件规范 v3):空心灯 +「访客视图」→ 静态骨架 → 整宽纸白 CTA → 一句登录后会变成什么。
 *
 *  🚨「没登录」不是「出错」:红色留给真失败(请求超时/解析失败),未登录一律走这里。
 *  退役掉的两件——红色报错「出错了:未登录或登录已过期」、28×17 的裸下划线「登录」——别再出现。
 *  CTA 直链主站登录页并 target="_top":嵌入 iframe 时跳出框,不在框里再套一个控制台。 */
export function GuestGate({ tab }: { tab: GuestTab }) {
  const Skeleton = SKELETON[tab]
  const tabName = t(`invoice.tabs.${tab}`)
  return (
    <div className="flex flex-col gap-4 rounded-xl border border-snb-hairline bg-snb-panel p-5">
      <div className="flex items-center gap-2">
        <span className="h-2 w-2 flex-none rounded-full border-[1.5px] border-snb-t3" />
        <span className="font-mono text-[12.5px] tracking-[0.04em] text-snb-t3">
          {ti('invoice.guest.badge', { tab: tabName })}
        </span>
      </div>
      <Skeleton />
      <a
        className="flex h-12 items-center justify-center rounded-lg bg-paper text-sm font-semibold text-asphalt shadow-[0_2px_0_rgba(0,0,0,0.45)] transition-colors hover:bg-[#FFFFFF] active:bg-[#DED8CE] sm:h-11"
        href={loginUrl()}
        target="_top"
      >
        {ti('invoice.guest.cta')}
      </a>
      <p className="text-[13px] leading-[1.65] text-snb-t2">{ti(`invoice.guest.${tab}`)}</p>
    </div>
  )
}
