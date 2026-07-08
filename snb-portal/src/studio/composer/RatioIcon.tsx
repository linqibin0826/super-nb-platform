/** 比例形状小图标：按真实比例画一个空心圆角矩形，选中随文字变白 */
export function RatioIcon({ w, h }: { w: number; h: number }) {
  const iconW = w >= h ? 15 : Math.max(7, Math.round((15 * w) / h))
  const iconH = h >= w ? 15 : Math.max(7, Math.round((15 * h) / w))
  return (
    <span
      aria-hidden="true"
      className="relative z-[1] inline-block rounded-[3px] border-[1.5px] border-current"
      style={{ width: iconW, height: iconH }}
    />
  )
}
