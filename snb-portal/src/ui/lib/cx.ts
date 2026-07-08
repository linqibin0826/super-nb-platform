/** className 拼接：过滤 false/null/undefined（免 clsx 依赖） */
export function cx(...parts: Array<string | false | null | undefined>): string {
  return parts.filter(Boolean).join(' ')
}
