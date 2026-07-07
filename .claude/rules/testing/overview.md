---
paths: "**/src/test/**/*.java"
---

# 测试策略总纲

TDD 强制（Red-Green-Refactor）。**完成定义 = `./gradlew build` 全绿**（编译 + 全部测试 + ArchUnit 门禁）。

## 分层策略

| 层 | 形态 | 细则 |
|----|------|------|
| domain / app / snb-common | 纯单测，零 Spring，mock 端口 | [unit-test.md](unit-test.md) |
| infra / snb-sub2api / boot | Testcontainers 真 PG + 真 Flyway | [integration-test.md](integration-test.md) |
| adapter | standalone MockMvc 契约测试 | [adapter-test.md](adapter-test.md) |

**与 patra 的差异**：单一 `src/test` 源集，命名一律 `*Test.java`（不切 unitTest/integrationTest/e2eTest 源集、无 `*IT`/`*E2E` 后缀——单体规模不值得）。真 E2E（compose 全栈 + playwright 走真流程）属割接验收活动，由私有运维仓库组织，不在本仓测试套件。

## 测试风格（本仓既定，与 patra 不同处以本文为准）

- **方法名即规格**：英文驼峰句子（`delegatesToDrawPort`、`propagatesNoDrawsLeft`、`likeRequiresTokenAndReturnsCount`），不用 `@DisplayName`，不写 given/when/then 注释——方法短到不需要
- 类级 `///` 中文说明测试意图与关键约束（一两句，含并发/降级语义）
- 断言用 **AssertJ**（`assertThat` / `assertThatThrownBy`），mock 用 **Mockito**（java-base 已全模块内置，无需加依赖）
- 一个测试一个行为；公共夹具用字段初始化，少用 `@BeforeEach` 仪式

## 超时纪律

- 纯单测 `@Timeout` ≤ 2s；容器类测试 ≤ 30s（并发/锁类测试**必须**标注——锁 bug 的表现就是挂死）
- 存量测试未标注，**不做一次性全量回填**；新增或实质改动某测试类时顺手补齐

## 比例与覆盖口径（照 patra，本仓暂无 jacoco 门禁，作 TDD 纪律与评审口径）

- 单元测试 ≥ 75%；容器集成 + 契约测试合计 ~25%
- 行覆盖 ≥ 80%，分支覆盖 ≥ 70%；**关键业务 100%**（并发发奖、计数增减、鉴权、幂等写入）
- 排除口径：DTO record、配置类、启动类

## Spring Boot 4 注意

- `@MockitoBean`（`@MockBean` 已移除；包 `org.springframework.test.context.bean.override.mockito`）
- Jackson 3（`tools.jackson`，非 `com.fasterxml`）
- `@AutoConfigureMockMvc` 在 `org.springframework.boot.webmvc.test.autoconfigure`（模块 `spring-boot-webmvc-test`）

## 必测清单（本项目专属坑）

1. **动态拼接的 HQL 每个分支**都要遍历（启动期不校验，见 tech/jpa.md）
2. **starter 条件装配**用 `ApplicationContextRunner` 验「不配不装、配了就装」
3. 优雅降级语义（无进行中活动 → 空）是用例契约，必须有测试钉住（样板 `GracefulDegradationTest`）
4. 并发语义（SKIP LOCKED、advisory lock、并发双删、撞 PK 重试）用真 PG 多线程测，禁止只测单线程 happy path
5. 外部端口（R2、缩略图）在单测里 mock；任何测试不真调外部服务
