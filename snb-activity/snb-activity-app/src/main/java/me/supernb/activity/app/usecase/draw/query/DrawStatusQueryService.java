package me.supernb.activity.app.usecase.draw.query;

import java.math.BigDecimal;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawEligibility;
import me.supernb.activity.domain.model.read.DrawStatus;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import me.supernb.activity.domain.port.read.RechargeReadPort;
import org.springframework.stereotype.Service;

/// 查询当前用户在本活动的抽奖资格与剩余次数。无进行中活动 → CampaignNotActiveException(404)。
@Service
public class DrawStatusQueryService {

    private final CampaignPort campaignPort;
    private final RechargeReadPort rechargePort;
    private final DrawPort drawPort;

    /// 构造:注入活动/充值读/抽奖端口。
    public DrawStatusQueryService(CampaignPort campaignPort, RechargeReadPort rechargePort, DrawPort drawPort) {
        this.campaignPort = campaignPort;
        this.rechargePort = rechargePort;
        this.drawPort = drawPort;
    }

    /// 取活动窗口内充值总额与已抽次数:充值总额达 DrawEligibility 定义的门槛即算资格达标,
    /// 剩余次数委托 DrawEligibility 计算。
    public DrawStatus status(long userId) {
        Campaign c = campaignPort.activeCampaign().orElseThrow(CampaignNotActiveException::new);
        BigDecimal total = rechargePort.totalRecharge(userId, c.startsAt(), c.endsAt());
        int used = drawPort.countDraws(c.id(), userId);
        boolean eligible = total.compareTo(DrawEligibility.DRAW_THRESHOLD) >= 0;
        return new DrawStatus(eligible, DrawEligibility.remainingDraws(total, used));
    }
}
