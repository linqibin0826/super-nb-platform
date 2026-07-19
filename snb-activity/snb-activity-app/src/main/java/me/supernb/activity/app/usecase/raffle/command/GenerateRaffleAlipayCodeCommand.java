package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;

/// 生成一个不重复的支付宝口令字符串。prizeId 非空=回填已有奖品行的 payload(克隆草稿留下的
/// 空壳行);为空=新建一件奖品。tier/displayName/sortOrder 只在新建路径生效,回填路径沿用
/// 原行既有值(不重复传,避免和"改名字用 UpdateRafflePrizeCommand"职责重叠)。
public record GenerateRaffleAlipayCodeCommand(long campaignId, Long prizeId, String tier,
        String displayName, int sortOrder) implements Command<Long> {}
