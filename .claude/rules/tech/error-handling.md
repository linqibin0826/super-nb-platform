# 异常处理规范

## 体系（复用 commons）

- 业务异常继承 `dev.linqibin.commons.error.DomainException`，构造时携带 `StandardErrorTrait` 语义特征
- commons 的错误处理自动把 trait 映射为 RFC 9457 problem+json 响应（`NOT_FOUND`→404、`CONFLICT`→409、`UNAUTHORIZED`→401…）
- 数据层通用异常由 `commons-starter-jpa` 的 `JpaErrorMappingContributor` 统一兜底

## 各层职责

| 层 | 规则 |
|----|------|
| domain | 定义业务异常 + trait（如 `NoDrawsLeftException`、`CampaignNotActiveException`） |
| app | 领域异常**直接传播**，不包装不吞 |
| infra | 只处理有补偿语义的技术异常（如撞 PK 的幂等回读），其余上抛 |
| adapter | 不 try-catch 业务异常，不手工拼错误体 |

## 平台级异常

- 401 统一用 `snb-common` 的 `UnauthorizedException`（`@CurrentUser` 解析失败即抛它）

## 禁止行为

1. 禁止吞异常返回 null / 空集合来"容错"（优雅降级必须是显式的用例语义，如"无进行中活动 → 空榜"）
2. 禁止用 `ResponseEntity` 手工构造错误响应
3. 禁止自建错误码体系——语义特征（trait）就是错误分类
