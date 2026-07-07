---
paths: snb-sub2api/**/*.java, snb-*/snb-*-infra/**/*.java, snb-*/snb-*-adapter/**/*.java
---

# snb-sub2api 防腐层规范

sub2api 是外部上游（主站开源 fork）。`snb-sub2api` 是**唯一**知道 sub2api 细节（HTTP 接口形状 + 库表结构）的模块——上游变化时只修这一个模块。

## 形态 = 防腐层 starter（非领域、非普通 jar）

- `@AutoConfiguration` + `META-INF/spring/...AutoConfiguration.imports` **自注册**，不依赖宿主组件扫描（boot 的默认扫描经 `AutoConfigurationExcludeFilter` 自动跳过 imports 类，同包无双注册）
- 包按能力组织：`auth/`（introspect 客户端 + `@CurrentUser`）、`recharge/`（充值只读读模型）、将来 `admin/`（写客户端）
- 能力各自条件装配：introspect 常开；读模型 `@ConditionalOnProperty("sub2api.read-datasource.url")` 不配不装；web 解析器 `@ConditionalOnWebApplication`。全部 `@ConditionalOnMissingBean` 可覆盖

## 交互设计准则（站长定案 2026-07-07）

| 交互形态 | 技术归宿 |
|---|---|
| **读** sub2api 库（只读投影/聚合） | starter 里的 **JdbcTemplate 读模型能力包**（外部 schema + 无实体生命周期，不上 JPA / 第二持久化单元；显式 SQL 收敛在单文件、测试自建上游表结构钉住假设） |
| **写** sub2api（发码/建 Key/建号） | starter 里的 **API 客户端能力包**（写一律走 sub2api HTTP 接口，**永不直写库**；`@ConditionalOnProperty(admin-key)` 不配不装） |
| 围绕上游长出**自有业务过程/不变量**（如账号池） | **新业务 context**（标准四模块），其 infra 消费 starter 的客户端 |

一句话：「怎么跟 sub2api 说话」永远归 starter，「我们自己的业务」才立 context。单个能力的实现可随时单独换（消费方只看接口），不是单向门。

## 纪律（防垃圾场）

1. 公开面按真实需求逐个添加，禁止「顺手」透出整表/整接口
2. 能走 sub2api HTTP 接口的优先走接口；直读库仅限没有接口可用的聚合场景
3. 敏感列（如 `redeem_code`、`claimed_by`）绝不透出；邮箱等脱敏在 starter 层内完成

## ⚠️ 工程红线：Bean 暴露

**绝不把只读 `DataSource` / `JdbcTemplate` 暴露为 Spring Bean**——会把 Boot 主 DataSource/jdbcTemplate 自动配置挤退（`@ConditionalOnMissingBean(JdbcOperations)` 背锅），全应用注入错库。在 `@Bean` 方法**内部**构建并直接喂给读模型；第二个读模型出现时收敛为 starter 内部共享的自有 holder 类型（或标 `defaultCandidate = false`），仍不暴露原生类型。

## 消费方式（各上下文怎么用）

- **controller 鉴权**：方法参数 `@CurrentUser UserProfile user`（Authorization → introspect → active 的 user/admin 账号 → 否则 401）
- **infra 薄适配**：注入 starter 读模型/客户端，把上游 DTO 映射为本上下文读视图（样板：`RechargeReadAdapter`）
- **ArchUnit 门禁**：`me.supernb.sub2api` 类型只进 infra/adapter，禁入 domain/app
