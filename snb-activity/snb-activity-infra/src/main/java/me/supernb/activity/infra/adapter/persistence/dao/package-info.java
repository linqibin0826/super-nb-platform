/// activity 库 3 张表的 Spring Data 仓储接口(`{Entity}JpaRepository`),一表一接口。
///
/// 并发发奖的两处 PG 特有原语落 native SQL:`DrawJpaRepository.acquireUserXactLock`
/// (`pg_advisory_xact_lock`,事务级 advisory lock,void 函数包一层子查询取 boolean)与
/// `PrizeSlotJpaRepository.lockRandomAvailable`(`FOR UPDATE SKIP LOCKED` 原子领槽,返回
/// 受管实体,改字段即入 dirty checking);其余查询走派生方法/JPQL(含接口投影 `PoolTierView`),
/// 细则见 tech/jpa.md。
///
/// 只被 persistence 包下的两个持久化适配器与 read 包下的 PoolReadAdapter 注入消费。
package me.supernb.activity.infra.adapter.persistence.dao;
