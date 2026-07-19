package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;

/// 作废:任意状态均可调用(含已开奖——彩排局 id=1 先例,开奖后仍需要把问题期隐身)。
public record CancelRaffleCampaignCommand(long campaignId) implements Command<Void> {}
