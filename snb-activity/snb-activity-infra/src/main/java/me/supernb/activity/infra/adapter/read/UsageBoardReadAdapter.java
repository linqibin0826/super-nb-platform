package me.supernb.activity.infra.adapter.read;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import me.supernb.sub2api.usageboard.UsageBoardReadModel;
import org.springframework.stereotype.Component;

/// UsageBoardReadPort 实现:薄适配,委托 snb-sub2api 的 UsageBoardReadModel,
/// 把上游行映射为 activity 自己的读视图(displayName 已在 starter 层脱敏)。
@Component
public class UsageBoardReadAdapter implements UsageBoardReadPort {

    private final UsageBoardReadModel readModel;

    /// 构造:注入 starter 提供的用量榜读模型。
    public UsageBoardReadAdapter(UsageBoardReadModel readModel) {
        this.readModel = readModel;
    }

    /// 委托 starter 读模型聚合,映射为 [UsageBoardRow]。
    @Override
    public List<UsageBoardRow> aggregate(Instant start, Instant end) {
        return readModel.aggregate(start, end).stream()
                .map(r -> new UsageBoardRow(r.userId(), r.displayName(), r.avatarUrl(),
                        r.tokens(), r.requests(), r.cost()))
                .toList();
    }

    /// 委托 starter 读模型查上榜门槛。
    @Override
    public boolean eligible(long userId) {
        return readModel.eligible(userId);
    }
}
