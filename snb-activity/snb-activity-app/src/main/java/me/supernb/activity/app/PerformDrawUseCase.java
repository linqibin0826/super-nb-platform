package me.supernb.activity.app;

import me.supernb.activity.domain.Campaign;
import me.supernb.activity.domain.CampaignNotActiveException;
import me.supernb.activity.domain.DrawResult;
import org.springframework.stereotype.Service;

/// 执行一次抽奖。无进行中活动 → CampaignNotActiveException(404);无剩余次数 → NoDrawsLeftException(409)。
/// 并发超额防护由 DrawPort 实现(advisory lock + 事务)保证。
@Service
public class PerformDrawUseCase {

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;

    public PerformDrawUseCase(CampaignPort campaignPort, DrawPort drawPort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
    }

    public DrawResult draw(long userId) {
        Campaign c = campaignPort.activeCampaign().orElseThrow(CampaignNotActiveException::new);
        return drawPort.drawFor(c, userId);
    }
}
