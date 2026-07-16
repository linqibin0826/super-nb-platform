package me.supernb.guide.app.usecase.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import me.supernb.guide.domain.model.GuideKey;
import me.supernb.guide.domain.port.repository.GuideAckRepository;
import org.springframework.stereotype.Service;

/// 已读用例:key 格式把门后幂等落库。
@Service
public class AckGuideHandler implements CommandHandler<AckGuideCommand, Void> {

    private final GuideAckRepository acks;

    /// 构造:注入已读仓储端口。
    public AckGuideHandler(GuideAckRepository acks) {
        this.acks = acks;
    }

    @Override
    public Void handle(AckGuideCommand cmd) {
        acks.ack(cmd.userId(), GuideKey.checked(cmd.key()));
        return null;
    }
}
