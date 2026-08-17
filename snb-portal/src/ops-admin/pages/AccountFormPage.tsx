import { PageHead } from './shared'

/** 占位版:Task 10 覆写成完整表单(账号全生命周期 + 订阅编辑 + 密码显示)。 */
export function AccountFormPage({ mode }: { mode: 'create' | 'edit' }) {
  return <PageHead title={mode === 'create' ? '新建账号(建设中)' : '编辑账号(建设中)'} />
}
