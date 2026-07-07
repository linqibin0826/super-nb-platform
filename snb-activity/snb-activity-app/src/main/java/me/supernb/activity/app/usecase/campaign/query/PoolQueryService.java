package me.supernb.activity.app.usecase.campaign.query;

import java.util.List;
import me.supernb.activity.domain.model.read.PoolTier;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.read.PoolReadPort;
import org.springframework.stereotype.Service;

/// 奖池实况,按档位统计余量份数。无进行中活动 → 空列表(前端优雅降级)。
@Service
public class PoolQueryService {

    private final CampaignPort campaignPort;
    private final PoolReadPort poolPort;

    /// 构造:注入活动与奖池读端口。
    public PoolQueryService(CampaignPort campaignPort, PoolReadPort poolPort) {
        this.campaignPort = campaignPort;
        this.poolPort = poolPort;
    }

    /// 取当前活动 id,委托 PoolReadPort 按档位取奖槽余量;无进行中活动 → 空列表。
    public List<PoolTier> pool() {
        return campaignPort.activeCampaign()
                .map(c -> poolPort.pool(c.id()))
                .orElseGet(List::of);
    }
}
