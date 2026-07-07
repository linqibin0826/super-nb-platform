---
paths: snb-*/snb-*-app/**/*.java, snb-*/snb-*-adapter/**/*.java
---

# CommandBus 使用规范

**写操作统一经 CommandBus 派发**,adapter 与 Handler 解耦。基建全部来自 linqibin-commons,本仓零自建:接口在 `commons-core` 的 `dev.linqibin.commons.cqrs`,`SimpleCommandBus` 由 `starter-core` 的 `CommandBusAutoConfiguration` 自动装配(starter-web 已把 starter-core 带进 boot classpath,无需显式加依赖)。

## 架构图

```
Adapter 层(Controller/Filter)
    ↓ 只注入 CommandBus
CommandBus(SimpleCommandBus:按 Command 类型自动路由,支持 @Order 拦截器链)
    ↓
CommandHandler(App 层,@Service)
    ↓ 调端口
Domain + Infra
```

## 核心组件

| 组件 | 坐标 | 说明 |
|------|------|------|
| `Command<R>` | commons-core `cqrs` | 命令标记接口,R 为返回类型;**必须用 record** |
| `CommandHandler<C,R>` | commons-core `cqrs` | 一命令一 Handler,泛型绑定路由 |
| `CommandBus` | commons-core `cqrs` | `handle()` 同步 / `handleAsync()` 异步 |
| `SimpleCommandBus` | starter-core 自动配置 | 收集全部 Handler Bean 建路由表;重复 Handler 启动时 warn 覆盖 |

## 命名约定

- 命令 `{Action}{Entity}Command`、处理器 `{Action}{Entity}Handler`(本仓现有:`PerformDraw`、`TogglePromptLike`、`TogglePromptFavorite`、`CreateGeneration`、`DeleteGeneration`)
- 返回类型 R **优先复用既有类型**(领域结果 `DrawResult`、DTO `GalleryDto.LikeResult`),只在结果确属新形状时才立 `{Action}{Entity}Result`
- 命令是顶级类型(不塞进 `{Context}Dto` 容器);record 的 equals 让测试用精确匹配断言派发参数

## Void 返回

无返回值命令用 `Command<Void>`,Handler `return null`(样板 `DeleteGenerationCommand`)。

## Query 端策略(与 patra 同款)

**查询不走 CommandBus**:读操作直接注入查询用例(本仓命名 `Get*UseCase` / `{Thing}Queries`,见 layers/app.md)——无副作用,不需要横切派发。

| 操作 | 路由 |
|------|------|
| 写(Command) | `commandBus.handle(command)` |
| 读(Query) | 直接注入查询用例 |

## 事务(与 commons JavaDoc 示例的差异)

commons 的 CommandHandler 文档示例把 `@Transactional` 放 Handler;**本仓不这么做**——事务边界在 infra 端口实现内用 `TransactionTemplate`(见 tech/jpa.md),Handler 保持无事务、纯编排。

## 禁止行为

1. 禁止 adapter 直接注入 Handler(ArchUnit 门禁 `adapterInjectsBusNotHandlers`)
2. 禁止在 Command 里放业务逻辑(纯数据载体;字段校验最多到紧凑构造器非空)
3. 禁止 Handler 调用其他 Handler
4. 禁止读操作走 CommandBus

## 测试口径

- Handler 单测:直接 `new` + `handler.handle(new XxxCommand(...))`,mock 端口(见 testing/unit-test.md)
- 契约测试:mock `CommandBus`,`when(bus.handle(new XxxCommand(精确参数)))`——record equals 即断言了派发参数(见 testing/adapter-test.md)
- WiringTest:每上下文至少一条带 token 的写请求**真经 bus 派发**到 Handler(路由表是运行期构建的,必须真跑一次;见 testing/integration-test.md)
