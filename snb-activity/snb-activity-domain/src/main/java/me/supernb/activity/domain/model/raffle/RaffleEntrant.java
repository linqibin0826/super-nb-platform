package me.supernb.activity.domain.model.raffle;

/// 报名者(列席代表):userId + 参会证号。
public record RaffleEntrant(long userId, int entryNo) {}
