package me.supernb.activity.domain.port.read;

import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.usage.UsageBoardRow;

/// 用量榜只读查询端口(infra 委托 snb-sub2api 的 UsageBoardReadModel 实现)。窗口 [start, end),end 排他。
public interface UsageBoardReadPort {

    /// 窗口 [start, end) 内逐用户聚合 tokens/requests/cost,无序返回(排序是上层的事)。
    List<UsageBoardRow> aggregate(Instant start, Instant end);

    /// 上榜门槛单查:该用户是否存在 COMPLETED balance 充值单。
    boolean eligible(long userId);
}
