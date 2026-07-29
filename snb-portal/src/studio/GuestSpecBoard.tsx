// 访客版规格牌（设计定稿 Studio.dc.html「访客版规格牌」/ StudioMobile 05）：
// 正在评估「值不值得充钱」的人原本两手空空——这块把「能选什么模型 / 出什么尺寸 /
// 大概花多少」三个位置立出来。
// 🚨 事实纪律：模型清单绑用户 Key、预估绑用户分组，访客侧根本没有这两份数据 ⇒
//    一律骨架 + 口径说明，**一个模型名、一个价格数字都不许出现**。
//    可以写的真数就三样：七种比例档 + 1K/2K/4K 计费归档 / 灵感库 5778 条 / 队列同时 5 个。
// 骨架用静态 snb-elv 色块（深 #242A33 / 浅 #E5DFD3）：不闪不动，「数据永远不闪」。
import type { ReactNode } from 'react'
import { RATIO_OPTIONS } from '../lib/sizes'
import { RatioIcon } from './composer/RatioIcon'
import { ctaAnchorClass, StatusLamp } from './parts'
import { registerUrl } from './links'
import { st } from './i18nStudio'

/** 三格之一：mono 小标 + 内容 */
function Slot({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-3 rounded-[10px] border border-snb-hairline bg-snb-well px-[18px] py-4">
      {/* 🚨 深井上不许用 t3（4.23:1 不过线，见 tokens.css 的降级纪律），标签也走 t2 */}
      <span className="font-mono text-[11px] tracking-[0.14em] text-snb-t2">{title}</span>
      {children}
    </div>
  )
}

export function GuestSpecBoard() {
  return (
    <section className="mb-5 rounded-xl border border-snb-hairline bg-snb-panel p-[clamp(18px,2vw,24px)]">
      <div className="flex flex-wrap items-baseline justify-between gap-x-5 gap-y-2">
        <div className="flex items-center gap-2.5">
          {/* 空心灯 = 待开（实心橙只给「正在发生」） */}
          <StatusLamp state="pending" />
          <span className="font-mono text-[12.5px] tracking-[0.04em] text-snb-t3">
            {st('studio.guestBoard.badge')}
          </span>
        </div>
        <span className="text-[13px] text-snb-t2">{st('studio.guestBoard.lead')}</span>
      </div>

      <div className="mt-[18px] grid gap-[clamp(14px,1.6vw,22px)] sm:grid-cols-2 xl:grid-cols-3">
        <Slot title={st('studio.guestBoard.modelsTitle')}>
          <div aria-hidden="true" className="flex flex-col gap-2.5">
            {['74%', '58%', '66%', '44%'].map((w) => (
              <span key={w} className="h-3 rounded-md bg-snb-elv" style={{ width: w }} />
            ))}
          </div>
          <span className="text-[13px] leading-[1.65] text-snb-t2">{st('studio.guestBoard.modelsBody')}</span>
        </Slot>

        <Slot title={st('studio.guestBoard.sizesTitle')}>
          {/* 七种比例档是真数据（RATIO_OPTIONS 单一真源），形状一眼可辨、不用读数字 */}
          <div className="flex flex-wrap gap-1.5">
            {RATIO_OPTIONS.map((r) => (
              <span
                key={r.value}
                className="inline-flex h-7 items-center gap-1.5 rounded-full border border-snb-hairline-strong px-2.5 font-mono text-xs text-snb-t1"
              >
                <span className="text-snb-t3">
                  <RatioIcon w={r.w} h={r.h} />
                </span>
                {r.value}
              </span>
            ))}
            <span className="inline-flex h-7 items-center rounded-full border border-dashed border-snb-hairline-strong px-2.5 font-mono text-xs text-snb-t2">
              {st('studio.guestBoard.sizesAuto')}
            </span>
          </div>
          <span className="text-[13px] leading-[1.65] text-snb-t2">
            {st('studio.guestBoard.sizesBody1')}
            <span className="font-mono text-snb-t1">1K / 2K / 4K</span>
            {st('studio.guestBoard.sizesBody2')}
          </span>
        </Slot>

        <Slot title={st('studio.guestBoard.costTitle')}>
          <div className="flex items-center gap-2.5">
            <span className="font-mono text-[22px] font-bold text-snb-t3">$</span>
            <span aria-hidden="true" className="h-5 w-24 rounded-lg bg-snb-elv" />
          </div>
          <div className="font-mono text-xs leading-[1.9] text-snb-t2">
            {st('studio.guestBoard.costFormula1')}
            <br />
            {st('studio.guestBoard.costFormula2')}
          </div>
          <span className="text-[13px] leading-[1.65] text-snb-t2">{st('studio.guestBoard.costBody')}</span>
        </Slot>
      </div>

      <div className="mt-[18px] flex flex-wrap items-center gap-x-4 gap-y-3">
        {/* 手机档整宽（GuestGate 定稿：CTA 保持整宽 44 高），≥sm 回到行内 */}
        <a href={registerUrl()} className={`${ctaAnchorClass} w-full sm:w-auto`}>
          {st('studio.guestBoard.cta')}
        </a>
        <span className="text-[13px] text-snb-t2">{st('studio.guestBoard.ctaNote')}</span>
      </div>
    </section>
  )
}
