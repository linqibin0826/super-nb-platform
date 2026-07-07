---
paths: "**/src/test/**/*.java"
---

# 测试策略

TDD 强制（Red-Green-Refactor）。单一 `src/test` 源集，命名一律 `*Test.java`（与 patra 的差异：单体规模不切 IT/E2E 源集）。**完成定义 = `./gradlew build` 全绿**。

## 分层策略

| 层 | 形态 | 要点 |
|----|------|------|
| domain | 纯单测，零 Spring | 最快，覆盖规则分支 |
| app | 单测 + Mockito mock 端口 | 验编排/降级语义，不碰库 |
| infra | `@SpringBootTest(classes = XxxInfraTestApp.class)` + **Testcontainers 真 PG + 真 Flyway** | 验映射、SQL、并发语义 |
| adapter | standalone MockMvc | 验路由、JSON 契约、鉴权语义 |
| boot | WiringTest + ArchUnit | 真装配冒烟 + 依赖边界门禁 |

## infra 测试样板（TestApp 模式）

测试源里放最小装配类，`@DynamicPropertySource` 指向 Testcontainers：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(DrawAdapter.class)          // 只挂被测适配器
class ActivityInfraTestApp {
    @Bean RechargeQueryPort fakeRecharge() { /* 固定假数据 */ }
}
```

- 真实中间件（Testcontainers PG），禁止内存数据库
- Flyway 跑真迁移脚本——infra 测试同时验证迁移与实体映射一致
- 并发语义（SKIP LOCKED、advisory lock、并发双删）用真 PG 多线程测

## adapter 测试样板

```java
mvc = MockMvcBuilders.standaloneSetup(new GalleryController(...))
        .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
        .build();
```

带 `@CurrentUser` 的端点：mock `Sub2apiIntrospectClient` 给有效/无效两路。

## Spring Boot 4 注意

- `@MockitoBean`（`@MockBean` 已移除）
- Jackson 3（`tools.jackson`，非 `com.fasterxml`）
- `@AutoConfigureMockMvc` 在 `org.springframework.boot.webmvc.test.autoconfigure`（模块 `spring-boot-webmvc-test`）

## 必测清单（本项目专属坑）

1. **动态拼接的 HQL 每个分支**都要遍历（启动期不校验，见 tech/jpa.md）
2. **starter 条件装配**用 `ApplicationContextRunner` 验"不配不装、配了就装"
3. 优雅降级语义（无进行中活动 → 空）是用例契约，必须有测试钉住
4. 外部端口（R2、缩略图）在 app/infra 测试里 mock；不真调外部服务
