package me.supernb.activity.domain.port;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.read.CodeStatus;
import me.supernb.activity.domain.model.read.LeaderEntry;
import me.supernb.activity.domain.model.read.RechargeEntry;

/// 充值只读查询端口(infra 委托 snb-sub2api 的 RechargeReadModel 实现)。窗口 [start, end)。
public interface RechargeQueryPort {

    BigDecimal totalRecharge(long userId, Instant start, Instant end);

    List<LeaderEntry> leaderboard(Instant start, Instant end, int limit);

    List<RechargeEntry> recentRecharges(Instant start, Instant end, int limit);

    /// 批量取脱敏邮箱(role=user);找不到的 id 不在返回 map。
    Map<Long, String> maskedEmailsByIds(Collection<Long> ids);

    /// 批量取兑换码状态;找不到的 code 不在返回 map。
    Map<String, CodeStatus> codeStatuses(Collection<String> codes);
}
