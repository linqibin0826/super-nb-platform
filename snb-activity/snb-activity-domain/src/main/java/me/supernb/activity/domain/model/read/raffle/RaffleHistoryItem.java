package me.supernb.activity.domain.model.read.raffle;

import java.time.Instant;

/// 历届通报存档条目。
public record RaffleHistoryItem(long id, String name, Instant drawnAt, int prizeCount, int entrantCount) {}
