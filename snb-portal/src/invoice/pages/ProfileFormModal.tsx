import { useState } from 'react'
import { Button, Input, Modal } from '../../ui'
import { t } from '../../i18n'
import { api, type ProfileT } from '../api'
import { ErrorBar } from './shared'

export type ProfileDraft = Omit<ProfileT, 'id'>

export const EMPTY_DRAFT: ProfileDraft = {
  type: 'COMPANY',
  title: '',
  taxNo: '',
  regAddress: '',
  regPhone: '',
  bankName: '',
  bankAccount: '',
}

/** 抬头表单弹窗(新增/编辑共用):ProfilesPage 管理 + ApplyPage 就地新增复用。
 *  保存成功回调 onSaved(id),父组件决定关闭/选中/刷新。 */
export function ProfileFormModal({
  id,
  initial,
  onClose,
  onSaved,
}: {
  id: string | null
  initial: ProfileDraft
  onClose: () => void
  onSaved: (id: string) => void
}) {
  const [draft, setDraft] = useState(initial)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  const set = (patch: Partial<ProfileDraft>) => setDraft((d) => ({ ...d, ...patch }))

  const incomplete =
    !draft.title.trim() || (draft.type === 'COMPANY' && !(draft.taxNo ?? '').trim())

  const save = async () => {
    setSaving(true)
    setError('')
    try {
      if (id) {
        await api.updateProfile(id, draft)
        onSaved(id)
      } else {
        const r = await api.createProfile(draft)
        onSaved(r.id)
      }
    } catch (e) {
      setError(String((e as Error).message))
      setSaving(false)
    }
  }

  const fields: { key: keyof ProfileDraft; label: string; hint?: string; companyOnly?: boolean }[] = [
    { key: 'taxNo', label: t('invoice.profiles.taxNo'), hint: t('invoice.profiles.taxHint'), companyOnly: true },
    { key: 'regAddress', label: t('invoice.profiles.regAddress'), companyOnly: true },
    { key: 'regPhone', label: t('invoice.profiles.regPhone'), companyOnly: true },
    { key: 'bankName', label: t('invoice.profiles.bankName'), companyOnly: true },
    { key: 'bankAccount', label: t('invoice.profiles.bankAccount'), companyOnly: true },
  ]

  return (
    <Modal open onClose={onClose} title={id ? t('invoice.profiles.editTitle') : t('invoice.profiles.add')}>
      <div className="space-y-3">
        {error && <ErrorBar msg={error} />}
        <div className="flex gap-2">
          {(['COMPANY', 'PERSONAL'] as const).map((type) => (
            <Button
              key={type}
              size="sm"
              variant={draft.type === type ? 'primary' : 'secondary'}
              onClick={() => set({ type })}
            >
              {type === 'COMPANY' ? t('invoice.profiles.typeCompany') : t('invoice.profiles.typePersonal')}
            </Button>
          ))}
        </div>
        <label className="block">
          <span className="mb-1 block text-xs text-snb-t3">
            {t('invoice.profiles.title')}
            <b className="ml-0.5" style={{ color: 'var(--iv-no-red)' }}>*</b>
          </span>
          <Input
            value={draft.title}
            onChange={(e) => set({ title: e.target.value })}
            placeholder={t('invoice.profiles.titleHint')}
          />
        </label>
        {fields
          .filter((f) => !f.companyOnly || draft.type === 'COMPANY')
          .map((f) => (
            <label key={f.key} className="block">
              <span className="mb-1 block text-xs text-snb-t3">
                {f.label}
                {f.key === 'taxNo' && <b className="ml-0.5" style={{ color: 'var(--iv-no-red)' }}>*</b>}
              </span>
              <Input
                value={draft[f.key] ?? ''}
                onChange={(e) => set({ [f.key]: e.target.value } as Partial<ProfileDraft>)}
                placeholder={f.hint ?? ''}
              />
            </label>
          ))}
        <div className="flex justify-end gap-2 pt-1">
          <Button variant="secondary" onClick={onClose}>{t('invoice.profiles.cancel')}</Button>
          <Button variant="primary" disabled={saving || incomplete} onClick={save}>
            {t('invoice.profiles.save')}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
