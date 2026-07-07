# JPA 使用规范

数据访问统一 Spring Data JPA + Flyway；自动配置来自 `commons-starter-jpa`（Hibernate 7 调优：批量 500、insert/update 排序、关闭启动期 JDBC 元数据探测、Jackson3 JSON mapper、JPA 审计、数据层错误映射——全部开箱生效，不要重复配置）。

## 实体基座（patra 审计基座，2026-07-07 验收意见⑥接入）

全部实体继承 `commons-starter-jpa` 的基座家族，按语义选：

| 基类 | 字段 | 本仓实例 |
|------|------|----------|
| **BaseJpaEntity** | 雪花 id + record_remarks(JSONB) + created_at/created_by/created_by_name + updated_at/updated_by/updated_by_name + version + ip_address | 聚合根：campaign、draw、category、prompt、generation |
| **ChildJpaEntity** | 雪花 id + created_at/updated_at + version | 独立生命周期/独立更新的子实体：prize_slot、prompt_like、prompt_favorite、ref_image |
| **ValueObjectJpaEntity** | 仅雪花 id | 完全随聚合根的值对象表：generation_image、generation_ref |
| SoftDeletable 两变体 | 上两者 + Hibernate `@SoftDelete`(deleted_at) | **本仓暂无**——现有删除语义都是真删（generation 删除连 R2 对象一起清，软删会留悬空引用）；需要软删的新聚合再选 |

- 实体子类风格：`@Getter` + `@NoArgsConstructor(PROTECTED)` + 意图构造器/意图方法；基座已带 `@Data`/`@SuperBuilder`，子类**不要**再加 `@Data`（全字段 equals/toString 与懒加载冲突，见 code-style.md）
- **雪花 ID 应用层预分配**：业务构造器第一行 `setId(SnowflakeIdGenerator.getId())`；级联子实体在自己的构造器里各自预分配。例外：generation 的 id 由端口 `nextId()` 预分配后传入构造器（R2 键须先于落库确定，见下节）。无业务写路径的实体（campaign/category/prompt 由运维 SQL/收录管线维护）不写业务构造器
- **审计操作人**：`@CreatedBy`/`@LastModifiedBy` 由 boot 的 `auditorAware` Bean 供值——读 `@CurrentUser` 解析器挂的请求属性（`CurrentUserAuditorConfig`）。⚠️ **Bean 名必须叫 `auditorAware`**（starter 的 `@EnableJpaAuditing(auditorAwareRef = "auditorAware")` 按名引用，改名会让容器起不来）。无请求上下文（迁移/定时任务/infra 测试）审计人留 NULL
- created_by_name / updated_by_name / ip_address 暂不填（sub2api 画像无用户名；列随基座存在，将来要用再接）
- **纯 SQL 写入（数据迁移/收录管线/测试造数）必须显式给 id**；审计列靠 DDL DEFAULT 兜底（created_at/updated_at `DEFAULT now()`、version `DEFAULT 0`）

## 对外 id 契约（验收意见⑦：单一身份）

雪花 id 就是唯一身份，对内对外同一条——**不设 client_task_id 类自然键双轨**（一行两 id 的赘余已废）：

- **JSON 里实体 id 一律字符串**（雪花 ≈ 3e17 超 JS `Number.MAX_SAFE_INTEGER`）：读视图/DTO 的 id 字段建模为 `String`，mapper 用 `String.valueOf(...)` 转；路径/查询参数照常收 `long`（Spring 解析字符串，非法数字自然 400）
- **持久化前就要用 id 的聚合**（generation：R2 键 `gen/{userId}/{id}/…` 须先于落库确定）由仓储端口 `nextId()` 预分配、实体构造器收传入 id；其余实体仍在业务构造器内自取雪花
- 成员表幂等走**唯一约束**：`UNIQUE(prompt_id, user_id)`、`UNIQUE(user_id, sha256)`（复合主键/@EmbeddedId 已全部退役）
- 建单幂等预检随 client_task_id 一并退役：id 服务端生成、客户端无法预给；防重靠前端队列单飞（显式取舍：极端重试接受重复历史行）

## 与 patra 的刻意差异

- **保留 Flyway + `ddl-auto=validate`**（patra 无迁移脚本）；索引/唯一约束以 DDL 为单一真源，实体注解不重复声明
- **无 MapStruct**：Entity ↔ 读视图（domain/model/read）手写 mapper
- **事务在 infra 用 `TransactionTemplate`**（patra 规范是 app 层 `@Transactional`）

## PG 特有 SQL 留 nativeQuery（钦定形态，别改写）

```java
// advisory lock：void 函数没法直接标量映射，包一层子查询
@Query(value = "SELECT true FROM (SELECT pg_advisory_xact_lock(:key)) AS acquired", nativeQuery = true)
boolean acquireUserXactLock(@Param("key") long key);

// 随机领槽：native 返回【受管实体】，拿到后直接改字段即入 dirty-checking
// （SELECT * 天然带上基座审计列；实体有 @Version，行已被 FOR UPDATE 锁住，无并发版本冲突）
@Query(value = "SELECT * FROM activity.prize_slot WHERE campaign_id = :campaignId "
        + "AND status = 'available' ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED", nativeQuery = true)
Optional<PrizeSlotEntity> lockRandomAvailable(@Param("campaignId") long campaignId);
```

其余查询优先派生方法 / `@Query` JPQL（含 theta join、接口投影、`org.springframework.data.domain.Limit`）。

## 并发语义（踩坑规则）

1. **并发删除禁用派生 delete**：select-then-remove 在并发 0 行删除时抛 `StaleStateException`。必须 `@Modifying @Query("DELETE ...")` 批量删除，用返回行数决定语义（是否计入减量）
2. **计数增减**用 `@Modifying` UPDATE 原子加减（`SET like_count = like_count + :delta`），禁止读-改-写（批量语句不走乐观锁，别指望 version 拦并发计数）
3. **toggle 幂等**（点赞/收藏）：插入撞成员唯一约束 → 整事务回滚 → 在事务**外**捕获 `DataIntegrityViolationException` → 回读计数返回
4. **唯一键竞态重试一次**：按 (user_id, sha256) 去重类写入撞 `DataIntegrityViolationException` 时重试一轮，重试轮 exists 命中改走跳过

## 聚合写入

- 聚合根 `cascade = ALL` + `orphanRemoval = true`；新聚合直接 `dao.save()`——实体带 `@Version`，version 为 null 判定为新实体走 persist（雪花 id 预填不会误触发 merge）

## 事务

`TransactionTemplate` 显式包裹（禁 `@Transactional`，自调用代理坑）。回滚后的补偿/回读必须放 `execute` 之外。

## 动态 HQL 的测试义务

`EntityManager.createQuery` 动态拼接的 HQL（如列表排序 `NULLS LAST` 分支）**启动期不校验**（`@Query` 才会）——每个拼接分支必须有测试遍历，否则错到线上才炸。片段只许常量拼接，参数一律绑定。

## Flyway

- 单 Flyway 历史跨双 schema → **版本号全局唯一**（V1、V2 已用，新迁移从 V3 起，两上下文共享序号）
- 迁移文件放对应 infra 模块 `resources/db/migration/{context}/`
- `ddl-auto=validate` 兜实体映射与迁移结果一致，两边改任何一边都要同步另一边（审计列/JSONB/BYTEA 都在校验范围）
