---
paths: snb-boot/**/*.java, snb-boot/**/*.yml
---

# Boot 模块开发规范

## 核心职责

- 唯一 `@SpringBootApplication`（`SnbPlatformApplication`，扫描 `me.supernb` 全部上下文）；**不写业务代码**
- `application.yml` 集中环境配置；Flyway 聚合各 infra 模块的 `classpath:db/migration/{context}`

## 配置纪律

- 一切外部参数 `${ENV_VAR:默认值}` 注入，真实凭据零入库（本仓库开源）
- `spring.jpa.hibernate.ddl-auto: validate` + `open-in-view: false` **不许动**——schema 由 Flyway 全权管理，validate 只兜实体映射与库结构一致
- snb-sub2api 的能力开关（`sub2api.read-datasource.url` 等）见 tech/sub2api.md

## 守门测试住这里

- `HexagonalBoundaryTest`（ArchUnit）：六边形依赖边界的编译产物级门禁——新增依赖规则往这里加
- `{Context}WiringTest`：真装配冒烟（上下文组件在 boot 组装形态下能拉起、关键 Bean 在位）

## 禁止行为

1. 禁止硬编码配置值
2. 禁止在 boot 放 `@Service` / `@RestController` 等业务组件
