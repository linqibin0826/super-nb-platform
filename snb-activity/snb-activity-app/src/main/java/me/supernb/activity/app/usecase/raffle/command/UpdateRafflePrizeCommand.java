package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;

public record UpdateRafflePrizeCommand(long campaignId, long prizeId, String tier, String displayName,
        String kind, String payload, int sortOrder) implements Command<Void> {}
