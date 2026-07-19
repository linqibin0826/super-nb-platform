package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.springframework.stereotype.Service;

@Service
public class UpdateRafflePrizeHandler implements CommandHandler<UpdateRafflePrizeCommand, Void> {

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;

    public UpdateRafflePrizeHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
    }

    @Override
    public Void handle(UpdateRafflePrizeCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        prizePort.update(command.prizeId(), command.tier(), command.displayName(), command.kind(),
                command.payload(), command.sortOrder());
        return null;
    }
}
