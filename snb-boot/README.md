# snb-boot

全仓库唯一的组装点(composition root)。不写业务代码——没有 `@Service`、没有 `@RestController`,只做三件事:装配唯一的 Spring Boot 应用入口、聚合双上下文的 Flyway 迁移历史、把 snb-sub2api 的鉴权结果接给 JPA 审计基座。

## 包结构

```
me.supernb
├── SnbPlatformApplication   唯一 @SpringBootApplication,启动入口
└── config/                  横切 @Configuration(目前只有 CurrentUserAuditorConfig)
```

测试树按职责分两处:`src/test/java/me/supernb/boot/` 放守门测试(`HexagonalBoundaryTest`、各上下文 `WiringTest`),`src/test/java/me/supernb/config/` 放 `CurrentUserAuditorConfig` 自身的单测。

## 核心设计

### 唯一的组装点

`build.gradle.kts` 里,`snb-boot` 是全仓库唯一同时依赖 `snb-activity-{adapter,infra,api}`、`snb-gallery-{adapter,infra,api}`、`snb-common`、`snb-sub2api` 的模块。这个"同时"是关键:每个上下文内部,`adapter` 和 `infra` 在 Gradle 依赖图上是两条平行分支,各自只经 `app` 传递依赖 `domain`,彼此之间没有一行编译期耦合——一行代码也不会从 adapter import infra,反过来也一样。

只有在 snb-boot,`adapter` 的 Bean(Controller)和 `infra` 的 Bean(端口实现)才第一次被扫描进同一个 `ApplicationContext`:adapter 通过 `CommandBus`/`QueryService` 接口找到 app 层的 Bean,app 层通过 domain 定义的端口接口找到 infra 层的实现——全程没有一处编译期的 adapter↔infra 耦合,连接完全靠 Spring 运行期按接口类型做的依赖注入。这意味着理论上可以把 infra 换成任何其他实现,只要新实现挂同一套 domain/port 接口,adapter 与 app 不用动一行。

`SnbPlatformApplication` 用默认扫描(扫描根即自身所在包 `me.supernb`),天然覆盖 activity/gallery 两个上下文的全部组件,不需要额外声明扫描路径——新上下文接入时也不用在这里加一行配置,前提是包名挂在 `me.supernb` 下。

### Flyway:单一版本历史聚合双 schema

数据库层面是单个 PostgreSQL 库 `snb`,两个 schema(`activity`、`gallery`)。Flyway 只有**一份**版本历史,不是每个上下文各自一份——`flyway_schema_history` 是单一一张表,版本号是全局唯一序列(`V1` 已用于 activity 基线,`V2` 已用于 gallery 基线,下一次不管改哪个上下文,新迁移都要接着编 `V3`)。如果两个上下文都发布 `V1`,Flyway 会认为其中一个已经跑过而跳过它,这是踩过的坑,不是随口的规矩。

迁移脚本物理上仍然分别放在各自 infra 模块的 `resources/db/migration/{context}/` 下,保持"一个上下文的迁移归它自己的模块"的组织直觉;`application.yml` 通过 `spring.flyway.locations` 配两个 classpath 路径(`classpath:db/migration/activity,classpath:db/migration/gallery`)+ `schemas: activity,gallery` + `create-schemas: true`,在运行期把它们拼成一条历史一起跑。

`spring.jpa.hibernate.ddl-auto` 钉死 `validate`(不是 `update`,不是 `none`),配 `open-in-view: false`。schema 的唯一真源是 Flyway 迁移脚本,JPA 实体注解不重复声明索引/唯一约束这些 DDL 层面的东西。`validate` 不负责建表,只负责启动时拿实体的 O/R 映射去跟数据库当前结构比对——一旦迁移脚本与实体字段/类型/列名出现漂移,启动直接报错。这条防线的价值在于"漂移在启动那一刻就炸,而不是等到某个具体查询触发才发现列对不上"。

### auditorAware:鉴权结果与 JPA 审计基座的唯一交汇点

`CurrentUserAuditorConfig` 是 [snb-sub2api](../snb-sub2api/README.md) 鉴权结果与 commons-starter-jpa 审计基座之间唯一的交汇点——两者互相可见的地方只有这个组装点:snb-sub2api 本身不依赖 JPA(它的读模型故意只用 JdbcTemplate),看不见 `AuditorAware` 这套机制;单个限界上下文的 infra 虽然能看见 JPA 审计基座,却看不见跨上下文的鉴权类型。两个条件只在 snb-boot 同时满足,这个配置类因此既不属于任何一个限界上下文,也不能下沉到 snb-sub2api。

行为很直接:从当前请求属性(`CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE`)读已鉴权的 `UserProfile`,有则给出其 `id` 作为审计操作人;无请求上下文(迁移脚本、定时任务、infra 测试)或匿名请求一律返回 `Optional.empty()`,此时 `created_by`/`updated_by` 审计列留 NULL。

⚠️ **Bean 名必须叫 `auditorAware`**——commons-starter-jpa 的 `@EnableJpaAuditing(auditorAwareRef = "auditorAware")` 按名字取这个 Bean,改名不是"装配了但没生效"这种能事后从行为上发现的软失败,是容器直接起不来。

### 守门测试

两类测试都只能住在 snb-boot,因为都需要全部模块编译完、组装成一个真实的 `ApplicationContext` 才能跑:

- **`HexagonalBoundaryTest`**(ArchUnit):六边形依赖边界的编译产物级门禁。当前钉住 domain 不依赖 app/infra/adapter/Spring/JPA/Hibernate、app 不依赖 infra/adapter、app 不感知持久化技术、activity 不依赖 gallery(上下文隔离)、`me.supernb.sub2api` 类型只进 infra/adapter、adapter 只能注入 `CommandBus` 不能直接依赖 `CommandHandler` 实现、api 模块不依赖任何实现层(空壳期靠 `allowEmptyShould(true)` 空转)。这是 Gradle 编译期隔离(`build-logic` 的 convention plugin + `domain` 模块的 `enforceDomainPurity` 任务)之外的第二道防线——挡的是"编译能过但设计上不允许"的写法。新增依赖规则都加在这里。
- **`{Context}WiringTest`**(`ActivityWiringTest`、`GalleryWiringTest`):`@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers 真 PG 的全栈组装冒烟。验证三件事——公开端点可达、未带 token 的写请求经 commons 错误处理回 401、至少一条带 token 的写请求真实穿过 `CommandBus` 派发到 Handler。最后一条不是形式主义:`CommandBus` 的路由表是运行期按 `Command` 的泛型参数动态构建的,不是编译期生成,"路由表接对了"这件事本身需要一次真实的端到端派发去验证,静态类型检查证明不了。两个 `WiringTest` 各自只测自己上下文的端点,但因为 `@SpringBootTest` 装配的是整个单体,连 `ActivityWiringTest` 也要用 `@MockitoBean` 打桩 gallery 的 `ImageStoragePort`(R2 存储端口)——这本身就是"boot 是唯一同时组装两个上下文的地方"这句话最直接的证据。

### 配置纪律

`application.yml` 集中管理环境配置,一切外部参数走 `${ENV_VAR:默认值}` 形式注入,真实凭据零入库(仓库将开源)。各上下文/starter 的配置项(`sub2api.*`、`gallery.ratelimit.*` 等)也统一落在这一份文件里——装配类只负责消费,不负责定义顶层配置文件结构。
