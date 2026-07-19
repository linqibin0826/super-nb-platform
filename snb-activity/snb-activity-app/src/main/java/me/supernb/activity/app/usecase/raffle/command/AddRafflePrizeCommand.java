package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;

/// 管理端手动新增一件奖品(payload 已知——手填或从别处拿到的码/口令),仅限开奖前的期。
public record AddRafflePrizeCommand(long campaignId, String tier, String displayName, String kind,
        String payload, int sortOrder) implements Command<Long> {}
