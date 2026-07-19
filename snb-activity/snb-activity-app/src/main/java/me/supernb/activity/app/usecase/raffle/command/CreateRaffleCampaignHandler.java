package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import org.springframework.stereotype.Service;

@Service
public class CreateRaffleCampaignHandler implements CommandHandler<CreateRaffleCampaignCommand, Long> {

    private final RaffleCampaignPort campaignPort;
    private final RafflePrizePort prizePort;

    public CreateRaffleCampaignHandler(RaffleCampaignPort campaignPort, RafflePrizePort prizePort) {
        this.campaignPort = campaignPort;
        this.prizePort = prizePort;
    }

    @Override
    public Long handle(CreateRaffleCampaignCommand command) {
        RaffleAdminValidation.validateScalars(command.name(), command.entryOpenAt(), command.entryCloseAt(),
                command.drawAt(), command.gateAmount(), command.minAccountAgeDays());
        long id = campaignPort.create(command.name(), command.entryOpenAt(), command.entryCloseAt(),
                command.drawAt(), command.gateType(), command.gateAmount(), command.gateFrom(),
                command.minAccountAgeDays(), command.weightMode());
        for (CreateRaffleCampaignCommand.PrizeSkeleton p : command.prizes()) {
            prizePort.create(id, p.tier(), p.displayName(), p.kind(), "", p.sortOrder());
        }
        return id;
    }
}
