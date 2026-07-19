package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import org.springframework.stereotype.Service;

@Service
public class CancelRaffleCampaignHandler implements CommandHandler<CancelRaffleCampaignCommand, Void> {

    private final RaffleCampaignPort campaignPort;

    public CancelRaffleCampaignHandler(RaffleCampaignPort campaignPort) {
        this.campaignPort = campaignPort;
    }

    @Override
    public Void handle(CancelRaffleCampaignCommand command) {
        campaignPort.byId(command.campaignId()).orElseThrow(RaffleNotFoundException::new);
        campaignPort.cancel(command.campaignId());
        return null;
    }
}
