package me.supernb.activity.app;

import java.util.List;
import org.springframework.stereotype.Service;

/// 活动期充值榜 Top10。无进行中活动 → 空榜(前端优雅降级)。
@Service
public class GetLeaderboardUseCase {

    private static final int LIMIT = 10;

    private final CampaignPort campaignPort;
    private final RechargeQueryPort rechargePort;

    public GetLeaderboardUseCase(CampaignPort campaignPort, RechargeQueryPort rechargePort) {
        this.campaignPort = campaignPort;
        this.rechargePort = rechargePort;
    }

    public List<ActivityDto.LeaderEntry> leaderboard() {
        return campaignPort.activeCampaign()
                .map(c -> rechargePort.leaderboard(c.startsAt(), c.endsAt(), LIMIT))
                .orElseGet(List::of);
    }
}
