package me.supernb.activity.app.usecase.draw.command;

import dev.linqibin.commons.cqrs.Command;
import me.supernb.activity.domain.model.DrawResult;

/// 抽奖命令:当前登录用户在进行中活动里抽一次。
public record PerformDrawCommand(long userId) implements Command<DrawResult> {
}
