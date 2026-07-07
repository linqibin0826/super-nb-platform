package me.supernb.activity.app.usecase.draw;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.port.CampaignPort;
import me.supernb.activity.domain.port.DrawPort;
import org.springframework.stereotype.Service;

/// 执行一次抽奖。无进行中活动 → CampaignNotActiveException(404);无剩余次数 → NoDrawsLeftException(409)。
/// 并发超额防护由 DrawPort 实现(advisory lock + 事务)保证——Handler 无事务注解(约定:事务在 infra)。
@Service
public class PerformDrawHandler implements CommandHandler<PerformDrawCommand, DrawResult> {

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;

    public PerformDrawHandler(CampaignPort campaignPort, DrawPort drawPort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
    }

    @Override
    public DrawResult handle(PerformDrawCommand command) {
        Campaign c = campaignPort.activeCampaign().orElseThrow(CampaignNotActiveException::new);
        return drawPort.drawFor(c, command.userId());
    }
}
