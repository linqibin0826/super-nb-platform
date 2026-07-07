---
paths: snb-*/snb-*-adapter/**/*.java
---

# Adapter 层开发规范

## 核心职责

- 入站适配：每上下文单 `@RestController`（`{Context}Controller`），路径 `/{context}/v1/*`
- 协议转换：HTTP 参数/JSON → **写操作组命令经 `CommandBus.handle()` 派发，读操作调注入的查询用例** → 响应 DTO（规范见 tech/commandbus.md）

## 包结构（照 patra-catalog）

- `adapter/rest/` — Controller
- `adapter/rest/request/` — 请求 DTO record（一文件一类；请求自有载体可嵌套，如 `CreateGenerationRequest.ImagePayload`）
- `adapter/rest/response/` — 响应 DTO record（`DrawResponse`、`DeleteResponse`）
- `adapter/web/` — 上下文级 web 横切（`RateLimitFilter`、`TokenBucket`）

## 鉴权

- 需登录的端点在方法签名声明 `@CurrentUser UserProfile user` 即完成鉴权（解析器在 snb-sub2api starter：Authorization → introspect → active 终端用户，否则 401 problem+json）
- 公开端点不加该参数，不读 Authorization 头
- `@RequestBody` 参数写在 `@CurrentUser` **前面**，保持"坏 JSON 先 400、再谈 401"的语义

## DTO

- 请求/响应 DTO 用 record 定义在 `rest/request/`、`rest/response/`，禁止透出 JPA 实体；读端点直接返回 domain/model/read 的读视图（本平台约定：读视图即对外契约的中间形态）
- 分页响应统一 `{items, total}` 形状

## 横切 Filter

- 上下文级横切（如 gallery 的 `RateLimitFilter` 令牌桶）标 `@Component`，**只作用于本上下文路径前缀**
- 🚨 绝不把限流/封禁类逻辑作用到付费模型 API 路径（那是上游 sub2api 的域）

## 错误处理

领域异常直接抛，commons 按 `StandardErrorTrait` 自动映射 problem+json。禁止 try-catch 业务异常、禁止手工拼 `ResponseEntity` 错误体。

## 禁止行为

1. 禁止业务逻辑（判断/计算归 app 或 domain）
2. 禁止直接注入 `CommandHandler` 实现——写操作只经 `CommandBus`（ArchUnit 门禁 `adapterInjectsBusNotHandlers`）
3. 禁止直接注入 `{Entity}JpaRepository` / `JdbcTemplate` / `EntityManager`——只经 app 用例
4. 禁止跨上下文依赖
