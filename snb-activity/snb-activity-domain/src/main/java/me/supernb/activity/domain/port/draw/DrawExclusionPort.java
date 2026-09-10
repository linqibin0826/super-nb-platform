package me.supernb.activity.domain.port.draw;

/// 抽奖排除端口:某些接入方式的用户不参与抽奖(2026-09-10 站长拍板:中转接入——被授权了
/// 「Claude-Max-下游专属」1.0x 分组的用户——已经拿了折扣,幸运余额包只给散客抽)。
/// 具体「哪些分组算中转」由 infra 侧配置决定,domain 只问「这个人排不排除」。
public interface DrawExclusionPort {

    /// 该用户是否被排除在抽奖之外(true = 不能抽,充够了也不算)。
    boolean isExcluded(long userId);
}
