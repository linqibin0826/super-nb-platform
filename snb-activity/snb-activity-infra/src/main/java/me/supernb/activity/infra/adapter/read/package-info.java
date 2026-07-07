/// 读侧适配器:`PoolReadAdapter` 实现 `PoolReadPort`(奖池档位统计,委托 dao 包下的
/// `PrizeSlotJpaRepository` 投影查询),`RechargeReadAdapter` 实现 `RechargeReadPort`
/// (充值/兑换码只读数据,薄适配 snb-sub2api starter 的 `RechargeReadModel`)。
///
/// 两者都不碰写路径;`RechargeReadAdapter` 是本仓 infra 消费 sub2api 防腐层的样板
/// (~30 行,只做 DTO 映射,不重复脱敏/SQL 逻辑——那些已经在 starter 内完成),
/// 细则见 tech/sub2api.md、tech/port-service.md。
package me.supernb.activity.infra.adapter.read;
