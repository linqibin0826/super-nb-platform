package me.supernb.activity.app.usecase.campaign.query;

import java.util.List;
import me.supernb.activity.domain.model.read.RechargeEntry;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.RechargeQueryPort;
import org.springframework.stereotype.Service;

/// 活动期最近充值动态 Top20。无进行中活动 → 空(前端优雅降级)。
@Service
public class GetRecentRechargesUseCase {

    private static final int LIMIT = 20;

    private final CampaignPort campaignPort;
    private final RechargeQueryPort rechargePort;

    public GetRecentRechargesUseCase(CampaignPort campaignPort, RechargeQueryPort rechargePort) {
        this.campaignPort = campaignPort;
        this.rechargePort = rechargePort;
    }

    public List<RechargeEntry> recentRecharges() {
        return campaignPort.activeCampaign()
                .map(c -> rechargePort.recentRecharges(c.startsAt(), c.endsAt(), LIMIT))
                .orElseGet(List::of);
    }
}
