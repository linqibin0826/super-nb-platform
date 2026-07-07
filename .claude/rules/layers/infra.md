---
paths: snb-*/snb-*-infra/**/*.java
---

# Infra 层开发规范

## 核心职责

- 实现 domain/port 的端口：持久化、读投影、外部存储（R2）、图像处理、防腐层委托
- JPA 实体 ↔ 读视图（domain/model/read）的映射**手写**（如 `PromptMapper`），本项目无 MapStruct

## 包结构与命名（照 patra-catalog，`infra/adapter/` 下按能力分包）

| 组件 | 命名 | 注解 | 位置 |
|------|------|------|------|
| 写侧持久化适配器（事务/锁） | `{Thing}Adapter` | `@Repository`（享数据层异常翻译） | `infra/adapter/persistence/` |
| 读侧适配器（投影/统计） | `{Thing}Adapter` | `@Repository` 或 `@Component` | `infra/adapter/read/` |
| 其他能力适配器 | `{Thing}Adapter` | `@Component` | `infra/adapter/{storage,thumbnail,…}/` |
| JPA 实体 | `{Entity}Entity` | `@Entity` | `infra/adapter/persistence/entity/` |
| Spring Data 仓储 | `{Entity}JpaRepository` | 无（接口） | `infra/adapter/persistence/dao/` |
| 共享映射 | `{Entity}Mapper` | public 静态方法 | `infra/adapter/read/` |

## 事务

事务边界在这一层：注入 `TransactionTemplate`，`execute(...)` 显式包裹（禁 `@Transactional`——自调用代理坑）。需要"事务失败后在事务外补偿/回读"的模式（如点赞 toggle 撞 PK 后回读计数）必须把回读放 `execute` 之外。

## 消费 sub2api 防腐层

薄适配模式（`RechargeReadAdapter` 为样板，位于 `infra/adapter/read/`）：注入 starter 的读模型/客户端 → 把上游 DTO 映射为**本上下文的读视图**（domain/model/read）→ 上下文互不感知 sub2api 细节。规范见 tech/sub2api.md。

## R2 / 外部凭据

- AWS SDK v2（`S3Client` 写删 + `S3Presigner` 签 URL）
- 凭据仅环境变量注入；配置类 `@ConditionalOnProperty`（如 `gallery.r2.endpoint`）缺配不激活（测试即此形态）
- ⚠️ **yml 禁止写空串默认值**：空串会让 `@ConditionalOnProperty` 误判"存在"，后续 `URI.create("")` NPE

## JPA 细则

见 tech/jpa.md（patra 审计基座选型、雪花 id 预分配、PG 特有 SQL 的钦定形态、并发删除/计数、聚合级联、动态 HQL 测试义务）。
