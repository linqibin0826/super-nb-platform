package me.supernb.activity.domain.port.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import me.supernb.activity.domain.model.raffle.GateType;

/// 门槛只读端口(sub2api 库):真金口径——RECHARGE=COMPLETED 余额充值单(completed_at 窗口),
/// SPEND=billing_type=0 余额扣费(created_at 窗口)。窗口一律 [from, to)。
public interface RaffleGateReadPort {

    /// 单人门槛指标值(报名校验/进度条)。
    BigDecimal gateValue(long userId, GateType type, Instant from, Instant to);

    /// 批量门槛指标值(开奖复核,一条 SQL);窗口内无流水的 user 缺席于返回 map(调用方按 0 处理)。
    Map<Long, BigDecimal> gateValues(Collection<Long> userIds, GateType type, Instant from, Instant to);

    /// 注册时刻(账龄门槛用);查无此人缺席于返回 map。
    Map<Long, Instant> registeredAts(Collection<Long> userIds);

    /// 展示名(username 优先,否则脱敏邮箱——契约同用量榜:前2+***+后2)。
    Map<Long, String> displayNames(Collection<Long> userIds);
}
