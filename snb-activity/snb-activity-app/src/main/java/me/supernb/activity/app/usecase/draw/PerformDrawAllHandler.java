package me.supernb.activity.app.usecase.draw;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.List;
import me.supernb.activity.app.usecase.draw.command.PerformDrawAllCommand;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import org.springframework.stereotype.Service;

/// 执行一次批量抽奖:取进行中活动,委托 DrawPort.drawAllFor 完成实际发奖。
/// 无进行中活动 → CampaignNotActiveException(404);无剩余次数 → NoDrawsLeftException(409),
/// 由 DrawPort 判定并抛出。并发超额防护(advisory lock + 单事务)在 DrawPort 实现内。
@Service
public class PerformDrawAllHandler implements CommandHandler<PerformDrawAllCommand, List<DrawResult>> {

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;

    /// 构造:注入活动与抽奖端口。
    public PerformDrawAllHandler(CampaignPort campaignPort, DrawPort drawPort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
    }

    /// 取进行中活动,委托 DrawPort 执行批量抽奖。
    @Override
    public List<DrawResult> handle(PerformDrawAllCommand command) {
        Campaign c = campaignPort.activeCampaign().orElseThrow(CampaignNotActiveException::new);
        return drawPort.drawAllFor(c, command.userId());
    }
}
