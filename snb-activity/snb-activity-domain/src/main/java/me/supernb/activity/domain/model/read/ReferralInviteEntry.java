package me.supernb.activity.domain.model.read;

/// 拉新人数榜条目,`ReferralReadPort.inviteBoard` 按有效邀请人数降序返回。
///
/// @param name  邀请人脱敏邮箱(如 ab***@qq.com)
/// @param count 有效邀请人数(曾开通新人组的被邀请人数)
public record ReferralInviteEntry(String name, int count) {
}
