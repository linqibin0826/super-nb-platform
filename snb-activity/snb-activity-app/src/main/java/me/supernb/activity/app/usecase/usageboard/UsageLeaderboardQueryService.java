package me.supernb.activity.app.usecase.usageboard;

import me.supernb.activity.domain.model.read.usage.BoardMetric;
import me.supernb.activity.domain.model.read.usage.BoardPeriod;
import me.supernb.activity.domain.model.read.usage.BoardView;
import me.supernb.activity.domain.port.read.UsageBoardReadPort;
import org.springframework.stereotype.Service;

/// 用量排行榜查询用例:从缓存取周期数据集,按请求者组装对外视图;数据全在内存,
/// me 缺席时懒查一次上榜门槛。缓存未预热(启动即挂/只读库不可用)返回 null,
/// 由 adapter 层译成 503——app 层不引 spring-web(spec §12)。
@Service
public class UsageLeaderboardQueryService {

    private final UsageBoardCache cache;
    private final UsageBoardReadPort readPort;

    /// 构造:注入缓存与读端口(读端口仅用于 me 缺席时的门槛单查)。
    public UsageLeaderboardQueryService(UsageBoardCache cache, UsageBoardReadPort readPort) {
        this.cache = cache;
        this.readPort = readPort;
    }

    /// 取一份榜单视图;缓存未预热返回 null。
    public BoardView board(BoardPeriod period, BoardMetric metric, long userId) {
        BoardDataset ds = cache.dataset(period);
        if (ds == null) {
            return null;
        }
        return BoardAssembler.view(ds, period, metric, userId, readPort::eligible);
    }
}
