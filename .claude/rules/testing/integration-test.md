---
paths: snb-*/snb-*-infra/**/src/test/**/*.java, snb-boot/**/src/test/**/*.java, snb-sub2api/**/src/test/**/*.java
---

# 集成测试规范（infra / snb-sub2api / boot）

## 硬性要求

1. **真实中间件**：Testcontainers PG（`postgres:16-alpine`）+ 真 Flyway 迁移，**禁止内存数据库**——infra 测试同时验证迁移脚本与实体映射一致（`ddl-auto=validate` 在测试里就会炸出漂移）
2. `@Timeout` ≤ 30s；并发/锁类测试**必须**标注（锁 bug 的表现就是挂死）
3. mock 注入用 `@MockitoBean`（`@MockBean` 已移除）
4. 测试内准备数据允许直接用 `JdbcTemplate`（生产代码禁止，见 layers/adapter.md）；清场用 `TRUNCATE ...`。⚠️ 雪花基座后无数据库自增：**造数 INSERT 必须显式给 id**（审计列有 DDL DEFAULT 兜底不用给）

## infra 测试样板（TestApp 模式）

测试源里放最小装配类（**不叫 `*Test` 结尾**，命名 `{Context}InfraTestApp`，与被测适配器同包，如 `infra/adapter/persistence/`），只挂被测适配器 + 假端口：

```java
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(DrawAdapter.class)
class ActivityInfraTestApp {
    @Bean RechargeReadPort fixedRecharge() { /* 固定假数据 */ }
}
```

测试类接 Testcontainers（`@DynamicPropertySource` 按上下文指 Flyway）：

```java
@SpringBootTest(classes = ActivityInfraTestApp.class)
@Testcontainers
class DrawAdapterConcurrencyTest {

    @Container
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", PG::getJdbcUrl);
        r.add("spring.datasource.username", PG::getUsername);
        r.add("spring.datasource.password", PG::getPassword);
        r.add("spring.flyway.locations", () -> "classpath:db/migration/activity");
        r.add("spring.flyway.schemas", () -> "activity");
    }
}
```

## 并发语义测试模式（关键业务 100% 覆盖的主力）

`ExecutorService` + `CountDownLatch` 同放闸，断言**不变量**而非时序：发放总数 = min(应得, 池容量)、超额一律业务异常、计数最终一致。样板 `DrawAdapterConcurrencyTest`（advisory lock 不超发）、`GalleryRepositoriesTest`（并发双删只计一次）。

## snb-sub2api starter 测试

- **条件装配**用 `ApplicationContextRunner`：introspect 常开；`RechargeReadModel` 不配 `sub2api.read-datasource.url` 不装、配了就装（样板 `Sub2apiAutoConfigurationTest`）
- 读模型 SQL 用 Testcontainers **自建上游表结构**钉住对 sub2api schema 的假设（样板 `JdbcRechargeReadModelTest`）——上游 fork 合并改表时这里先红

## boot 守门测试

- `{Context}WiringTest`：`@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers，真组装冒烟——公开端点可达、未带 token 的登录端点经 commons 错误处理回 401、**每上下文至少一条带 token 的写请求真经 CommandBus 派发到 Handler**（bus 路由表是运行期按泛型构建的，必须真跑一次；空库按契约 404 即证明全链通）；sub2api 只读源可指同一容器（仅需能连上）；mock 的 introspect 未 stub 时返回 Optional.empty，401 路径天然成立
- `HexagonalBoundaryTest`（ArchUnit）：依赖边界的编译产物级门禁，新增依赖规则往这里加
