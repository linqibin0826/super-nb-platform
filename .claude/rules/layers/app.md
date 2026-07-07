---
paths: snb-*/snb-*-app/**/*.java
---

# App 层开发规范

## 核心职责

- 用例编排：校验 → 调端口 → 组装 DTO
- **驱动端口定义**：本上下文需要的外部能力全部在这里定义接口（与 patra 的差异——patra 端口在 domain）。端口按用例需求定形状（如 activity 的 `RechargeQueryPort` 说"资格金额"，不说原始订单行），实现放 infra

## 包与命名

- 平包 `me.supernb.{context}.app`，无子包仪式
- 用例两式，**新代码跟随所在上下文的既有风格**：
  - 动作型用例类 `{Action}{...}UseCase`（activity 风格：`PerformDrawUseCase`、`GetPoolUseCase`）
  - 内聚服务复数名词（gallery 风格：`PromptQueries`、`Interactions`、`Generations`）
- 端口：外部能力 `{Thing}Port`（`PoolPort`、`ImageStoragePort`）；持久化 `{Entity}Repository`（`PromptRepository`）
- DTO：聚在 `{Context}Dto` 容器类里，全部 record
- Bean 注解：`@Service`

## 事务（与 patra 的差异）

**app 层无事务注解**。事务边界在 infra 端口实现内用 `TransactionTemplate` 显式管理（避 `@Transactional` 自调用代理坑），并发不变量（advisory lock、SKIP LOCKED）由 infra 实现保证、app 只依赖端口语义。禁止在 app 使用 `@Transactional`。

## 禁止行为

1. 禁止依赖 infra / adapter
2. 禁止感知持久化技术：`jakarta.persistence` / `org.hibernate` / `org.springframework.data` 一律不许 import（ArchUnit 门禁 `appIsPersistenceFree`）
3. 禁止依赖 `me.supernb.sub2api`（防腐层类型只进 infra/adapter；app 用自己的端口）
4. 禁止跨上下文依赖
