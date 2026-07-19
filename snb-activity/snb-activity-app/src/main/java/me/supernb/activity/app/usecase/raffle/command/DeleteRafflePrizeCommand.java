package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;

public record DeleteRafflePrizeCommand(long campaignId, long prizeId) implements Command<Void> {}
