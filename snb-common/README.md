# snb-common

跨上下文共享的纯 web 横切模块。不属于 activity、gallery 中任何一个限界上下文,也不是 [snb-sub2api](../snb-sub2api/README.md) 的一部分——鉴权失败要抛的那个 401,语义上跟 sub2api 的接口形状、库表结构没有任何关系,是纯粹的平台关注点,不该塞进知道上游细节的防腐层里。

## 包结构

```
me.supernb.common
└── UnauthorizedException   平台统一 401:未登录 / 身份无效(非 active 的 user/admin 账号)
```

单包单类,没有第二个类型——这就是"最小面"的字面意思。

## 核心设计

**为什么不并进 snb-sub2api**:`UnauthorizedException` 目前唯一的实际抛出点是 snb-sub2api 的 `CurrentUserArgumentResolver`(`@CurrentUser` 解析失败时),但异常本身携带的语义只是"未登录或身份无效 → 映射 401",不涉及 sub2api 的 HTTP 接口形状或库表结构。如果把它挪进 snb-sub2api,任何想抛这个异常的场景——哪怕跟 sub2api 完全无关——都要被迫引入整个防腐层依赖,模块边界就错位了。依赖方向因此是单向的:snb-sub2api 依赖 snb-common,不是反过来。

**错误映射零自建**:`UnauthorizedException` 继承 commons-core 的 `DomainException`,构造时带上 `StandardErrorTrait.UNAUTHORIZED`;把它翻译成 RFC 9457 problem+json 响应体的工作全部交给 `commons-starter-web` 的统一错误处理机制,本模块不写一行 `ResponseEntity` 拼装代码。

**消费方式**:作为构建依赖,声明在 snb-sub2api(实际抛出点)、activity/gallery 各自的 `adapter` 模块、以及 snb-boot——都是处理 HTTP 请求边界的地方;`infra`、`domain`、`app` 都不依赖它,401 是协议边界的关注点,不是业务不变量该管的事。

**刻意维持最小面**:这不是"还没顾上填内容"的半成品,是有意为之。新东西要不要放进来,判断标准是"是否所有上下文都用得到、且不携带任何单一上下文或单一外部系统的细节"——不满足就该归对应上下文自己的模块,或者归 snb-sub2api;不能因为"图方便复用"就把这里变成大杂烩式的 Util/Helper 仓库(见 code-style.md 对 Manager/Helper/Util 类名的禁止)。
