package me.supernb.guide.app.usecase.command;

import dev.linqibin.commons.cqrs.CommandHandler;
import java.util.Set;
import me.supernb.guide.domain.exception.GuideException;
import me.supernb.guide.domain.model.GuideKey;
import me.supernb.guide.domain.port.repository.GuideAckRepository;
import org.springframework.stereotype.Service;

/// 已读用例:key 格式把门 → 幂等短路 → 单用户上限封顶 → 落库。
/// 上限防「登录用户脚本刷不同 key 无限膨胀 guide_ack」的存储 DoS(安全审计发现;
/// /guide/v1 无速率限流,故在应用层给单用户已读行数封顶,比限流更本质地堵住表膨胀)。
@Service
public class AckGuideHandler implements CommandHandler<AckGuideCommand, Void> {

    private final GuideAckRepository acks;

    /// 构造:注入已读仓储端口。
    public AckGuideHandler(GuideAckRepository acks) {
        this.acks = acks;
    }

    @Override
    public Void handle(AckGuideCommand cmd) {
        String key = GuideKey.checked(cmd.key());
        Set<String> existing = acks.ackedKeys(cmd.userId());
        if (existing.contains(key)) {
            return null; // 已读,幂等短路(不占新配额)
        }
        if (existing.size() >= GuideKey.MAX_PER_USER) {
            throw GuideException.tooMany(GuideKey.MAX_PER_USER);
        }
        acks.ack(cmd.userId(), key);
        return null;
    }
}
