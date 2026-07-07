# super-nb-platform 架构文档

super-nb-platform 是 super-nb 中转站生意的自写后端统一平台:一个部署单元(单体),内部按限界上下文(bounded context)与六边形架构(hexagonal / ports-and-adapters)分层。这篇文档面向第一次读这个仓库的 Java 工程师,把只写在 `.claude/rules/`(给 Claude Code 用的机器可读开发规范)里的设计决策和"为什么"讲成人话。想查某条规则的精确措辞去读 `.claude/rules/`;想搞清楚这套架构是怎么想出来的、取舍在哪、坑在哪,读这篇。

## 1. 定位与两个限界上下文

这个仓库收编了此前散装的两个自写业务服务,统一成一个 Java 后端。未来新的自写后端业务也在这里长新的限界上下文,不再起新服务——"统一平台"这个名字是字面意思。

技术栈:Java 25、Spring Boot 4(4.0.6)、Spring Data JPA(Hibernate 7)+ Flyway、PostgreSQL、AWS SDK v2(R2 对象存储走 S3 协议)、Gradle 9.5(Kotlin DSL + 自定义 convention plugin)。

当前两个限界上下文:

- **activity**(活动中心):抽奖、充值榜、奖池实况。技术上的核心难点是并发正确性——同一时刻大量用户抢一个容量有限的奖池,不能超发,也不能因为加锁过粗把不同用户的请求串成队列。
- **gallery**(灵感库):公开的提示词库(浏览/点赞/收藏)、登录用户的 AI 生成历史(图片存 R2、下发 presigned URL、列表用 256px 缩略图省流量)、匿名/登录混合场景下的令牌桶限流。

架构血统:照 [patra](https://github.com/linqibin0826/patra)(同一作者的另一个项目)的架构,复用其 `linqibin-commons` 基建——CQRS 总线、JPA 审计基座、统一错误处理这些横切能力不在本仓重新发明。但 patra 是微服务形态(每个上下文独立部署、各有自己的 api/boot),本仓库是单体。同一套六边形/DDD 骨架套在不同的部署形态上必然要做取舍,这些取舍贯穿全文,第 11 节集中总结。

`linqibin-commons` 不在 Maven Central,`scripts/bootstrap-commons.sh` 按 `gradle.properties` 里 `patraRef` 钉的 commit,从公开 patra 仓库现场 build 出来发布到 mavenLocal——这是 CI 和本地开发都要先跑一次的前置步骤,不是可选项。

## 2. 六边形分层与依赖方向

标准 ports-and-adapters,每个上下文五个模块:

| 层 | 职责 | 能否感知技术细节 |
|---|---|---|
| `domain` | 业务规则与不变量的纯计算;定义端口(接口)但不实现 | 零框架依赖,连 Spring 注解都不许有 |
| `app` | 用例编排:校验 → 调 domain 定义的端口 → 组装结果 | 不感知持久化技术(JPA/Hibernate/Spring Data 一律不许 import) |
| `infra` | 实现 domain 端口:JPA 持久化、R2、跨上下文防腐层薄适配 | 技术细节全在这层 |
| `adapter` | 入站适配:REST Controller,HTTP ↔ 命令/查询 | 只做协议转换,不许有业务判断 |
| `api` | 上下文对外契约(当前空壳,见第 2.3 节) | 不依赖任何实现层 |

依赖方向单向、不可逆:

```
        ┌───────────────────────────────────────────┐
        │                  adapter                   │  入站:REST Controller / Filter
        └───────────────────────┬─────────────────────┘
                                 │ 写:CommandBus.handle(cmd)
                                 │ 读:直接注入 QueryService
                                 ▼
        ┌───────────────────────────────────────────┐
        │                    app                     │  用例编排:Handler / QueryService
        └───────────────────────┬─────────────────────┘
                                 │ 调用 domain/port 定义的接口
                                 ▼
        ┌───────────────────────────────────────────┐
        │                  domain                    │  纯 Java:model / port(接口)/ exception
        └───────────────────────▲─────────────────────┘
                                 │ 实现 domain/port 接口
        ┌───────────────────────┴─────────────────────┐
        │                   infra                     │  出站:JPA 持久化 / R2 / sub2api 薄适配
        └───────────────────────────────────────────┘
```

adapter 与 infra 都指向 domain(前者经 app 间接依赖,后者直接实现端口),但两者互相之间没有任何依赖——它们是两条独立的分支,只在运行期由 Spring 通过 domain 定义的端口接口连起来。这个"编译期不相连、运行期靠 DI 接起来"的结构在第 3 节展开。

### 2.1 两道强制机制

依赖方向不是君子协定,有两层强制:

1. **Gradle 编译期隔离**:`build-logic` 的 convention plugin(`snb.hexagonal-{domain,app,infra,adapter,api,boot}`)锁死每层能声明的依赖坐标。`domain` 只挂 `commons-core`(纯 Java,无框架);`domain` 模块还额外挂了一个自定义 Gradle 任务 `enforceDomainPurity`,扫描已解析的编译/运行时 classpath,一旦出现 `org.springframework`、`jakarta.persistence`、`org.hibernate` 等禁用 group 直接编译失败——这比"没声明依赖所以传不进来"更进一步,防的是有人在其他模块里意外把框架类型带进 domain 的间接路径。
2. **编译产物级 ArchUnit 门禁**(`HexagonalBoundaryTest`,住在 `snb-boot`):在全部模块都编译完、打成一个 ApplicationContext 之后再检查一遍依赖方向。这一层挡的是"Gradle 挡不住,但设计上不允许"的情况——比如 app 模块技术上可以在不加任何新依赖的情况下调用某个已经在 classpath 上的 Handler 实现,ArchUnit 负责把这类"能编译但违反分层意图"的写法钉死。

两层配合是纵深防御:Gradle 挡"编译都通不过",ArchUnit 挡"编译能过但设计上不允许"。`HexagonalBoundaryTest` 当前钉了这几条:

| 规则 | 内容 | 备注 |
|---|---|---|
| `domainIsFrameworkFreeAndInward` | domain 不依赖 app/infra/adapter/Spring/JPA/Hibernate | 与 Gradle 层的 `enforceDomainPurity` 互为补充 |
| `appDoesNotDependOnInfraOrAdapter` | app 不依赖 infra/adapter | |
| `appIsPersistenceFree` | app 不依赖 jakarta.persistence/hibernate/spring-data | app 模块本身没这些依赖,这条防的是"以后有人手贱加了依赖还写出这种代码" |
| `domainDoesNotDependOnOtherContexts` | activity 包不依赖 gallery 包 | 见下节,注意这条只测了单向 |
| `aclStaysOutOfDomainAndApp` | `me.supernb.sub2api` 类型只能出现在 infra/adapter | 见第 7 节 |
| `adapterInjectsBusNotHandlers` | adapter 不依赖任何 `CommandHandler` 的实现类 | 见第 6 节 |
| `apiIsContractOnly` | api 模块不依赖 domain/app/infra/adapter | 当前 `allowEmptyShould(true)` 空转(api 模块目前只有 package-info),契约类一出现自动生效 |

### 2.2 上下文隔离:一个容易忽略的细节

`domainDoesNotDependOnOtherContexts` 这条规则名字里带"domain",但它的包匹配式是 `resideInAPackage("..activity..")`——这个通配符圈住的是包名含有 `.activity.` 的**全部**类,横跨 domain/app/infra/adapter 四层,不只是 domain 层。换句话说这条规则实际做的是整个上下文级别的隔离,不是字面意思的"domain 层隔离"。

另外这条规则目前只测了一个方向:activity 不依赖 gallery。反方向(gallery 不依赖 activity)没有镜像的规则去测。现状下这不构成风险(两个上下文互不调用,两边 api 模块都是空壳),但如果之后先长出 gallery → activity 的调用,新增门禁的时候记得两个方向都要覆盖,不能假设这条规则已经是双向的。

### 2.3 跨上下文调用:api 模块与预留路径

单体内没有网络调用,但"上下文之间不能直接依赖对方的 domain/app"这条规矩照抄自 patra(微服务形态下这是物理隔离,单体里退化成编译期隔离)。跨上下文调用的路径已经预留好、但目前零使用:

- 被调方把对外契约(DTO、接口定义)放进自己的 `api` 模块。
- 调用方的 `infra` 依赖被调方的 `api` 模块做薄适配;调用方自己的 `domain`/`app` 定义端口,不直接感知被调方的存在——跟第 7 节 sub2api 防腐层是同一个模式,只是被调方从外部系统换成了另一个内部上下文。

当前 `snb-activity-api`、`snb-gallery-api` 都只有一个 package-info,没有任何契约类型,`apiIsContractOnly` 靠 `allowEmptyShould(true)` 空转。第一笔真实的跨上下文调用出现时,这条门禁自动从"没东西可测"变成"真的在把关"。

## 3. 模块依赖图(Gradle 视角)

上一节的分层图是概念模型,这一节是 `build.gradle.kts` 里实际写的依赖坐标——这张图能解释很多新读者会疑惑的问题,比如"adapter 怎么拿到 domain 类型的,它明明没声明依赖 domain"。以 activity 为例(gallery 对称):

```
commons-core(dev.linqibin.commons,纯 Java,来自 patra)
        │ api
        ▼
snb-activity-domain(唯一依赖是 commons-core,零其他)
        │ api
        ▼
snb-activity-app(用例编排;引入 spring-tx/spring-context 仅为了 @Service Bean 装配)
        │ api                                    │ api
        ▼                                        ▼
snb-activity-adapter                        snb-activity-infra
(+ snb-common, snb-sub2api)                 (+ snb-sub2api, commons-starter-jpa)
        │                                        │
        └───────────────────┬────────────────────┘
                             ▼
                         snb-boot
        implementation 同时依赖 adapter + infra + api(+ snb-common + snb-sub2api)
```

关键点:`app` 用 `api(project(":...domain"))` 声明依赖(不是 `implementation`),这样依赖它的模块能传递拿到 domain 类型。`adapter` 和 `infra` 各自独立用 `api(project(":...app"))`,都从 app 传递拿到 domain——但它们两个在 Gradle 图上是**平行的两条分支,互相之间没有任何依赖声明**,一行代码也不会从 adapter import infra,反过来也一样。

`snb-boot` 是全仓库唯一同时依赖 `adapter` + `infra` + `api` 的模块,是唯一的组装点(composition root)。它把 adapter 的 Bean(Controller)和 infra 的 Bean(端口实现)一起扫描进同一个 `ApplicationContext`:adapter 通过 `CommandBus`/`QueryService` 接口找到 app 层的 Bean,app 层通过 domain 定义的端口接口找到 infra 层的实现——全程没有一处编译期的 adapter↔infra 耦合,连接完全靠 Spring 运行期按接口类型做的依赖注入。这意味着理论上可以把 infra 换成任何其他实现(比如换存储引擎),只要新实现挂同一套 domain/port 接口,adapter 与 app 不用动一行。

`snb-common`(纯 web 横切,目前只有一个 `UnauthorizedException`)和 `snb-sub2api`(防腐层 starter,第 7 节详述)是两个跨上下文共享的独立模块,不属于任何一个限界上下文,`adapter` 和 `infra` 按需挂它们。

## 4. DDD 包组织(照 patra-catalog)

这是外部工程师第一次看容易懵的地方:为什么端口(接口)定义在 domain,读模型(DTO)也放在 domain,而不是常见的"domain 只放实体"套路?因为这套包组织是"读模型驱动的六边形"——domain 同时承载写侧的不变量(纯计算)和读侧的数据形状(查询结果契约),两者物理分包、语义不同,但都算"业务对这份数据的定义",都不该散落在 app 或 infra 里被技术细节污染。

### domain

```
domain/model/          业务规则与不变量的纯计算(Campaign、DrawResult、DrawEligibility)
domain/model/read/     读侧视图 record,一文件一类(LeaderEntry、PromptSummary、Page<T>……)
domain/model/enums/    枚举
domain/port/repository/   聚合持久化端口({Entity}Repository)
domain/port/read/         CQRS 读投影端口({Entity}ReadPort)
domain/port/{function}/   外部能力/领域动作端口({Function}Port,如 draw/、storage/、thumbnail/)
domain/exception/      业务异常
```

`domain/model/` 里的类型不是 DDD 教科书意义上的"实体"——它们是 record,不可变,没有聚合根基类、没有版本锁、没有领域事件发布订阅那套仪式(第 11 节详述这个刻意的差异)。`domain/model/read/` 里的读视图 record 会原样(或接近原样)透出到 REST 响应——adapter 层直接把它们当 DTO 返回,domain 与 API 契约在这里合流,这是本仓的约定:读视图即对外契约的中间形态,不再另起一套 app 层 DTO 做二次映射。

### app

```
app/usecase/{子域}/            写处理器 {Action}{Entity}Handler
app/usecase/{子域}/command/    命令 {Action}{Entity}Command(record)
app/usecase/{子域}/dto/        写结果 record
app/usecase/{子域}/query/      查询用例 {View}QueryService
```

子域划分不是按技术分层,是按业务动作分组:activity = `draw`、`campaign`;gallery = `prompt`、`interaction`、`generation`。app 层没有 DTO 容器类——读视图在 domain,写命令/写结果各自在自己子域的 `command/`、`dto/` 子包,不设一个大杂烩的 `dto/` 目录。

### infra

```
infra/adapter/persistence/          写侧 JPA 适配器({Thing}Adapter)
infra/adapter/persistence/entity/   JPA 实体({Entity}Entity)
infra/adapter/persistence/dao/      Spring Data 仓储({Entity}JpaRepository)
infra/adapter/read/                 读侧适配器(投影/统计)+ 手写 {Entity}Mapper
infra/adapter/{storage,thumbnail,…}/  其他能力适配器(R2StorageAdapter、ImageIoThumbnailAdapter)
```

Entity ↔ 读视图的映射是手写的(样板 `PromptMapper`),本仓没有 MapStruct——见第 11 节。

### adapter

```
adapter/rest/            Controller,单上下文单 {Context}Controller,路径 /{context}/v1/*
adapter/rest/request/    请求 DTO record
adapter/rest/response/   响应 DTO record
adapter/web/             上下文级横切(如 gallery 的 RateLimitFilter 令牌桶)
```

一个上下文一个 Controller(`ActivityController`、`GalleryController`),不按子资源拆多个 Controller——子域划分体现在它们各自注入的 QueryService/Command 上,不体现在 Controller 数量上。

### api

当前每个上下文的 api 模块只有一个 package-info,空壳预留(见 2.3 节)。

## 5. Port / Service 命名与选型

domain/port 下按端口的性质分四类,命名、包位置、实现层严格对应,不能混着来:

| 类型 | 何时用 | 接口命名 → 实现命名 | 接口包 → 实现包 |
|---|---|---|---|
| Repository | 聚合的持久化(写为主) | `{Entity}Repository` → `{Entity}RepositoryAdapter` | `domain/port/repository/` → `infra/adapter/persistence/` |
| Driven Port | 外部能力/领域动作(抽奖原子事务、R2 读写、缩略图生成) | `{Function}Port` → 具体实现可用具体命名(不强制 XxxAdapter,如 `R2StorageAdapter`) | `domain/port/{function}/` → `infra/adapter/{能力}/` |
| ReadPort | CQRS 读投影(跨聚合的列表/统计/跨库读) | `{Entity}ReadPort` → `{Entity}ReadAdapter` | `domain/port/read/` → `infra/adapter/read/` |
| QueryService | 纯查询编排,无副作用 | `{View}QueryService`,**不定义接口** | 只有 App 实现,住 `app/usecase/{子域}/query/` |

选型逻辑:一个端口如果要被 mock 测(app 层单测常规操作),那接口必须存在,归 Repository/Driven Port/ReadPort 三类之一;QueryService 反而故意不给接口,因为它没有副作用、没有多实现需求——真要 mock 测试它背后编排的东西,直接 mock 它调用的端口就够了,给它包一层接口纯属多余的抽象。

禁止的写法(本仓明确踩过的坑,值得记住):Repository 不带 `Port` 后缀(不写成 `InteractionPort`);读投影不用 `Repository`/`Query` 后缀命名端口(一律 `{Entity}ReadPort`);QueryService 不定义接口;写操作绝不能塞进 QueryService——写一律走 Command + Handler(第 6 节)。

## 6. 写路径 CommandBus,读路径直注

写和读走两条不同的路,adapter 层的调用方式肉眼可辨:

```
写:Controller ──► CommandBus.handle(command) ──► SimpleCommandBus 按 Command 类型路由 ──► CommandHandler(app,@Service)
读:Controller ──► 直接注入的 {View}QueryService ──► 端口
```

基建全部来自 `linqibin-commons`,本仓零自建:`Command<R>`/`CommandHandler<C,R>` 接口在 commons-core 的 `dev.linqibin.commons.cqrs` 包,`SimpleCommandBus`(按 Command 类型自动路由到对应 Handler,支持 `@Order` 拦截器链)由 starter-core 的自动配置装配。

写为什么要多绕一层 bus 而不是 Controller 直接注入 Handler:解耦 adapter 与具体 Handler 实现,给未来要加的横切关注点(审计日志、限流、重试)留一个统一挂载点——真要加,在 bus 层面用 `@Order` 拦截器链一次性覆盖所有命令,不用每个 Controller 方法各自实现。`adapterInjectsBusNotHandlers` 门禁把"adapter 只能注入 CommandBus"这条焊死,防止有人图省事绕过去直接注入某个具体 Handler。

读为什么不走 bus:查询无副作用,不需要横切派发这一套机制,直接注入更直接、少一层间接寻址成本——这是与 patra 相同的选择,不是本仓自创。

命名与位置:命令 `{Action}{Entity}Command`、处理器 `{Action}{Entity}Handler`,一命令一 Handler(泛型绑定路由表)。命令位置 `app/usecase/{子域}/command/`,Handler 直接住 `app/usecase/{子域}/`。返回类型优先复用既有类型(领域计算结果、写结果 DTO),只有确实是新形状才新建一个 Result 类型,不为每个命令都造一个专属返回类型。无返回值的命令用 `Command<Void>`,Handler 里 `return null`(样板 `DeleteGenerationCommand`)。

事务边界:commons 的 `CommandHandler` 官方 JavaDoc 示例把 `@Transactional` 放 Handler 上,**本仓不这么做**——Handler 保持无事务、纯编排,事务边界收在 infra 端口实现内部用 `TransactionTemplate` 显式管理(第 10 节详细说明为什么)。

一个容易被忽视的测试义务:CommandBus 的路由表是运行期按 Command 的泛型参数动态构建的,不是编译期生成——意味着"路由表接对了"这件事本身需要一次真实的端到端派发去验证,静态类型检查证明不了。每个上下文的 `WiringTest` 至少有一条带 token 的写请求真正穿过 Controller → CommandBus → Handler → 真实 PostgreSQL 全链路,就是为了钉住这一点。

## 7. snb-sub2api:防腐层 starter

这是仓库里形态最特殊的模块,值得讲清楚"它是什么、不是什么"。

背景:sub2api 是外部系统——站长中转站生意的主站,一个独立部署的开源 fork,管用户账号、鉴权、充值。本平台收编的两个上下文都要向它借东西:鉴权(这个请求是谁发的)、充值数据(算抽奖资格)。这是经典的防腐层(Anti-Corruption Layer)问题:如果每个上下文各自抄一份"怎么跟 sub2api 说话",上游接口一变全部要跟着改。

### 7.1 形态选择:AutoConfiguration starter,不是领域模块

`snb-sub2api` 不是某个上下文的一部分,也不是一个普通的共享 jar——它是一个 Spring Boot **AutoConfiguration starter**:`@AutoConfiguration` 类 + `META-INF/spring/...AutoConfiguration.imports` 自注册,不依赖宿主(`snb-boot`)的组件扫描路径。这个选择本身是设计决策:自注册意味着它可以像任何第三方依赖一样即插即用——哪怕以后 boot 模块的扫描根包变了,或者这个 starter 被整个拆到别的仓库单独发布,都不需要动 boot 侧的配置。boot 的 `AutoConfigurationExcludeFilter` 会自动跳过 imports 里声明的类,不会跟组件扫描产生双重注册。

包按能力组织,三块独立条件装配:

| 能力 | 装配条件 | 说明 |
|---|---|---|
| `auth/`(introspect 客户端) | 常开(`Sub2apiAutoConfiguration`) | 所有上下文都要鉴权,没有条件开关的必要 |
| `auth/`(`@CurrentUser` 参数解析器) | `@ConditionalOnWebApplication(SERVLET)` + classpath 有 webmvc(`Sub2apiWebAutoConfiguration`) | 非 web 场景(比如未来的批处理模块)不装配一个用不上的 MVC 组件 |
| `recharge/`(充值只读读模型) | `@ConditionalOnProperty("sub2api.read-datasource.url")`(`Sub2apiRechargeAutoConfiguration`) | 不配就完全不装配——不建只读 DataSource,不逼无关的测试/上下文伺候它 |

### 7.2 交互设计准则

这条准则(站长 2026-07-07 定案)对以后往 starter 里加新能力有指导意义:

| 交互形态 | 技术归宿 |
|---|---|
| **读** sub2api 库(只读投影/聚合) | starter 里的 **JdbcTemplate 读模型能力包**——外部 schema、没有实体生命周期要管,不上 JPA、不建第二个持久化单元;SQL 显式收敛在单文件,测试用 Testcontainers 自建上游表结构钉住假设(上游 fork 合并改表时这里先红,起契约测试的作用) |
| **写** sub2api(发码、建 Key、建号) | starter 里的 **API 客户端能力包**,一律走 sub2api 的 HTTP 接口,永不直写库;`@ConditionalOnProperty(admin-key)` 不配不装 |
| 围绕上游长出**自己的业务过程/不变量**(比如账号池管理) | 不进 starter,起新的业务 context(标准五模块),其 infra 消费 starter 的客户端 |

一句话:"怎么跟 sub2api 说话"永远归 starter,"我们自己的业务逻辑"才立新的限界上下文。当前实现了读模型这一路(`RechargeReadModel`/`JdbcRechargeReadModel`),写客户端与账号池这类新业务暂无实例,但包结构(`auth/`、`recharge/`、未来 `admin/`)已经按这个分法预留。

### 7.3 工程红线:绝不暴露只读 DataSource / JdbcTemplate 为 Bean

`Sub2apiRechargeAutoConfiguration` 在 `@Bean` 方法内部现场 `DataSourceBuilder.create()...build()` 构建只读 DataSource,包成 `JdbcTemplate` 直接喂给 `JdbcRechargeReadModel`,不对外暴露这两个原生类型。原因很具体:Spring Boot 对主 `DataSource`/`JdbcOperations` 的自动配置是 `@ConditionalOnMissingBean` 条件——如果 starter 意外把只读源也注册成一个候选 Bean,会把 Boot 该给整个应用用的主数据源自动配置直接挤退,后果是全应用所有原本该查 `snb` 库的代码全部错连到 sub2api 的只读库。这条红线在只有一个读模型时看起来像洁癖,但一旦有人为了"复用"顺手把 `JdbcTemplate` 标成 `@Bean` 就会引发一次很难定位的全局连库错误——错误现场是"某个查询返回的数据不对"或"某张表找不到",不会直接报"数据源装错了"。

### 7.4 ArchUnit 边界与消费方式

`aclStaysOutOfDomainAndApp` 门禁把 `me.supernb.sub2api` 包下的类型钉死只能出现在 infra/adapter,domain/app 完全不感知它的存在。每个上下文自己在 domain 定义端口(如 `RechargeReadPort`),infra 里一个薄适配器(样板 `RechargeReadAdapter`,~30 行)把 starter 的 DTO 映射成本上下文的读视图。好处是即使有一天要换掉整套鉴权体系,domain/app 代码不用动一行。

controller 侧的消费体验被压缩到极致:方法参数声明 `@CurrentUser UserProfile user` 就完成鉴权,不用每个上下文各写一遍"从 header 掏 token、转发校验、拼 401"的样板。解析器(`CurrentUserArgumentResolver`)做的事:转发 Authorization 头到 sub2api 的 `/api/v1/user/profile`、进程内按 token 做 30 秒短缓存吸收高频复验、`UserProfile.isActiveAccount()` 判定 active 的 `user` **或** `admin` 账号才放行,否则统一 401(problem+json,经 `snb-common` 的 `UnauthorizedException`)。放行 admin 是 2026-07-07 割接验收时用真 token 测出来的坑——站长自用账号在旧 gallery-svc 里以 admin 身份产生生成历史与互动记录,如果这里拒绝 admin,历史数据的归属查询会直接对不上,是实打实的割接回归,不是随手加的口子。这个判断收在 `UserProfile` 内部而不是各上下文自己写,保证判定口径只有一处。

鉴权成功后,解析出的 `UserProfile` 会被顺手挂到当前请求属性上(`CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE`),供 JPA 审计的 `AuditorAware`(boot 层 `CurrentUserAuditorConfig`)取值填充 `created_by`/`updated_by`——这是第 8 节要讲的审计基座与本节鉴权机制的唯一交汇点。

## 8. 持久化:JPA 审计基座、雪花 id、对外 id 契约

三块放在一起讲,因为它们是一体的:实体的"生命周期形态"决定选哪个审计基座,基座决定 id 怎么来,id 怎么来决定对外契约怎么设计。

### 8.1 三选一的审计基座

基座家族来自 `commons-starter-jpa`(patra 带来的),按语义选:

| 基座 | 携带的列 | 给什么用 | 本仓实例 |
|---|---|---|---|
| `BaseJpaEntity` | 雪花 id + `record_remarks`(JSONB)+ 完整审计列(created_at/by/by_name、updated_at/by/by_name、version、ip_address) | 聚合根 | `campaign`、`draw`、`category`、`prompt`、`generation` |
| `ChildJpaEntity` | 雪花 id + created_at/updated_at + version | 依附聚合根但有**独立更新时点**的子实体 | `prize_slot`(领奖是独立更新动作)、`prompt_like`/`prompt_favorite`(成员表)、`ref_image` |
| `ValueObjectJpaEntity` | 仅雪花 id | 完全随聚合根生死、没有独立更新语义的表 | `generation_image`、`generation_ref` |
| SoftDeletable 两变体 | 上两者 + Hibernate `@SoftDelete`(`deleted_at`) | 本仓暂无实例 | — |

选哪个不是随便挑的,是在回答"这行数据的生命周期归谁管、要不要留痕":聚合根用 Base;依附聚合根但自己有独立更新时点的子实体用 Child;纯粹是聚合根的一部分、自己没有存在意义的用 ValueObject。软删两变体基座已经提供,但本仓现有的删除语义全是硬删——比如删一条 `generation` 要把 R2 上的对象一起清掉,软删会留下指向已经不存在的 R2 对象的悬空引用,反而制造问题;新聚合如果确实需要软删场景,基座已经在,不用自己搭。

实体子类的写法统一是 `@Getter` + `@NoArgsConstructor(PROTECTED)` + 意图明确的业务构造器/业务方法——基座本身已经带 `@Data`/`@SuperBuilder`,子类**不再**加 `@Data`(全字段 equals/hashCode/toString 与懒加载、`@Id` 语义冲突)。没有业务写路径的实体(`campaign`、`category`、`prompt`——由运维 SQL 或收录管线维护,不是应用代码创建的)不写业务构造器,只留受保护的无参构造器给 JPA。

### 8.2 雪花 id 应用层预分配

数据库不设自增序列,id 在进数据库之前就已经确定。绝大多数实体的业务构造器第一行是 `setId(SnowflakeIdGenerator.getId())`,级联的子实体在各自的构造器里各自领一个。

例外是 `generation` 这个聚合:它的 R2 对象键是 `gen/{userId}/{id}/…`,必须先知道 id 才能算出图片该存哪——不可能先存图片再补 id。所以这一个聚合的 id 由仓储端口的 `nextId()` 方法预分配后传入构造器,而不是构造器内部自己领:调用方(`CreateGenerationHandler`)先拿到 id、算好 R2 key、把图片传上去,成功后再带着确定的 id 构造实体落库。这个"先取号、再落地"的顺序在代码里体现为 Handler 里第一行就是 `long id = repo.nextId();`,后续所有 R2 key 拼接都依赖这个 id。

纯 SQL 写入(数据迁移、收录管线、测试造数)必须显式给 id——没有数据库自增兜底;审计列(`created_at`/`updated_at`/`version`)靠 DDL 的 `DEFAULT` 兜底,纯 SQL 路径不用管这几列。

### 8.3 对外 id 契约:JSON 里一律字符串

雪花 id 是 `long`,数值量级在 3×10^17 左右,超过 JavaScript `Number.MAX_SAFE_INTEGER`(2^53 ≈ 9×10^15)。任何走 `JSON.parse` 的客户端(不只是浏览器)如果把这种大整数当 JSON number 解析,末位精度会丢。规矩是:JSON 响应体里,实体 id 一律建模成字符串——读视图/DTO 的 id 字段类型是 `String`,infra 里 mapper 用 `String.valueOf(...)` 转;URL 路径参数和查询参数照常收 `long`,因为那是走 Spring 的字符串到数字绑定,精度不丢,非法数字请求自然触发 400,不用手写校验。

这条规矩背后还废掉了一个更早的设计:曾经有一版方案给每个写请求配一个客户端生成的 `client_task_id` 自然键,跟服务端雪花 id 双轨并存,目的是让前端能在请求发出前"预知"未来的 id、支持乐观 UI 和建单幂等预检。这个方案在验收时被判定为赘余并推翻,现在是单一身份口径:雪花 id 既是内部持久化身份也是对外身份,不设自然键分身。副作用是"建单幂等预检"这个能力也跟着退役——id 是服务端生成的,客户端没法提前给出待比对的值;防重复提交改靠前端队列的"单飞"语义兜底,这是显式接受的取舍:极端网络重试场景下可能产生重复的历史行,业务上可以接受。

成员表(点赞/收藏/参考图去重引用)的幂等不再靠复合主键或 `@EmbeddedId`(这套已全部退役),改靠唯一约束:`UNIQUE(prompt_id, user_id)`、`UNIQUE(user_id, sha256)`。

## 9. PG 特有并发原语

activity 上下文的核心场景是"同一时刻大量用户点同一个抽奖按钮,奖池里只有有限个奖槽,不能超发"——经典的库存超卖问题。解法是数据库层面的并发原语,不是应用层加锁(应用层锁在多实例部署下不成立,本仓虽然当前单实例部署,但不能把架构决策押注在"以后也一直单实例"这个假设上)。

### 9.1 三层防护

一次抽奖(`DrawAdapter.doDraw`)依次做三件事,缺一不可:

1. **事务级 advisory lock 串行化同一用户**——`pg_advisory_xact_lock(userId)`。锁的维度是用户,不是全局:全局锁会把所有用户的抽奖请求串成一条队列,吞吐灾难;锁用户维度只序列化"同一个人的并发重复点击",不同用户之间完全并行。锁挂在事务上(xact 后缀),随事务提交或回滚自动释放,不需要手动 unlock,也不会因异常导致锁泄漏。没有这道锁的后果:同一用户狂点按钮触发的并发请求会同时通过"剩余次数"校验(校验时读到的是同一个旧值),导致超额领奖。这个函数是 void 返回,JPQL 没法直接把它当标量映射,写法上要包一层子查询:
   ```sql
   SELECT true FROM (SELECT pg_advisory_xact_lock(:key)) AS acquired
   ```
2. **`FOR UPDATE SKIP LOCKED` 原子领槽**——随机挑一个可用奖槽加行锁,如果这行已经被别的并发事务锁住就跳过去找下一行,而不是排队等它:
   ```sql
   SELECT * FROM activity.prize_slot WHERE campaign_id = :campaignId
   AND status = 'available' ORDER BY random() LIMIT 1 FOR UPDATE SKIP LOCKED
   ```
   `SKIP LOCKED` 天然把"等锁"变成"换一行",保证两个不同用户的并发请求不会互相阻塞去抢同一行,整体吞吐不会因为偶尔撞车就退化成串行。这条 native 查询直接返回**受管实体**(`SELECT *` 天然带出基座的全部审计列),拿到后可以直接改字段走 JPA 的 dirty checking 落库——因为这行已经被 `FOR UPDATE` 锁住,不存在并发版本冲突,不需要走 `@Version` 乐观锁那一套校验。
3. **充值总额走只读端口,接受弱一致**——算"剩余抽奖次数"要读 sub2api 库的充值总额,这次读不在同一个事务、也不在同一个数据库里,不可能强一致。这是显式接受的取舍:现查现比,允许存在极短窗口的不一致,不为了这个去搭跨库分布式事务。

### 9.2 其余并发坑

这些是与上面三条配合、覆盖其他并发写场景的踩坑规则:

- **并发删除禁用 JPA 派生 delete**:select-then-remove 模式在两个事务并发删同一行时,后一个会因为查到的实体已经不在了而抛 `StaleStateException`。改用 `@Modifying @Query("DELETE ...")` 直接发一条批量 DELETE,不经过"先查再删",用返回的影响行数(0 或 1)决定这次删除是否真的生效(用来判断要不要联动减计数)。
- **计数增减用原子 UPDATE**:点赞数、收藏数这类反规范化计数,用 `@Modifying` 的 `SET count = count + :delta`,不能读出来加一再存回去(read-modify-write 在并发下会丢更新)。这类批量更新语句不走乐观锁——`@Version` 拦不住它,不能指望版本号发现计数并发冲突,原子 SQL 本身就是唯一的保护。
- **toggle 幂等靠唯一约束 + 事务外回读**:同一用户并发点两次"点赞",两个请求都可能判断"还没赞过"然后都尝试插入成员行,撞在成员表的 `UNIQUE(prompt_id, user_id)` 上。处理方式:插入失败触发整个事务回滚,在事务**外**捕获 `DataIntegrityViolationException`,回读一次当前计数返回——数据库唯一约束天然保证只有一行能插入成功,失败的那个请求不需要重试插入,只需要知道"结果已经是别人达成的那个状态"。
- **唯一键竞态重试一次**:按 `(user_id, sha256)` 去重的写入(参考图去重引用)撞唯一约束时重试一轮,重试轮里如果发现记录已存在(exists 命中)就转成"跳过,复用已有的",不再尝试插入。

其余查询优先走 Spring Data 派生方法或 `@Query` JPQL(含 theta join、接口投影、`org.springframework.data.domain.Limit`),只有 PG 特有语法(advisory lock、SKIP LOCKED)才落 native SQL——这是钦定形态,不要为了"统一风格"把这两条 native SQL 改写成别的写法。

### 9.3 动态 HQL 的测试义务

`EntityManager.createQuery` 拼出来的动态查询(比如列表排序的 `NULLS LAST` 分支)在应用启动期不会被校验——`@Query` 标注的语句会在启动期做一次语法检查,动态拼接的不会,意味着一条写错的分支要等真正跑到才会炸,而且可能炸在生产。规则是每一个动态拼接出来的分支都必须有测试真正跑一遍,拼接内容只允许常量,参数一律走绑定。

## 10. 事务边界与 Flyway

### 10.1 事务边界故意放在 infra

`app` 层没有任何事务注解;事务边界收在 `infra` 端口实现内部,用显式 `TransactionTemplate` 包裹,不用 `@Transactional`。这与 patra 的规范(app 层 `@Transactional`)不同,是本仓刻意做的偏离。

原因是 Spring 声明式事务基于动态代理,同一个类内部方法互相调用(自调用)时代理不会生效——这是个容易踩、踩了很难排查的经典坑(方法上标了 `@Transactional`,但因为是从同一个类的另一个方法内部直接调用,事务实际没有生效,现象是"看起来配了事务但该回滚的没回滚")。`TransactionTemplate` 是显式的 `execute(callback)`,没有代理魔法,读代码就能看清事务边界具体在哪一段 lambda 里。这个选择还带来一个额外的好处:"事务失败后要在事务外做补偿/回读"这类模式(比如第 9 节提到的 toggle 幂等回读)天然成立——回读代码写在 `execute(...)` 调用之外,物理上就不在事务范围内,不需要额外小心翼翼地把它排除在事务外。代价是每个需要事务的 infra 适配器都要自己注入 `PlatformTransactionManager`、手动包一个 `TransactionTemplate` 字段,比一个注解啰嗦,换来的是事务边界肉眼可见、不会被自调用坑到。

### 10.2 单一 Flyway 历史,双 schema

数据库层面:单个 PostgreSQL 库 `snb`,两个 schema(`activity`、`gallery`)。Flyway 只有**一份**版本历史(不是每个上下文各自一份),这意味着版本号是全局唯一序列——`V1` 已经用在 activity 基线,`V2` 已经用在 gallery 基线,下一次不管改哪个上下文,新迁移都要接着编 `V3`,不能各上下文各起一个 `V1`。原因很直接:Flyway 的版本历史表(`flyway_schema_history`)是单一一张表,如果两个上下文都发布 `V1`,Flyway 会认为其中一个已经跑过而跳过它。

迁移脚本物理上仍然放在各自 infra 模块的 `resources/db/migration/{context}/` 下(保持"一个上下文的迁移归它自己的模块"的组织直觉),`snb-boot` 的 `application.yml` 通过 `spring.flyway.locations` 配两个 classpath 路径,在运行期把它们拼成一条历史一起跑。

### 10.3 `ddl-auto=validate` 是防漂移机制,不是建表机制

`spring.jpa.hibernate.ddl-auto` 钉死 `validate`(不是 `update`,不是 `none`),`open-in-view: false`。schema 的唯一真源是 Flyway 迁移脚本,JPA 实体注解不重复声明索引/唯一约束这些 DDL 层面的东西。`validate` 的作用是启动时拿实体的 O/R 映射去跟数据库当前结构比对,一旦迁移脚本与实体字段/类型/列名出现漂移(改了一边忘改另一边),启动直接报错——这条防线的价值在于"漂移在启动那一刻就炸,而不是等到某个具体查询触发才发现列对不上"。改任何一边都必须同步另一边,审计列、JSONB、BYTEA 这些细节都在校验范围内。

## 11. 与 patra 的血统与刻意差异

Patra 是这个仓库的架构母版和基建来源——`linqibin-commons` 直接从 patra 源码 build 出来发布到 mavenLocal,commit 钉死在 `gradle.properties` 的 `patraRef`。但 patra 是微服务架构,本仓库是单体,同一套六边形/DDD 骨架落在不同的部署形态上,必然要做取舍。已知的刻意偏离,逐条说明为什么:

- **无 MapStruct**:Entity ↔ 读视图的映射手写(样板 `PromptMapper`)。仓库规模没有大到需要生成式映射框架分摊维护成本,手写 mapper 更透明,编译期报错更直接,IDE 跳转不会跳进生成代码里。
- **保留 Flyway + `ddl-auto=validate`**:patra 没有迁移脚本。本仓库服务在线生产业务,schema 变更必须可追溯、可重放、有版本历史,这里选的是最保守也最成熟的路线,不跟 patra 的做法。
- **事务边界在 infra 用 `TransactionTemplate`**:patra 规范是 app 层 `@Transactional`。见 10.1 节的自调用代理坑分析——这不是"图省事没照抄",是踩过坑之后的主动选择。
- **domain 刻意薄**:本仓库聚合的生命周期普遍简单(没有复杂状态机、没有需要发布订阅的领域事件),domain 只放"业务规则与不变量的纯计算",不跟进聚合根基类、版本锁、领域事件那一整套仪式。这是"按需引入复杂度",不是偷懒省事:哪天真有一个聚合需要那套东西了再加,不预先把复杂度堆出来占地方等着没人用。
- **单一雪花身份,无 `client_task_id` 自然键双轨**(见 8.3 节):这是本仓库自己走过一遍又推翻的弯路——先加了双轨方案,验收时判定是赘余,废掉改成单一身份。不是"patra 没有这套",是本仓库自己实践中判断不需要。

理解这些差异的存在,能少走"这里跟 patra 不一样,是不是漏改了"的弯路——它们都是同一套架构哲学落在"单体形态 + 生产在线业务"这个具体约束下的必然结果,不是执行走样。

## 12. 构建、测试、开源安全红线

### 12.1 构建顺序

`linqibin-commons` 不在 Maven Central。`scripts/bootstrap-commons.sh` 按 `gradle.properties` 里 `patraRef` 钉的 commit,从公开 patra 仓库 clone/checkout 后现场跑 `publishToMavenLocal`,产出 commons 全家桶(core、starter-core、starter-web、starter-jpa、starter-test)到本地 Maven 仓库。这一步 CI 和本地开发都要先跑一次;本地跑过一次、mavenLocal 里有产物缓存之后可以跳过,直接 `./gradlew build`。commons 升级 = 改 `patraRef` 指向新 commit → 重跑 bootstrap 脚本,不是手改本仓代码。

`./gradlew build` 是完成定义:编译 + 全部测试 + ArchUnit 门禁全绿。`.github/workflows/ci.yml` 在每次 push/PR 到 main 时跑这一整套(先 bootstrap commons,再 build)。

### 12.2 测试分层

| 层 | 形态 | 为什么 |
|---|---|---|
| domain / app / snb-common | 纯单测,零 Spring,mock 端口 | 这层没有框架依赖,起 Spring 上下文纯属浪费时间 |
| infra / snb-sub2api / boot | Testcontainers 真 PG + 真 Flyway | 禁止内存数据库——这层测试要同时验证迁移脚本与实体映射一致(`ddl-auto=validate` 在测试里就会先炸出漂移),内存数据库(H2 之类)方言差异会把这条验证能力废掉 |
| adapter | standalone MockMvc | 不起 Spring 上下文,比 `@WebMvcTest` 快且更可控,只验协议转换(路由/绑定/DTO 形状/鉴权注入),业务行为归 app 单测,不在这层重复测 |

单一 `src/test` 源集,不像 patra 切 unitTest/integrationTest/e2eTest 三个 Gradle source set,一律 `*Test.java` 后缀放同一个目录——单体规模不值得为测试分类多加一层构建复杂度,测试属于哪一类从它 mock 什么、要不要 Testcontainers 就能看出来。真正的端到端测试(compose 全栈 + 浏览器自动化走真实业务流程)不在本仓测试套件里,那是私有运维仓库割接验收的活动,不属于代码仓库的职责范围。

覆盖口径(非强制门禁,作 TDD 纪律与评审参考,本仓暂无 jacoco 强制):单元测试占比 ≥75%,容器集成 + 契约测试合计约 25%;行覆盖 ≥80%,分支覆盖 ≥70%;关键业务(并发发奖、计数增减、鉴权、幂等写入)要求 100%。

### 12.3 发布链路

`.github/workflows/release.yml`:推 `v*` tag 触发云端 build,产出 `ghcr.io/linqibin0826/super-nb-platform:<版本号>`(amd64,对齐生产 VPS 架构)。`workflow_dispatch` 手动触发产出短 SHA 的 tag,用于验证流水线本身,不占用正式版本号。**部署本身(拉镜像、割接、灌数据)不在本仓库范围内**,`docker-compose.sample.yml` 只是占位样例,真实的部署脚本与参数留在私有运维仓库——这条边界是有意为之:代码仓库定义"系统应该长什么样",不代管"生产此刻是什么状态"。

### 12.4 开源安全红线

仓库公开,任何提交内容(含 git 历史)按全网可读对待:

- 不提交真实凭据与生产参数——API key、数据库/对象存储凭据、服务器 IP、内网拓扑。一切外部参数走环境变量注入,`application.yml` 里全是 `${VAR:默认值}` 占位形式。
- `docker-compose.sample.yml` 只放示例参数,真正必填的用 `${XXX:?}` 形式(缺失直接报错而不是静默套用一个错误的默认值,避免"忘配也能跑起来但连错库"这种更隐蔽的事故)。
- 一旦发现敏感信息被提交:立即轮换作废该凭据,再清理提交记录——顺序不能反,先作废保证即使历史清理不干净也不构成实际风险。

---

配套阅读:`.claude/rules/` 下的规则文件是这篇文档每一节内容的机器可读精确版本(路径级生效范围、禁止清单、样板代码),开发时以那边为准;这篇文档负责讲清楚"为什么定成这样",规则文件负责"具体怎么写"。
