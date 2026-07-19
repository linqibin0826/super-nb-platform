package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.springframework.stereotype.Service;

@Service
public class AddRafflePrizeHandler implements CommandHandler<AddRafflePrizeCommand, Long> {

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;

    public AddRafflePrizeHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
    }

    @Override
    public Long handle(AddRafflePrizeCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        return prizePort.create(command.campaignId(), command.tier(), command.displayName(),
                command.kind(), command.payload(), command.sortOrder());
    }
}
