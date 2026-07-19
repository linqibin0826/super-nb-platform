import { t } from '../../i18n'
import { PageHead } from './shared'

export function CampaignFormPage({ mode }: { mode: 'create' | 'edit' }) {
  return (
    <PageHead
      eyebrow={t('raffle.admin.eyebrow')}
      title={mode === 'create' ? t('raffle.admin.newCampaign') : t('raffle.admin.title')}
    />
  )
}
