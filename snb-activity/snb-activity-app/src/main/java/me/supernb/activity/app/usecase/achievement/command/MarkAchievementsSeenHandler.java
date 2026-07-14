package me.supernb.activity.app.usecase.achievement.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.activity.domain.port.achievement.AchievementUnlockPort;
import org.springframework.stereotype.Service;

/// 标记已读编排:纯委托,无业务规则(已读是幂等状态位,重复标记不报错)。
@Service
public class MarkAchievementsSeenHandler implements CommandHandler<MarkAchievementsSeenCommand, Integer> {

    private final AchievementUnlockPort unlockPort;

    public MarkAchievementsSeenHandler(AchievementUnlockPort unlockPort) {
        this.unlockPort = unlockPort;
    }

    @Override
    public Integer handle(MarkAchievementsSeenCommand command) {
        return unlockPort.markSeen(command.userId(), command.codes());
    }
}
