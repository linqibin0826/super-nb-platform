package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;

/// 对指定奖品行(空壳兑换码)生成 1 张码并就地回填,返回该奖品 id。
/// 克隆骨架「逐行点亮」的主路径:批量生成产生新行,本命令填既有行。
public record GenerateRaffleRedeemCodeForPrizeCommand(long campaignId, long prizeId, long groupId,
        int validityDays) implements Command<Long> {}
