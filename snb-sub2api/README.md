# snb-sub2api

sub2api 是外部上游——站长中转站生意的主站,一个独立部署的开源 fork,管用户账号、鉴权、充值。本平台收编的 activity、gallery 两个限界上下文都要向它借东西:鉴权(这个请求是谁发的)、充值数据(算抽奖资格)。这是经典的防腐层(Anti-Corruption Layer)问题——如果每个上下文各自抄一份"怎么跟 sub2api 说话",上游接口一变全部要跟着改。

`snb-sub2api` 是**唯一**知道 sub2api 细节(HTTP 接口形状 + 库表结构)的模块。上游变化时只修这一个模块,各上下文的 domain/app 完全不感知它的存在。

## 包结构

```
me.supernb.sub2api
├── auth/         introspect 鉴权客户端 + @CurrentUser 参数解析器(所有上下文共用)
├── recharge/     充值 / 兑换码只读读模型(条件装配)
└── autoconfig/   @AutoConfiguration 装配入口与配置属性,只放装配声明
```

`autoconfig/` 不放能力实现,只做三块能力的条件装配声明;实际逻辑住 `auth/`、`recharge/`。

## 核心设计

### 形态:AutoConfiguration starter,不是领域模块

这不是某个上下文的一部分,也不是一个普通的共享 jar——它是 Spring Boot **AutoConfiguration starter**:`@AutoConfiguration` 类 + `META-INF/spring/...AutoConfiguration.imports` 自注册,不依赖宿主(snb-boot)的组件扫描路径。三个装配类全部经 imports 文件登记:

```
me.supernb.sub2api.autoconfig.Sub2apiAutoConfiguration
me.supernb.sub2api.autoconfig.Sub2apiRechargeAutoConfiguration
me.supernb.sub2api.autoconfig.Sub2apiWebAutoConfiguration
```

自注册意味着它可以像任何第三方依赖一样即插即用——哪怕以后 boot 模块的扫描根包变了,或者这个 starter 被整个拆到别的仓库单独发布,都不需要动 boot 侧的配置。boot 的 `AutoConfigurationExcludeFilter` 会自动跳过 imports 里声明的类,不会跟组件扫描产生双重注册。

### 能力条件装配

三块能力各自独立生效,互不牵连:

| 能力 | 装配类 | 条件 | 为什么 |
|---|---|---|---|
| introspect 客户端 | `Sub2apiAutoConfiguration` | 常开(仅 `@ConditionalOnMissingBean` 可覆盖) | 所有上下文都要鉴权,没有条件开关的必要 |
| `@CurrentUser` 参数解析器 | `Sub2apiWebAutoConfiguration` | `@ConditionalOnWebApplication(SERVLET)` + classpath 有 webmvc + 已存在 introspect 客户端 Bean | 非 web 场景(比如未来的批处理模块)不装配一个用不上的 MVC 组件 |
| 充值只读读模型 | `Sub2apiRechargeAutoConfiguration` | `@ConditionalOnProperty("sub2api.read-datasource.url")` | 不配就完全不装配——不建只读 DataSource,不逼无关的测试/上下文伺候它 |

### 交互设计准则(站长 2026-07-07 定案)

这条准则对以后往 starter 里加新能力有指导意义,按交互形态分技术归宿:

| 交互形态 | 技术归宿 |
|---|---|
| **读** sub2api 库(只读投影 / 聚合) | starter 里的 **JdbcTemplate 读模型能力包**——外部 schema,没有实体生命周期要管,不上 JPA、不建第二个持久化单元;SQL 显式收敛在单文件,测试用 Testcontainers 自建上游表结构钉住假设(上游 fork 合并改表时这里先红,起契约测试的作用) |
| **写** sub2api(发码、建 Key、建号) | starter 里的 **API 客户端能力包**,一律走 sub2api 的 HTTP 接口,**永不直写库**;`@ConditionalOnProperty(admin-key)` 不配不装 |
| 围绕上游长出**自己的业务过程 / 不变量**(比如账号池管理) | 不进 starter,起新的业务限界上下文(标准五模块),其 infra 消费 starter 的客户端 |

一句话:"怎么跟 sub2api 说话"永远归 starter,"我们自己的业务逻辑"才立新的限界上下文。

当前只落地了读模型这一路——`RechargeReadModel`(端口形状,窗口口径统一 `[start, end)`)/ `JdbcRechargeReadModel`(唯一实现,独立只读 `DataSource` + `JdbcTemplate`)。写客户端与账号池这类新业务暂无实例,但包结构(`auth/`、`recharge/`、未来 `admin/`)已经按这个分法预留。

`JdbcRechargeReadModel` 内部承担一条安全边界:面向公开信息流的方法(`leaderboard`、`recentRecharges`、`maskedEmailsByIds`)在实现内部完成邮箱脱敏(保留本地部分前 2 位 + `***` + 域名),未脱敏的完整邮箱不跨出这个类的方法作用域;`redeem_code`、`claimed_by` 这类敏感列同样绝不透出。

### ⚠️ 工程红线:绝不暴露只读 DataSource / JdbcTemplate 为 Bean

`Sub2apiRechargeAutoConfiguration` 在 `@Bean` 方法内部现场 `DataSourceBuilder.create()...build()` 构建只读 DataSource,包成 `JdbcTemplate` 直接喂给 `JdbcRechargeReadModel`,不对外暴露这两个原生类型。

原因很具体:Spring Boot 对主 `DataSource`/`JdbcOperations` 的自动配置是 `@ConditionalOnMissingBean` 条件——如果 starter 意外把只读源也注册成一个候选 Bean,会把 Boot 该给整个应用用的主数据源自动配置直接挤退,后果是全应用所有原本该查 `snb` 库的代码全部错连到 sub2api 的只读库。这条红线在只有一个读模型时看起来像洁癖,但一旦有人为了"复用"顺手把 `JdbcTemplate` 标成 `@Bean`,就会引发一次很难定位的全局连库错误——错误现场是"某个查询返回的数据不对"或"某张表找不到",不会直接报"数据源装错了"。第二个读模型出现时,这条纪律收敛为 starter 内部共享的自有 holder 类型,仍然不暴露原生类型。

### ArchUnit 边界

`me.supernb.sub2api` 包下的类型只能出现在 infra / adapter——`aclStaysOutOfDomainAndApp` 门禁(住 snb-boot 的 `HexagonalBoundaryTest`,编译产物级检查)。domain/app 完全不感知这个包的存在:每个上下文自己在 domain 定义端口(如 `RechargeReadPort`),infra 里一个薄适配器(样板 `RechargeReadAdapter`,~30 行)把 starter 的 DTO 映射成本上下文的读视图。好处是即使有一天要换掉整套鉴权体系或读模型实现,domain/app 代码不用动一行。

### 消费方式

- **controller 鉴权**:方法参数声明 `@CurrentUser UserProfile user` 即完成鉴权,不用每个上下文各写一遍"从 header 掏 token、转发校验、拼 401"的样板。解析器(`CurrentUserArgumentResolver`)做的事:转发 Authorization 头到 sub2api 的 `/api/v1/user/profile`、进程内按 token 做短缓存(`sub2api.introspect-cache-seconds`,默认 30 秒,含负缓存)吸收高频复验、`UserProfile.isActiveAccount()` 判定 active 的 `user` **或** `admin` 账号才放行,否则统一抛 [UnauthorizedException](../snb-common/README.md)(映射 401 problem+json)。放行 admin 是 2026-07-07 割接验收时用真 token 测出来的坑——站长自用账号在旧 gallery-svc 里以 admin 身份产生生成历史与互动记录,如果这里拒绝 admin,历史数据的归属查询会直接对不上。判断口径唯一收在 `UserProfile` 内部,不各上下文自己写一份。
- **审计联动**:鉴权成功后,解析出的 `UserProfile` 会被顺手挂到当前请求属性上(`CurrentUserArgumentResolver.CURRENT_USER_ATTRIBUTE`),供 JPA 审计的 `AuditorAware`(boot 层 `CurrentUserAuditorConfig`)取值填充 `created_by`/`updated_by`——这是 snb-sub2api 与审计基座之间唯一的交汇点,细节见 [snb-boot 的说明](../snb-boot/README.md)。
- **infra 薄适配**:注入 starter 的读模型 / 客户端,把上游 DTO 映射为本上下文的读视图,不直接把 starter 类型透到 domain/app。
