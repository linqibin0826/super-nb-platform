---
paths: snb-*/snb-*-domain/**/*.java
---

# Domain 层开发规范

## 核心原则

- **纯 Java**：禁止依赖 Spring / JPA / Hibernate / spring-data（ArchUnit 门禁强制）
- **刻意薄**（与 patra 的差异）：本平台聚合生命周期简单，domain 只放业务规则与不变量的**纯计算**（如 `DrawEligibility`、`Campaign` 状态判断、`SortMode`），不搞聚合根基类 / 版本锁 / 领域事件那套仪式
- record 优先；无 Lombok 需求

## 端口位置（与 patra 的差异）

patra 端口定义在 domain；**本平台端口定义在 app 层**——domain 薄，端口形状由用例需求决定（见 layers/app.md）。domain 不定义接口。

## 异常

- 业务异常继承 commons 的 `DomainException`，构造时携带 `StandardErrorTrait` 语义特征（如 `NOT_FOUND`、`CONFLICT`），由 commons 错误处理自动映射为 problem+json HTTP 响应
- 例：`CampaignNotActiveException`（404）、`NoDrawsLeftException`（409）

## 禁止行为

1. 禁止任何 Spring 注解
2. 禁止依赖 infra / adapter / 其他上下文 / `me.supernb.sub2api`
3. 禁止可观测性、日志框架代码
