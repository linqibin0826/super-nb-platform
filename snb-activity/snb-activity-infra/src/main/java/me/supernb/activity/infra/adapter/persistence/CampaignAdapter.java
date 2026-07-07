package me.supernb.activity.infra.adapter.persistence;

import java.util.Optional;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.infra.adapter.persistence.dao.CampaignJpaRepository;
import org.springframework.stereotype.Repository;

/// CampaignPort 实现:查活动库(snb 库 activity schema)取当前进行中活动。
@Repository
public class CampaignAdapter implements CampaignPort {

    private final CampaignJpaRepository campaigns;

    /// 构造:注入活动 DAO。
    public CampaignAdapter(CampaignJpaRepository campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public Optional<Campaign> activeCampaign() {
        return campaigns.findFirstByStatusOrderByIdDesc("active")
                .map(e -> new Campaign(e.getId(), e.getName(), e.getStartsAt(), e.getEndsAt(),
                        e.getStatus(), e.getConsolationAmount()));
    }
}
