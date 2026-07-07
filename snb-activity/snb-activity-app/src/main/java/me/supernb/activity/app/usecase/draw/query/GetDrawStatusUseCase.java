package me.supernb.activity.app.usecase.draw.query;

import java.math.BigDecimal;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawEligibility;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.DrawPort;
import me.supernb.activity.domain.port.RechargeQueryPort;
import org.springframework.stereotype.Service;

/// 查询当前用户的抽奖资格与剩余次数。无进行中活动 → CampaignNotActiveException(404)。
@Service
public class GetDrawStatusUseCase {

    private final CampaignPort campaignPort;
    private final RechargeQueryPort rechargePort;
    private final DrawPort drawPort;

    public GetDrawStatusUseCase(CampaignPort campaignPort, RechargeQueryPort rechargePort, DrawPort drawPort) {
        this.campaignPort = campaignPort;
        this.rechargePort = rechargePort;
        this.drawPort = drawPort;
    }

    public DrawStatus status(long userId) {
        Campaign c = campaignPort.activeCampaign().orElseThrow(CampaignNotActiveException::new);
        BigDecimal total = rechargePort.totalRecharge(userId, c.startsAt(), c.endsAt());
        int used = drawPort.countDraws(c.id(), userId);
        boolean eligible = total.compareTo(DrawEligibility.DRAW_THRESHOLD) >= 0;
        return new DrawStatus(eligible, DrawEligibility.remainingDraws(total, used));
    }
}
