package me.supernb.sub2api.usageboard;

import java.time.Instant;
import java.util.List;

/// 用量排行榜只读读模型:窗口内(左闭右开)逐用户聚合 tokens/requests/actual_cost。
/// 口径:只收 role='user' AND status='active' AND deleted_at IS NULL 且历史有
/// COMPLETED balance 充值(payment_orders,家族红线:绝不用 users.total_recharged)的用户;
/// cost 仅计余额扣费(billing_type=0),订阅套餐消耗不计入金额(tokens/requests 全量);
/// displayName 服务端择名+脱敏(username 优先,否则邮箱前2+***+后2),原始邮箱不出本层。
public interface UsageBoardReadModel {

    /// 聚合一行:displayName 已脱敏;avatarUrl 可空(无头像或空串);cost 为 actual_cost 之和。
    record UsageRow(long userId, String displayName, String avatarUrl,
                    long tokens, long requests, double cost) {}

    /// 窗口 [start, end) 内的逐用户聚合,无序返回(排序是上层的事)。
    List<UsageRow> aggregate(Instant start, Instant end);

    /// 上榜门槛单查:该用户是否存在 COMPLETED balance 充值单(me 缺席时区分 no_usage/not_eligible 用)。
    boolean eligible(long userId);
}
