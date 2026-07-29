/** 容器判定:同一份页面要在两种容器里都体面。
 *
 *  - 嵌入形态(主路径):控制台左侧菜单「发票中心」→ 控制台外壳里 iframe 套着本站。
 *    本站自己**不许有顶栏**——站头与主题开关由宿主提供,装了就双头(2026-07-17 站长拍板)。
 *  - 独立形态:充值页「需要发票?」入口卡 target=_blank、iframe 右上角「在新标签页打开」。
 *    这条路径落地是孤儿页,补一条 40px 回家条(不是整套顶栏,在文档流里、不遮内容)。
 *
 *  判定信号 = `window.self !== window.top`。跨源取 window.top 属性理论上可能抛,
 *  兜底一律当嵌入处理:宁可少一条回家条,也不能在宿主顶栏下面再出一条。 */
export function isStandalone(): boolean {
  try {
    return window.self === window.top
  } catch {
    return false
  }
}
