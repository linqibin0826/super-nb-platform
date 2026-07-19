package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.List;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.exception.RafflePrizeNotFoundException;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.activity.domain.port.raffle.RaffleRedeemCodeIssuerPort;
import org.springframework.stereotype.Service;

@Service
public class GenerateRaffleRedeemCodeForPrizeHandler
        implements CommandHandler<GenerateRaffleRedeemCodeForPrizeCommand, Long> {

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;
    private final RaffleRedeemCodeIssuerPort redeemCodeIssuer;

    public GenerateRaffleRedeemCodeForPrizeHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort,
            RaffleRedeemCodeIssuerPort redeemCodeIssuer) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
        this.redeemCodeIssuer = redeemCodeIssuer;
    }

    @Override
    public Long handle(GenerateRaffleRedeemCodeForPrizeCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        if (command.groupId() <= 0) {
            throw new RaffleAdminValidationException("分组 ID 必须大于 0");
        }
        if (command.validityDays() < 1) {
            throw new RaffleAdminValidationException("有效天数至少 1 天");
        }
        RafflePrize prize = prizePort.byCampaign(command.campaignId()).stream()
                .filter(p -> p.id() == command.prizeId())
                .findFirst()
                .orElseThrow(RafflePrizeNotFoundException::new);
        if (!"REDEEM_CODE".equals(prize.kind())) {
            throw new RaffleAdminValidationException("该奖品不是兑换码类型");
        }
        // 已有码值拒绝覆盖:真码被顶掉会变孤儿码(sub2api 侧码活着但页面失联)
        if (prize.payload() != null && !prize.payload().isBlank()) {
            throw new RaffleAdminValidationException("该奖品已有码值,不能覆盖");
        }
        // 先调外部签发再回填:sub2api 失败时本地零写入
        List<String> codes = redeemCodeIssuer.issue(command.groupId(), command.validityDays(), 1);
        prizePort.updatePayload(command.prizeId(), codes.get(0));
        return command.prizeId();
    }
}
