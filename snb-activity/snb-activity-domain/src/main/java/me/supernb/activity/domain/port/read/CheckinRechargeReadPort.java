package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

/// 补给资格窗口充值只读端口(sub2api 库):判定窗口=当月新增真实充值(spec §5.2 决策1),
/// 窗口一律 [monthStart, monthEndExclusive)。
public interface CheckinRechargeReadPort {

    /// 单人窗口内真实充值(元);无流水返回 0。
    BigDecimal monthlyRecharge(long userId, Instant monthStart, Instant monthEndExclusive);

    /// 批量窗口内真实充值(一条 SQL,月度结算批处理用);窗口内无流水的 user 缺席于返回 map。
    Map<Long, BigDecimal> monthlyRecharges(Collection<Long> userIds, Instant monthStart,
            Instant monthEndExclusive);
}
