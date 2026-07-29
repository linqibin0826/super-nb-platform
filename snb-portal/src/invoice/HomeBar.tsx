import { useEffect, useState } from 'react'
import { CONSOLE_ORIGIN } from '../config'
import { loginUrl } from '../auth/apiFetch'
import { isLoggedIn, subscribeAuth } from '../auth/tokens'
import { isStandalone } from './container'
import { ThemeToggle } from '../ui'
import { ti } from './copy'

/** 回家条:只在**独立标签页**形态渲染(嵌入控制台 iframe 时整条不存在,否则与宿主顶栏双头)。
 *
 *  它不是顶栏——没有键帽行、没有余额、没有头像,那些是宿主的活。只有三件东西:
 *  字标(不可点)、「← 回我的机位」、访客时的「登录」。高 40、在文档流里、不 fixed、不遮票面。
 *
 *  🚨 两条出站链接一律 target="_top":嵌入形态下要跳出 iframe,别在框里再套一个控制台。
 *  (虽然本组件只在独立形态渲染,_top 仍显式写死——将来若改成两形态都出,行为不会漂。) */
export function HomeBar() {
  const [guest, setGuest] = useState(() => !isLoggedIn())

  // 主站登录/登出/轮换靠 subscribeAuth 的 storage + 聚焦对账接上,回家条的「登录」跟着进退
  useEffect(() => subscribeAuth(() => setGuest(!isLoggedIn())), [])

  if (!isStandalone()) return null

  return (
    <div className="iv-homebar">
      <div className="iv-homebar-brand">
        <span className="font-sign text-[14px] font-bold tracking-[0.07em] text-snb-t1">
          SUPER<span className="text-snb-safety">·</span>NB
        </span>
        {/* 🚨 回家条底是深井 well：t3 压它只有 4.23:1，按 tokens.css 的降级纪律走 t2 */}
        <span className="font-mono text-[11.5px] tracking-[0.04em] text-snb-t2">
          {ti('invoice.homebar.note')}
        </span>
      </div>
      <div className="flex flex-none items-center">
        {/* 开灯/关灯：只在独立形态出（嵌入形态由宿主控制台的开关管，同一枚父域 cookie，
            iframe 自己的 boot 片段会跟上）。
            去掉描边、保留 44 热区——与相邻的 .iv-homebar-link 同款（条高 40、热区 44 溢出，
            是本条既定做法，不是这里破例）。 */}
        <ThemeToggle className="border-0" />
        <a className="iv-homebar-link" href={CONSOLE_ORIGIN} target="_top">
          {ti('invoice.homebar.back')}
        </a>
        {guest && (
          <a className="iv-homebar-link on" href={loginUrl()} target="_top">
            {ti('invoice.homebar.login')}
          </a>
        )}
      </div>
    </div>
  )
}
