/// 写侧 JPA 持久化:聚合仓储适配器直接住这一层,`entity/`、`dao/` 两个子包放实现细节。
///
/// - `entity/`:JPA 实体(`{Entity}Entity`),按 patra 审计基座分类继承(聚合根用 BaseJpaEntity,
///   独立更新的子实体用 ChildJpaEntity,完全随聚合根生死的值对象表用 ValueObjectJpaEntity)
/// - `dao/`:Spring Data 仓储接口(`{Entity}JpaRepository`),派生方法与 JPQL 为主,无 native SQL
///
/// 直接类型 `GenerationRepositoryAdapter`(实现 `GenerationRepository`)与
/// `InteractionRepositoryAdapter`(实现 `InteractionRepository`),均标 `@Repository`(享 Spring
/// 数据层异常翻译);事务边界收在这一层,用 TransactionTemplate 显式包裹(禁 `@Transactional`,
/// 自调用代理坑,见 tech/jpa.md)。
package me.supernb.gallery.infra.adapter.persistence;
