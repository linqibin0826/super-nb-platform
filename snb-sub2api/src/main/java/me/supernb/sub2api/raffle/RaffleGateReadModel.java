package me.supernb.sub2api.raffle;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// 发布会门槛只读读模型:按用户聚合真金口径的充值/消费。窗口一律 [from, to)。
/// gateType 收字符串 "RECHARGE" | "SPEND"(本模块不依赖 activity 枚举),其余值抛 IllegalArgumentException。
public interface RaffleGateReadModel {

    /// 单人门槛指标值;无流水返回 0。
    BigDecimal gateValue(long userId, String gateType, Instant from, Instant to);

    /// RECHARGE 口径的逐笔到账明细,按到账时刻升序(签到准入闸滑窗算「网费余 N 天」用,
    /// 聚合值给不出逐笔滑出时点)。判定条件与 gateValue 的 RECHARGE 完全同源。
    List<RechargeEvent> rechargeEvents(long userId, Instant from, Instant to);

    /// 一笔真实充值到账事件(在线支付按 completed_at,购码按 used_at)。
    record RechargeEvent(Instant at, BigDecimal amount) {
    }

    /// 批量门槛指标值(一条 SQL);窗口内无流水的 user 缺席于返回 map;空入参返回空 map。
    Map<Long, BigDecimal> gateValues(Collection<Long> userIds, String gateType, Instant from, Instant to);

    /// 注册时刻;查无此人缺席;空入参返回空 map。
    Map<Long, Instant> registeredAts(Collection<Long> userIds);

    /// 展示名:username 非空白用 username,否则脱敏邮箱(前2+***+后2,契约同用量榜);空入参返回空 map。
    Map<Long, String> displayNamesByIds(Collection<Long> userIds);

    /// 当前站内余额(users.balance,不含冻结);查无此人返回 0。余额闸(2026-08-18)用。
    BigDecimal balance(long userId);
}
