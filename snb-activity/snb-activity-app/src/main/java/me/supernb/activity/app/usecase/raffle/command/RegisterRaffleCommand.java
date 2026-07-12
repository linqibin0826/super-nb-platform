package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;

/// 报名命令:campaignId 来自请求体解析,userId 来自登录态;ip/ua 为秋后清算留痕(可空)。
public record RegisterRaffleCommand(long campaignId, long userId, String clientIp, String userAgent)
        implements Command<RaffleEntryTicket> {}
