---
paths: snb-*/snb-*-infra/**/*.java
---

# JPA 使用规范

数据访问统一 Spring Data JPA + Flyway；自动配置来自 `commons-starter-jpa`（Hibernate 7 调优：批量 500、insert/update 排序、关闭启动期 JDBC 元数据探测、Jackson3 JSON mapper、JPA 审计、数据层错误映射——全部开箱生效，不要重复配置）。

## 与 patra 的刻意差异

- **实体不继承 `BaseJpaEntity`**：其 9 列审计基座与我们既定 schema 不匹配。`created_at` 两式任选其一：
  - DB default + `@Column(insertable = false, updatable = false)`（读回即可，如 `CampaignEntity`）
  - `@CreatedDate` + `@EntityListeners(AuditingEntityListener.class)`（插入时框架填，如 `DrawEntity`）
- **无雪花 ID**：主键用库 identity 或自然键/复合键（`@EmbeddedId`）
- **无软删除、无 MapStruct**

## PG 特有 SQL 留 nativeQuery（钦定形态，别改写）

```java
// advisory lock：void 函数没法直接标量映射，包一层子查询
@Query(value = "SELECT true FROM (SELECT pg_advisory_xact_lock(:key)) AS acquired", nativeQuery = true)
boolean acquireUserXactLock(@Param("key") long key);

// 随机领槽：native 返回【受管实体】，拿到后直接改字段即入 dirty-checking
@Query(value = "SELECT * FROM activity.prize_slot WHERE campaign_id = :campaignId "
        + "AND status = 'available' ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
Optional<PrizeSlotEntity> lockRandomAvailable(@Param("campaignId") long campaignId);
```

其余查询优先派生方法 / `@Query` JPQL（含 theta join、接口投影、`org.springframework.data.domain.Limit`）。

## 并发语义（踩坑规则）

1. **并发删除禁用派生 delete**：select-then-remove 在并发 0 行删除时抛 `StaleStateException`。必须 `@Modifying @Query("DELETE ...")` 批量删除，用返回行数决定语义（是否计入减量）
2. **计数增减**用 `@Modifying` UPDATE 原子加减（`SET like_count = like_count + :delta`），禁止读-改-写
3. **toggle 幂等**（点赞/收藏）：插入撞 PK → 整事务回滚 → 在事务**外**捕获 `DataIntegrityViolationException` → 回读计数返回
4. **PK 竞态重试一次**：按 (user_id, sha256) 去重类写入撞 `DataIntegrityViolationException` 时重试一轮，重试轮 exists 命中改走跳过

## 聚合写入

- 聚合根 `cascade = ALL` + `orphanRemoval = true`，写入用 `em.persist`（新实体别 merge）
- 幂等 save：主键已存在 → 直接返回既有 `createdAt`，不重写

## 事务

`TransactionTemplate` 显式包裹（禁 `@Transactional`，自调用代理坑）。回滚后的补偿/回读必须放 `execute` 之外。

## 动态 HQL 的测试义务

`EntityManager.createQuery` 动态拼接的 HQL（如列表排序 `NULLS LAST` 分支）**启动期不校验**（`@Query` 才会）——每个拼接分支必须有测试遍历，否则错到线上才炸。片段只许常量拼接，参数一律绑定。

## Flyway

- 单 Flyway 历史跨双 schema → **版本号全局唯一**（V1、V2 已用，新迁移从 V3 起，两上下文共享序号）
- 迁移文件放对应 infra 模块 `resources/db/migration/{context}/`
- `ddl-auto=validate` 兜实体映射与迁移结果一致，两边改任何一边都要同步另一边
