package me.supernb.activity.app.usecase.achievement.command;

import dev.linqibin.commons.cqrs.Command;
import java.util.List;

/// 批量标记成就已读(每次可 1 个或多个 code;前端约定每个信封各自独立发一次单元素请求)。
public record MarkAchievementsSeenCommand(long userId, List<String> codes) implements Command<Integer> {
}
