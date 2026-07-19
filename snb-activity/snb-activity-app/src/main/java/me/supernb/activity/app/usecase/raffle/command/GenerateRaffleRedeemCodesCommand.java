package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;
import java.util.List;

/// 生成 count 张同档位/同分组兑换码,一次调用一批,单事务落库。count 上限 100
/// (与 sub2api 侧硬限制对齐,这里先拦不用等上游 400);sortOrderStart 起,按生成顺序递增。
public record GenerateRaffleRedeemCodesCommand(long campaignId, String tier, String displayName,
        long groupId, int validityDays, int count, int sortOrderStart) implements Command<List<Long>> {}
