package me.supernb.activity.app.usecase.campaign.query;

import java.util.List;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.PoolPort;
import org.springframework.stereotype.Service;

/// 奖池实况(按档位份数)。无进行中活动 → 空(前端优雅降级)。
@Service
public class GetPoolUseCase {

    private final CampaignPort campaignPort;
    private final PoolPort poolPort;

    public GetPoolUseCase(CampaignPort campaignPort, PoolPort poolPort) {
        this.campaignPort = campaignPort;
        this.poolPort = poolPort;
    }

    public List<PoolTier> pool() {
        return campaignPort.activeCampaign()
                .map(c -> poolPort.pool(c.id()))
                .orElseGet(List::of);
    }
}
