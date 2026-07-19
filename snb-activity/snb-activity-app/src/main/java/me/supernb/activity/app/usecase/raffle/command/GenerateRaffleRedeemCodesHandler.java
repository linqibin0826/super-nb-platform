package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.List;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RaffleRedeemCodeIssuerPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.springframework.stereotype.Service;

@Service
public class GenerateRaffleRedeemCodesHandler
        implements CommandHandler<GenerateRaffleRedeemCodesCommand, List<Long>> {

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;
    private final RaffleRedeemCodeIssuerPort redeemCodeIssuer;

    public GenerateRaffleRedeemCodesHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort,
            RaffleRedeemCodeIssuerPort redeemCodeIssuer) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
        this.redeemCodeIssuer = redeemCodeIssuer;
    }

    @Override
    public List<Long> handle(GenerateRaffleRedeemCodesCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        if (command.count() < 1 || command.count() > 100) {
            throw new RaffleAdminValidationException("单次生成数量必须在 1~100 之间");
        }
        // 先调外部服务再落库:sub2api 失败时本地零落库;拿到码后一次 RafflePrizePort#createBatch
        // 事务,要么全部成功要么全部回滚(设计要求:不产生部分落库)。
        List<String> codes = redeemCodeIssuer.issue(command.groupId(), command.validityDays(), command.count());
        return prizePort.createBatch(command.campaignId(), command.tier(), command.displayName(),
                "REDEEM_CODE", codes, command.sortOrderStart());
    }
}
