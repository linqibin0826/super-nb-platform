package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.springframework.stereotype.Service;

@Service
public class DeleteRafflePrizeHandler implements CommandHandler<DeleteRafflePrizeCommand, Void> {

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;

    public DeleteRafflePrizeHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
    }

    @Override
    public Void handle(DeleteRafflePrizeCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        prizePort.delete(command.prizeId());
        return null;
    }
}
