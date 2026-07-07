package me.supernb.activity.domain.model.read;

/// 抽奖资格状态,GET /activity/v1/status 直接返回。
///
/// @param eligible  活动期充值总额是否已达抽奖门槛(¥100);与 remaining 是否 >0 不等价——
///                  已达门槛但次数已抽满时 eligible 仍为 true、remaining 为 0
/// @param remaining 剩余可抽次数(DrawEligibility.remainingDraws 的计算结果,不为负)
public record DrawStatus(boolean eligible, int remaining) {
}
