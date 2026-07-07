/// activity 库 3 张表的 JPA 实体(`{Entity}Entity`),按 patra 审计基座分类继承:聚合根
/// `CampaignEntity`、`DrawEntity` 继承 BaseJpaEntity(雪花 id + 全套审计列),子实体
/// `PrizeSlotEntity` 继承 ChildJpaEntity(领奖是独立更新语义,乐观锁列随身)。
///
/// campaign、prize_slot 由运维 SQL 维护,无业务构造器,只留 `@NoArgsConstructor(PROTECTED)`
/// 给 JPA;draw 的业务构造器在雪花 id 应用层预分配,`created_by` 由 JPA 审计自动填充为发起
/// 抽奖的登录用户。全部实体不加 `@Data`(全字段 equals/hashCode/toString 与懒加载、`@Id`
/// 语义冲突);字段与 Flyway DDL 一一对应,`ddl-auto=validate` 兜底两边不漂移,细则见 tech/jpa.md。
package me.supernb.activity.infra.adapter.persistence.entity;
