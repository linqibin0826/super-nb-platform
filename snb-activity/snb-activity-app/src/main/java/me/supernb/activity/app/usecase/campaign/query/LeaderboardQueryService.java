package me.supernb.activity.app.usecase.campaign.query;

import java.util.List;
import me.supernb.activity.domain.model.read.LeaderEntry;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.springframework.stereotype.Service;

/// 活动期充值榜 Top10。无进行中活动 → 空榜(前端优雅降级)。
@Service
public class LeaderboardQueryService {

    private static final int LIMIT = 10;

    private final CampaignPort campaignPort;
    private final RechargeReadPort rechargePort;

    /// 构造:注入活动与充值读端口。
    public LeaderboardQueryService(CampaignPort campaignPort, RechargeReadPort rechargePort) {
        this.campaignPort = campaignPort;
        this.rechargePort = rechargePort;
    }

    public List<LeaderEntry> leaderboard() {
        return campaignPort.activeCampaign()
                .map(c -> rechargePort.leaderboard(c.startsAt(), c.endsAt(), LIMIT))
                .orElseGet(List::of);
    }
}
