# 贡献指南

这份文档写给要往这个仓库提代码的人:环境怎么搭、构建怎么跑、代码规范卡在哪、六边形分层的边界在哪、新上下文怎么起、commit 怎么写、什么东西绝对不能进 git。架构决策背后的"为什么"在 `ARCHITECTURE.md`;逐层的精确规则(路径级生效范围、禁止清单、样板代码)在 `.claude/rules/` 下——本文档只管"贡献流程",不重复那两处已经讲清楚的内容。

## 环境

Java 版本由 [mise](https://mise.jdx.dev/) 管,`mise.toml` 钉了 `zulu-25.30.17.0`(Java 25)。装了 mise,进仓库目录会自动切到这个版本;没装 mise 就手动装同版本的 JDK 25。

Gradle 走仓库自带的 wrapper(`./gradlew`,当前 9.5.0),不需要另外装 Gradle,也不要用系统装的 `gradle` 命令跑——版本对不上会有奇怪的失败。

## 构建

`linqibin-commons`(架构母版 [patra](https://github.com/linqibin0826/patra) 的基建,CQRS 总线、JPA 审计基座、统一错误处理都在这里,本仓不重新发明)不在 Maven Central。第一次构建要先跑:

```bash
bash scripts/bootstrap-commons.sh
```

这个脚本做的事:按 `gradle.properties` 里 `patraRef` 钉的 commit,clone/checkout 公开的 patra 仓库,现场跑几个 `publishToMavenLocal` 任务,把 commons 全家桶(core、starter-core、starter-web、starter-jpa、starter-test)发布到本地 Maven 仓库。跑一次之后 mavenLocal 有产物缓存了,后续开发可以跳过这一步,除非 `patraRef` 升级了要重跑。需要 JDK 25 + git。

之后正常构建:

```bash
./gradlew build
```

单模块跑法举例:

```bash
./gradlew :snb-gallery:snb-gallery-infra:test
```

⚠️ `infra` / `snb-sub2api` / `snb-boot` 三层的测试用 Testcontainers 起真实 PostgreSQL(不允许内存数据库顶替,见下面"TDD 与完成定义"),本地要有 Docker 在跑,否则这几层的测试直接失败,不是环境问题看错了。

CI(`.github/workflows/ci.yml`)在每次 push / PR 到 main 时跑同样两步:先 bootstrap commons,再 `./gradlew build`。本地跑不过的东西,CI 也过不了,不要指望 push 上去让 CI 帮忙查。

## TDD 与完成定义

强制 Red-Green-Refactor:先写一个失败的测试,写最少的代码让它变绿,再在测试保护下重构。不写测试直接写实现、测试还没过就继续叠新功能,都不是本仓库的开发方式。

完成定义只有一条——**`./gradlew build` 全绿**。这句话包含三件事:编译过、全部测试过、ArchUnit 门禁(六边形分层边界检查,见下一节)也过。三件事缺一个都不算完成:光测试绿但 ArchUnit 红不算,只是编译过没跑全部测试更不算。提交前自己先跑一遍这条命令,不要把这个环节让给 CI。

## 代码规范要点

完整规则在 `.claude/rules/code-style.md`,这里列几条最容易踩的:

- **注释用 `///` markdown 风格 JavaDoc,不用 `/** */`**。所有方法都要写,包括构造函数、包括非 public 方法。例外只有两类:Lombok 注解生成的方法(注解本身就是文档,不用重复说明)、测试方法(方法名即规格,见 `.claude/rules/testing/`)。
- **命名要准确反映抽象层次**:抽象概念用抽象命名(`Repository`、`Port`、`QueryService`),具体实现用具体命名(`R2StorageAdapter`、`ImageIoThumbnailAdapter`)。禁止 `Manager`、`Helper`、`Util` 这类说不清职责边界的类名。
- **禁止全类名(FQN)**,类型引用一律 `import`。唯一例外是同一文件里出现两个同名类型需要消歧义(本仓已知实例:Spring Data 的 `Page` 和读视图的 `Page` 同现的文件),这种场景才允许写一次 FQN。
- **Lombok 优先**:`@Getter` / `@Setter` / `@Builder` / `@NoArgsConstructor` 这类注解能生成的样板不要手写,只有需要自定义逻辑时才手写。domain 与 DTO 一律用 record,本来就不需要 Lombok。
- **JPA 实体禁用 `@Data`**——全字段 `equals` / `hashCode` / `toString` 跟懒加载、`@Id` 语义直接冲突。实体子类统一 `@Getter` + `@NoArgsConstructor(access = PROTECTED)` + 意图明确的业务构造器/业务方法;审计基座(`BaseJpaEntity` / `ChildJpaEntity` / `ValueObjectJpaEntity`)本身已经带了 `@Data` / `@SuperBuilder`,子类不要再叠一层。
- **AI 辅助开发场景,Serena 语义工具优先于内置文本工具**。本仓 `.mcp.json` 配了 Serena MCP,符号级读写(`get_symbols_overview`、`find_symbol`、`find_referencing_symbols`、`replace_symbol_body` 等)是首选,内置的整文件 Read / Grep / Edit 是次选——只有 Serena 已经尝试失败、文件没法按代码解析、或者是 Markdown / JSON / YAML 这类非代码文件时才用内置工具。人工走惯用 IDE 正常开发就行,这条只约束 AI 辅助会话。

## 六边形分层与依赖方向

每个限界上下文五个模块:`domain`(业务规则与不变量,零框架依赖)、`app`(用例编排,不感知持久化技术)、`infra`(实现 domain 定义的端口,技术细节全在这层)、`adapter`(入站协议转换,不写业务判断)、`api`(对外契约,当前空壳预留)。依赖方向单向不可逆:

```
adapter → app → domain
infra   → app + domain(实现 domain 的端口接口)
```

`adapter` 和 `infra` 互相之间没有任何依赖——两条独立分支,只在 `snb-boot` 里由 Spring 按 domain 定义的端口接口做运行期依赖注入接起来。

这条边界不是靠自觉,有两层强制:

1. **Gradle 编译期**——`build-logic` 的 convention plugin(`snb.hexagonal-{domain,app,infra,adapter,api,boot}`)锁死每层能声明的依赖坐标;`domain` 模块另外挂一个 `enforceDomainPurity` 任务,扫描已解析的 classpath,一旦出现 `org.springframework`、`jakarta.persistence`、`org.hibernate` 这些禁用 group 直接编译失败。
2. **ArchUnit 编译产物级门禁**——`snb-boot` 的 `HexagonalBoundaryTest`,在全部模块编译完、组装成一个 `ApplicationContext` 之后再检查一遍依赖方向。这一层挡的是"Gradle 挡不住但设计上不允许"的写法。

`HexagonalBoundaryTest` 当前钉了这几条:

| 规则 | 检查内容 |
|---|---|
| `domainIsFrameworkFreeAndInward` | domain 不依赖 app/infra/adapter,也不依赖 Spring/JPA/Hibernate |
| `appDoesNotDependOnInfraOrAdapter` | app 不依赖 infra/adapter |
| `appIsPersistenceFree` | app 不依赖 jakarta.persistence/hibernate/spring-data |
| `domainDoesNotDependOnOtherContexts` | 上下文之间不互相依赖(如 activity 包不依赖 gallery 包)。⚠️ 当前只钉了 activity→gallery 一个方向,不是自动泛化的双向规则 |
| `aclStaysOutOfDomainAndApp` | `me.supernb.sub2api` 类型只能出现在 infra/adapter,domain/app 不感知 sub2api 的存在 |
| `adapterInjectsBusNotHandlers` | adapter 不依赖任何 `CommandHandler` 实现类,写操作只能注入 `CommandBus` |
| `apiIsContractOnly` | api 模块不依赖 domain/app/infra/adapter |

这条门禁跑在 `./gradlew build` 里,不是单独的检查——违反了直接 build 红,不会等到 code review 才被发现。上下文之间要互相调用,不走直接依赖,走对方的 `api` 契约模块(当前两个上下文的 api 模块都是空壳,零跨上下文调用)。

## 新增限界上下文

未来新的自写业务不起新服务,在本仓库长一个新的限界上下文:

1. **拷贝五模块骨架**——照现有 `snb-activity` 或 `snb-gallery` 的 `{domain,app,infra,adapter,api}` 五个目录整体复制一份,目录改名 `snb-{context}/snb-{context}-{层名}`,包名改成 `me.supernb.{context}.*`,清空业务代码只留骨架(package-info、空壳 build.gradle.kts)。每个模块的 `build.gradle.kts` 照抄来源自带的 convention plugin 声明(`snb.hexagonal-domain` 等)——这些插件本身就是上一节说的第一道依赖边界强制,不需要另外配置。
2. **`settings.gradle.kts` 注册**——用已有的 `includeAt(path, dir)` 帮助函数把五个新模块的坐标和目录路径登记进去,照抄 activity/gallery 那几行的写法。
3. **Flyway 用全局唯一版本号**——本仓单个 PostgreSQL 库、单份 Flyway 历史横跨所有上下文的 schema,版本号是全局唯一序列,不是每个上下文各起一份 `V1`。当前 `V1` 已经用在 activity 基线、`V2` 用在 gallery 基线,新上下文的第一个迁移接着编 `V3`。迁移脚本物理上仍放在自己 infra 模块的 `resources/db/migration/{context}/` 下,`snb-boot` 的 `application.yml` 加一条 `spring.flyway.locations` 指到新路径。
4. **`snb-boot` 组装 + `WiringTest`**——新上下文的 adapter/infra 模块加进 `snb-boot` 的依赖,写一个 `{Context}WiringTest` 跑一次真实的端到端派发(至少一条带 token 的写请求穿过 Controller → CommandBus → Handler → 真实 PostgreSQL)。这条测试不是锦上添花:`CommandBus` 的路由表是运行期按 Command 的泛型参数动态构建的,不是编译期生成,"路由表接对了"这件事本身需要一次真实派发去验证,静态类型检查证明不了。

`HexagonalBoundaryTest` 里按层名匹配的几条规则(`domainIsFrameworkFreeAndInward`、`appDoesNotDependOnInfraOrAdapter` 等)用包名通配符,新上下文起来后自动被覆盖,不需要手改。但上表 `domainDoesNotDependOnOtherContexts` 这条目前是按上下文对硬编的单向规则——新上下文如果之后真长出跟其他上下文互相调用的场景,记得手动补一条规则,不能假设它已经泛化覆盖了全部上下文对。

## 提交规范

commit message 用中文写,代码标识符保持英文;结尾按本仓库既有约定带一行:

```
Co-Authored-By: <助手名字> <noreply@anthropic.com>
```

`git log` 能看到大量实例。每次实质性改动提交一次,不要把不相关的改动攒进同一个 commit。

## 安全红线

仓库计划开源,任何提交内容(含 git 历史)都按全网可读对待:

- 不提交真实凭据与生产参数——API key、数据库/对象存储凭据、服务器 IP、内网拓扑,一律不进 git。
- 外部参数走环境变量注入:`application.yml` 里全是 `${VAR:默认值}` 占位形式;`docker-compose.sample.yml` 只放示例参数,真正必填的用 `${XXX:?}` 形式——缺失时直接报错,不要给一个能悄悄跑起来但连错库/连错存储的默认值。
- 一旦发现敏感信息被提交:先轮换作废该凭据,再清理提交记录,顺序不能反——先作废才能保证即使历史没清干净也不构成实际风险。
- 部署、割接、生产数据迁移这些操作不在本仓库范围内,归运维侧管理;本仓库只定义"系统应该长什么样",不代管"生产此刻是什么状态"。
