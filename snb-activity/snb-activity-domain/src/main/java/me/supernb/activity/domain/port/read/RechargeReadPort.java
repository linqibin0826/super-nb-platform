package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.model.read.CodeStatus;
import me.supernb.activity.domain.model.read.LeaderEntry;
import me.supernb.activity.domain.model.read.RechargeEntry;

/// 充值只读查询端口(infra 委托 snb-sub2api 的 RechargeReadModel 实现)。窗口一律
/// [start, end),end 为排他上界。
public interface RechargeReadPort {

    /// 活动期内已完成的余额充值合计(元);无记录返回 0。
    BigDecimal totalRecharge(long userId, Instant start, Instant end);

    /// 活动期充值榜 Top limit(仅 role=user,金额降序,name 已脱敏)。
    List<LeaderEntry> leaderboard(Instant start, Instant end, int limit);

    /// 活动期最近充值流水(仅 role=user、金额 ≥¥10 滤测试单,完成时间倒序,name 已脱敏)。
    List<RechargeEntry> recentRecharges(Instant start, Instant end, int limit);

    /// 批量取脱敏邮箱(role=user);找不到的 id 不在返回 map。
    Map<Long, String> maskedEmailsByIds(Collection<Long> ids);

    /// 批量取兑换码状态;找不到的 code 不在返回 map。
    Map<String, CodeStatus> codeStatuses(Collection<String> codes);
}
