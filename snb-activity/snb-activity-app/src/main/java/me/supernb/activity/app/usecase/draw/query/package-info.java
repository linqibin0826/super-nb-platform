/// 抽奖子域查询用例:`DrawStatusQueryService`(资格与剩余次数)、`MyDrawsQueryService`(本人中奖历史,
/// enrich 兑换码状态、面向本人不脱敏)、`RecentDrawsQueryService`(最近真实中奖信息流,排除安慰奖、服务端用
/// 脱敏邮箱做展示名)。无进行中活动时:`DrawStatusQueryService` 抛出 `CampaignNotActiveException`,
/// `MyDrawsQueryService`/`RecentDrawsQueryService` 则返回空列表;三者各自的降级口径以类级注释为准。
///
/// 命名与包位置照 `{View}QueryService` 约定,读路径不经 CommandBus。
package me.supernb.activity.app.usecase.draw.query;
