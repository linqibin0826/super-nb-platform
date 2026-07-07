---
paths: snb-*/snb-*-adapter/**/*.java
---

# Adapter 层开发规范

## 核心职责

- 入站适配：每上下文单 `@RestController`（`{Context}Controller`），路径 `/{context}/v1/*`
- 协议转换：HTTP 参数/JSON → app 用例调用 → 响应 DTO

## 鉴权

- 需登录的端点在方法签名声明 `@CurrentUser UserProfile user` 即完成鉴权（解析器在 snb-sub2api starter：Authorization → introspect → active 终端用户，否则 401 problem+json）
- 公开端点不加该参数，不读 Authorization 头
- `@RequestBody` 参数写在 `@CurrentUser` **前面**，保持"坏 JSON 先 400、再谈 401"的语义

## DTO

- 请求/响应 DTO 用 record 定义在 adapter 模块，禁止透出领域对象 / JPA 实体
- 分页响应统一 `{items, total}` 形状

## 横切 Filter

- 上下文级横切（如 gallery 的 `RateLimitFilter` 令牌桶）标 `@Component`，**只作用于本上下文路径前缀**
- 🚨 绝不把限流/封禁类逻辑作用到付费模型 API 路径（那是上游 sub2api 的域）

## 错误处理

领域异常直接抛，commons 按 `StandardErrorTrait` 自动映射 problem+json。禁止 try-catch 业务异常、禁止手工拼 `ResponseEntity` 错误体。

## 禁止行为

1. 禁止业务逻辑（判断/计算归 app 或 domain）
2. 禁止直接注入 `{Entity}JpaRepository` / `JdbcTemplate` / `EntityManager`——只经 app 用例
3. 禁止跨上下文依赖
