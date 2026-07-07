package me.supernb.sub2api.recharge;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// sub2api 充值 / 兑换码只读读模型(防腐层契约)。全部方法只读,时间窗口统一 [start, end)(end 为排他上界)。
///
/// 安全边界:面向公开信息流的方法(leaderboard/recentRecharges/maskedEmailsByIds)在实现内部
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

    /// 批量取用户的脱敏邮箱(仅 role=user);查无对应记录的 id 不出现在返回 map 中。
    Map<Long, String> maskedEmailsByIds(Collection<Long> ids);

    /// 批量取兑换码状态;查无对应记录的 code 不出现在返回 map 中。
    Map<String, RedeemCodeStatus> codeStatuses(Collection<String> codes);
}
