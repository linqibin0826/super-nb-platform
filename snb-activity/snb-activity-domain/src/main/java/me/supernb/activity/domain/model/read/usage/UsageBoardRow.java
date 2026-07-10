package me.supernb.activity.domain.model.read.usage;

/// 用量榜一行:窗口内单用户的 tokens/requests/cost 聚合(displayName 已在 infra 委托层脱敏)。
///
/// @param userId      用户 id
/// @param displayName 脱敏后的展示名
/// @param avatarUrl   头像地址,可为空
/// @param tokens      窗口内 token 用量之和
/// @param requests    窗口内请求次数之和
/// @param cost        窗口内实际花费(actual_cost)之和
public record UsageBoardRow(long userId, String displayName, String avatarUrl,
                            long tokens, long requests, double cost) {
}
