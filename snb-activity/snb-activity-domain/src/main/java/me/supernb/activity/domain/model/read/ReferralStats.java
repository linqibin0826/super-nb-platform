package me.supernb.activity.domain.model.read;

/// 拉新活动全场统计(公开读视图)。
///
/// @param newcomers 本期新人总数:活动窗口内注册且未软删的用户数(只看注册,不要求开组或有邀请人)
public record ReferralStats(long newcomers) {
}
