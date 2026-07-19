import { t } from '../../i18n'
import { PageHead } from './shared'

export function CampaignListPage() {
  return <PageHead eyebrow={t('raffle.admin.eyebrow')} title={t('raffle.admin.title')} sub={t('raffle.admin.sub')} />
}
