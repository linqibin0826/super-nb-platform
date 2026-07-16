package me.supernb.guide.app.usecase.command;

import dev.linqibin.commons.cqrs.Command;

/// 标记引导已读命令(幂等)。
public record AckGuideCommand(long userId, String key) implements Command<Void> {
}
