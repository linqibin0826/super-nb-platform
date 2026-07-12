package me.supernb.activity.domain.model.raffle;

/// 报名回执:参会证号 + 是否为重复报名的幂等返回(already=true 时未新增记录)。
public record RaffleEntryTicket(int entryNo, boolean already) {}
