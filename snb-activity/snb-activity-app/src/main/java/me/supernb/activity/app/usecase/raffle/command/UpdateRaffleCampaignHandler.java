package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import org.springframework.stereotype.Service;

@Service
public class UpdateRaffleCampaignHandler implements CommandHandler<UpdateRaffleCampaignCommand, Void> {

    private final RaffleCampaignPort campaignPort;

    public UpdateRaffleCampaignHandler(RaffleCampaignPort campaignPort) {
        this.campaignPort = campaignPort;
    }

    @Override
    public Void handle(UpdateRaffleCampaignCommand command) {
        RaffleAdminValidation.requireEditableCampaign(campaignPort, command.campaignId());
        RaffleAdminValidation.validateScalars(command.name(), command.entryOpenAt(), command.entryCloseAt(),
                command.drawAt(), command.gateAmount(), command.minAccountAgeDays());
        campaignPort.update(command.campaignId(), command.name(), command.entryOpenAt(), command.entryCloseAt(),
                command.drawAt(), command.gateType(), command.gateAmount(), command.gateFrom(),
                command.minAccountAgeDays(), command.weightMode());
        return null;
    }
}
