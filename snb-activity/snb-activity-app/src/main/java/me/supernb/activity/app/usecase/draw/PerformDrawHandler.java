package me.supernb.activity.app.usecase.draw;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.app.usecase.draw.command.PerformDrawCommand;
import me.supernb.activity.domain.exception.CampaignNotActiveException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.port.campaign.CampaignPort;
import me.supernb.activity.domain.port.draw.DrawPort;
import org.springframework.stereotype.Service;

/// 执行一次抽奖:取进行中活动,委托 DrawPort 完成实际发奖。无进行中活动 → CampaignNotActiveException(404);
/// 无剩余抽奖次数 → NoDrawsLeftException(409),由 DrawPort 判定并抛出。
///
/// 并发超额防护(同一用户不超发)由 DrawPort 的实现保证(advisory lock + 事务);Handler 本身无事务注解——
/// 事务边界按约定收在 infra,不放在这层。
@Service
public class PerformDrawHandler implements CommandHandler<PerformDrawCommand, DrawResult> {

    private final CampaignPort campaignPort;
    private final DrawPort drawPort;

    /// 构造:注入活动与抽奖端口。
    public PerformDrawHandler(CampaignPort campaignPort, DrawPort drawPort) {
        this.campaignPort = campaignPort;
        this.drawPort = drawPort;
    }

    /// 取进行中活动,委托 DrawPort 执行抽奖。
    @Override
    public DrawResult handle(PerformDrawCommand command) {
        Campaign c = campaignPort.activeCampaign().orElseThrow(CampaignNotActiveException::new);
        return drawPort.drawFor(c, command.userId());
    }
}
