/// 活动期查询用例:`LeaderboardQueryService`(充值榜 Top10)、`PoolQueryService`(奖池实况,按档位统计余量)、
/// `RecentRechargesQueryService`(近期充值动态 Top20)。三者都先取进行中活动,再委托对应读端口取数据:
/// `LeaderboardQueryService`/`RecentRechargesQueryService` 按活动窗口 `[startsAt, endsAt)` 委托
/// `RechargeReadPort`,`PoolQueryService` 按活动 id 委托 `PoolReadPort` 取当前奖池余量(不按时间窗口查询)。
/// 无进行中活动统一返回空列表,是显式的降级语义,不是异常。
///
/// 命名与包位置照 `{View}QueryService` 约定:纯查询编排、无副作用、不定义接口,由 controller 直接注入,
/// 读路径不经 CommandBus。
package me.supernb.activity.app.usecase.campaign.query;
