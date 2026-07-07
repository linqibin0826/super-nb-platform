package me.supernb.sub2api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/// sub2api 充值/兑换码只读读模型(防腐层)。全部只读,窗口一律 [start, end)(end 排他上界)。
///
/// 安全边界:面向公开信息流的方法(leaderboard/recentRecharges/maskedEmailsByIds)在本模块内
/// 完成邮箱脱敏,全名邮箱绝不出模块。
public interface RechargeReadModel {

    /// 榜单/流水条目(name 已脱敏)。
    record LeaderRow(String name, BigDecimal amount) {
    }

    /// 充值动态条目(name 已脱敏)。
    record RechargeRow(String name, BigDecimal amount, Instant at) {
    }

    /// 兑换码状态。
    record RedeemCodeStatus(String status, Instant expiresAt) {
    }

    /// 活动期内已完成的余额充值合计(元);无记录返回 0。
    BigDecimal totalRecharge(long userId, Instant start, Instant end);

    /// 活动期充值榜 Top limit(仅 role=user,金额倒序,name 已脱敏)。
    List<LeaderRow> leaderboard(Instant start, Instant end, int limit);

    /// 活动期最近充值流水(仅 role=user、金额 ≥¥10 滤测试单,完成时间倒序,name 已脱敏)。
    List<RechargeRow> recentRecharges(Instant start, Instant end, int limit);

    /// 批量取用户脱敏邮箱(仅 role=user);找不到的 id 不在返回 map 中。
    Map<Long, String> maskedEmailsByIds(Collection<Long> ids);

    /// 批量取兑换码状态;找不到的 code 不在返回 map 中。
    Map<String, RedeemCodeStatus> codeStatuses(Collection<String> codes);
}
