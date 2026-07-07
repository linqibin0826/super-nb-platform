package me.supernb.activity.infra;

import java.util.Optional;
import me.supernb.activity.app.CampaignPort;
import me.supernb.activity.domain.Campaign;
import me.supernb.activity.infra.jpa.CampaignJpaRepository;
import org.springframework.stereotype.Repository;

/// CampaignPort 实现:查活动库(snb 库 activity schema)取当前进行中活动。
@Repository
public class CampaignAdapter implements CampaignPort {

    private final CampaignJpaRepository campaigns;

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
