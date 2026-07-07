/// 写侧 JPA 持久化:聚合仓储适配器直接住这一层,`entity/`、`dao/` 两个子包放实现细节。
///
/// - `entity/`:JPA 实体(`{Entity}Entity`),按 patra 审计基座分类继承(聚合根 campaign/draw 用
///   BaseJpaEntity,独立更新的子实体 prize_slot 用 ChildJpaEntity)
/// - `dao/`:Spring Data 仓储接口(`{Entity}JpaRepository`);并发发奖用到的 PG 特有原语
///   (advisory lock、FOR UPDATE SKIP LOCKED)落 native SQL,其余走派生方法/JPQL
///
/// 直接类型 `CampaignAdapter`(实现 `CampaignPort`)与 `DrawAdapter`(实现 `DrawPort`,抽奖原子
/// 事务的落点),均标 `@Repository`(享 Spring 数据层异常翻译);事务边界收在这一层,用
/// TransactionTemplate 显式包裹(禁 `@Transactional`,自调用代理坑,见 tech/jpa.md)。
package me.supernb.activity.infra.adapter.persistence;
