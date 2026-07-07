package me.supernb.activity.infra.adapter.persistence;

import java.util.Optional;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.infra.adapter.persistence.dao.CampaignJpaRepository;
import org.springframework.stereotype.Repository;

/// CampaignPort 实现:委托 `CampaignJpaRepository` 查 `activity.campaign`(`snb` 库 `activity`
/// schema),取当前进行中的活动。
@Repository
public class CampaignAdapter implements CampaignPort {

    private final CampaignJpaRepository campaigns;

    /// 构造:注入活动仓储。
    public CampaignAdapter(CampaignJpaRepository campaigns) {
        this.campaigns = campaigns;
    }

    /// 取 status='active' 的最新一期活动(id 降序取首条)并映射为 [Campaign];无匹配返回 empty,不抛异常。
    @Override
    public Optional<Campaign> activeCampaign() {
        return campaigns.findFirstByStatusOrderByIdDesc("active")
                .map(e -> new Campaign(e.getId(), e.getName(), e.getStartsAt(), e.getEndsAt(),
                        e.getStatus(), e.getConsolationAmount()));
    }
}
