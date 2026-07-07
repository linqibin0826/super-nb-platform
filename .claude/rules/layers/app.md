---
paths: snb-*/snb-*-app/**/*.java
---

# App 层开发规范

## 核心职责

- 用例编排：校验 → 调端口（定义在 domain/port，见 layers/domain.md）→ 组装读视图/写结果

## 包结构（照 patra-catalog，按子域分包）

- `app/usecase/{子域}/` — 写处理器 `{Action}{Entity}Handler`（实现 `CommandHandler<C,R>`，如 `PerformDrawHandler`、`CreateGenerationHandler`）
- `app/usecase/{子域}/command/` — 命令 `{Action}{Entity}Command`（record，实现 `Command<R>`）+ 命令自有载体（`ImageBytes`、`RefBytes`）
- `app/usecase/{子域}/dto/` — **写结果** record（`LikeResult`、`FavResult`、`Created`）
- `app/usecase/{子域}/query/` — 查询用例，统一命名 `{View}QueryService`（`PoolQueryService`、`MyDrawsQueryService`、`PromptQueryService`），无接口、被 controller 直接注入（细则见 tech/port-service.md）
- 现有子域：activity = `draw`、`campaign`；gallery = `prompt`、`interaction`、`generation`
- **读视图 record 在 domain/model/read**（不在 app）；app 没有 DTO 容器类
- 写经 CommandBus、读被 controller 直接注入（规范见 tech/commandbus.md）
- Bean 注解：`@Service`（Handler 与查询用例同）

## 事务（与 patra 的差异）

**app 层无事务注解**。事务边界在 infra 端口实现内用 `TransactionTemplate` 显式管理（避 `@Transactional` 自调用代理坑），并发不变量（advisory lock、SKIP LOCKED）由 infra 实现保证、app 只依赖端口语义。禁止在 app 使用 `@Transactional`。

## 禁止行为

1. 禁止依赖 infra / adapter
2. 禁止感知持久化技术：`jakarta.persistence` / `org.hibernate` / `org.springframework.data` 一律不许 import（ArchUnit 门禁 `appIsPersistenceFree`）
3. 禁止依赖 `me.supernb.sub2api`（防腐层类型只进 infra/adapter；app 用自己的端口）
4. 禁止跨上下文依赖
