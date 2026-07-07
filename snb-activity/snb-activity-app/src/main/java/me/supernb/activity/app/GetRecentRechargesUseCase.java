package me.supernb.activity.app;

import java.util.List;
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

    public List<ActivityDto.RechargeEntry> recentRecharges() {
        return campaignPort.activeCampaign()
                .map(c -> rechargePort.recentRecharges(c.startsAt(), c.endsAt(), LIMIT))
                .orElseGet(List::of);
    }
}
