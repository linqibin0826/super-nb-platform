package me.supernb.sub2api.recharge;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// sub2api 充值 / 兑换码只读读模型(防腐层契约)。全部方法只读,时间窗口统一 [start, end)(end 为排他上界)。
///
/// 安全边界:面向公开信息流的方法(leaderboard/recentRecharges/displayNamesByIds)在实现内部
/// 完成邮箱脱敏,未脱敏的完整邮箱绝不跨出本模块。
public interface RechargeReadModel {

    /// 榜单条目(name 已脱敏)。
    ///
    /// @param name   已脱敏的用户标识
    /// @param amount 活动期充值合计(元)
    record LeaderRow(String name, BigDecimal amount) {
    }

    /// 充值动态条目(name 已脱敏)。
    ///
    /// @param name   已脱敏的用户标识
    /// @param amount 单笔充值金额(元)
    /// @param at     充值完成时刻
    record RechargeRow(String name, BigDecimal amount, Instant at) {
    }

    /// 兑换码状态。
    ///
    /// @param status    兑换码当前状态
    /// @param expiresAt 过期时刻(无过期时间则为 null)
    record RedeemCodeStatus(String status, Instant expiresAt) {
    }

    /// 活动期内该用户已完成的余额充值合计(元);无记录返回 0。
    BigDecimal totalRecharge(long userId, Instant start, Instant end);

    /// 活动期充值榜 Top limit(仅 role=user,金额倒序,name 已脱敏)。
    List<LeaderRow> leaderboard(Instant start, Instant end, int limit);

    /// 活动期最近充值流水(仅 role=user、金额 ≥¥10 滤掉测试单,完成时间倒序,name 已脱敏)。
    List<RechargeRow> recentRecharges(Instant start, Instant end, int limit);

    /// 批量取用户的**公开展示名**(仅 role=user):设过用户名就用用户名,没设才回退脱敏邮箱。
    /// 查无对应记录的 id 不出现在返回 map 中。
    ///
    /// 用户名原样返回、不脱敏——它是用户自己填的对外昵称,不是身份凭据(2026-07-30 实查生产:
    /// 6245 个号里 633 个设了用户名,含 `@` 的 0 个、像手机号的 0 个)。而邮箱恒脱敏,口径见 [EmailMask]。
    Map<Long, String> displayNamesByIds(Collection<Long> ids);

    /// 批量取兑换码状态;查无对应记录的 code 不出现在返回 map 中。
    Map<String, RedeemCodeStatus> codeStatuses(Collection<String> codes);

    /// 窗口 [since,until) 内有新增 COMPLETED 余额充值的用户 id(去重;补给记录成就候选
    /// 发现用,不做全表扫描)。
    java.util.List<Long> usersWithNewRechargeSince(java.time.Instant since, java.time.Instant until);

    /// 疯四桶资格名单:窗口内**单笔** ≥ minAmount 的 COMPLETED 余额单,一人只算首笔,
    /// 按首笔到账时刻升序,最多 limit 个。返回列表的下标 +1 即「桶序」。
    ///
    /// 口径与运营脚本 `thursday_scan.py` 逐字一致(spec §3「按到账顺序」,规则公示后不改判):
    /// 单笔门槛而非累计、一人一桶、排序键是到账时刻不是领取时刻——所以充得早的人
    /// 桶序在前,晚点才来领也不会被后来者挤掉。
    List<Long> qualifiedUserIdsInOrder(Instant start, Instant end, BigDecimal minAmount, int limit);

    /// 该用户在指定分组下是否已有带此 notes 的订阅(判「这一场领过没」)。
    ///
    /// 必须带 group_id:三场疯四共用固定 notes `opening-fk`、靠**分组**区分场次,
    /// 只按 notes 判会让第二场的回头客被误判成「已领」。
    boolean hasSubscription(long userId, long groupId, String notes);

    /// 首笔付款单(开学季首充礼定档用)。
    ///
    /// @param amountCny   订单面额(元,payment_orders.amount——返利/定档口径,非实付 pay_amount)
    /// @param completedAt 完成时刻
    record FirstOrder(BigDecimal amountCny, Instant completedAt) {
    }

    /// 该用户**人生第一笔** COMPLETED 付款订单(order_type ∈ {balance, subscription} 均算,
    /// 按 completed_at 最早取一;从未成功付过款返回 empty)。开学季「只认人生第一笔」的判定真源。
    java.util.Optional<FirstOrder> firstCompletedOrder(long userId);
}
