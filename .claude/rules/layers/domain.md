---
paths: snb-*/snb-*-domain/**/*.java
---

# Domain 层开发规范

## 核心原则

- **纯 Java**：禁止依赖 Spring / JPA / Hibernate / spring-data（ArchUnit 门禁强制）
- **刻意薄**（与 patra 的差异）：本平台聚合生命周期简单，domain 只放业务规则与不变量的**纯计算**（如 `DrawEligibility`、`Campaign` 状态判断），不搞聚合根基类 / 版本锁 / 领域事件那套仪式
- record 优先；无 Lombok 需求

## 包结构（照 patra-catalog，2026-07-07 验收意见③）

- `domain/model/` — 业务规则与不变量的纯计算（`Campaign`、`DrawResult`、`DrawEligibility`）
- `domain/model/read/` — **读侧视图 record，一文件一类**（`LeaderEntry`、`PromptSummary`、`Page`…）：不属于聚合、无业务逻辑，只承载查询数据（patra Read Model 同款）
- `domain/model/enums/` — 枚举（`SortMode`）
- `domain/port/{repository,read,功能}/` — **全部端口**（纯接口），按类型分子包：`repository/`（聚合持久化 `{Entity}Repository`）、`read/`（读投影 `{Entity}ReadPort`）、`{function}/`（外部能力/领域动作 `{Function}Port`，如 `draw/`、`storage/`）。端口形状仍由用例需求决定（说「资格金额」，不说原始订单行），实现在 infra；命名细则见 tech/port-service.md
- `domain/exception/` — 业务异常

## 异常

- 业务异常继承 commons 的 `DomainException`，构造时携带 `StandardErrorTrait` 语义特征（如 `NOT_FOUND`、`CONFLICT`），由 commons 错误处理自动映射为 problem+json HTTP 响应
- 例：`CampaignNotActiveException`（404）、`NoDrawsLeftException`（409）

## 禁止行为

1. 禁止任何 Spring 注解
2. 禁止依赖 infra / adapter / 其他上下文 / `me.supernb.sub2api`
3. 禁止可观测性、日志框架代码
