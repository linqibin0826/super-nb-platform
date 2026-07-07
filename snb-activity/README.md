# snb-activity

super-nb-platform 的活动中心限界上下文。六边形分层的平台级约定(依赖方向的两道强制机制、雪花 id 与对外契约、事务边界选型等)见仓库根 [`ARCHITECTURE.md`](../ARCHITECTURE.md);这篇文档只讲这一个上下文自己的模块分工与设计取舍。

活动中心提供三块面向用户的能力:抽奖、充值榜、奖池实况。三者共享同一个"活动期"概念(`Campaign`,窗口 `[startsAt, endsAt)`)——活动开着的时候三者都有数据,窗口之外三个只读端点统一降级成空结果,不当异常处理。三块能力里,抽奖是并发设计投入最多的一块:同一时刻大量用户点同一个抽奖按钮,奖池里只有有限个奖槽,不能超发,也不能因为加锁过粗把不同用户的请求并成一条队列。

## 五模块分工

| 模块 | 内容 |
|---|---|
| `domain`(`snb-activity-domain`) | `model/`:`Campaign`(活动期,窗口是抽奖资格与榜单统计的唯一口径)、`DrawEligibility`(抽奖资格纯函数)、`DrawResult`(单次抽奖结果);`model/read/`:六个读视图 record(`LeaderEntry`/`MyDrawView`/`PoolTier`/`PublicDraw`/`RechargeEntry`/`DrawStatus`),外加三个只在查询用例内部流转、不直接对外的中间态(`RawDraw`/`RawWinner`/`CodeStatus`);`port/`:按类型分三个子包——`campaign/CampaignPort`、`draw/DrawPort`、`read/`(`PoolReadPort`+`RechargeReadPort`);`exception/`:`CampaignNotActiveException`(404)、`NoDrawsLeftException`(409) |
| `app`(`snb-activity-app`) | `usecase/draw/`:写路径 `PerformDrawCommand`+`PerformDrawHandler`,外加三个查询用例(抽奖资格状态、我的中奖历史、近期真实中奖信息流);`usecase/campaign/`:只有查询用例(充值榜、奖池实况、近期充值动态),没有写路径——活动期本身不由应用代码创建,由运维 SQL 维护 |
| `infra`(`snb-activity-infra`) | `adapter/persistence/`:`CampaignAdapter`(查当前进行中活动)、`DrawAdapter`(抽奖原子事务,见"核心设计"),映射 `campaign`/`draw`/`prize_slot` 三张表——`CampaignEntity`/`DrawEntity` 是聚合根(`BaseJpaEntity`),`PrizeSlotEntity` 是有独立更新时点的子实体(`ChildJpaEntity`,领奖是一次独立的更新动作);`adapter/read/`:`PoolReadAdapter`(按档位统计奖池余量)、`RechargeReadAdapter`(薄适配 `snb-sub2api` 的充值读模型)。`CampaignEntity`/`PrizeSlotEntity` 没有业务构造器——活动期与奖槽都由运维 SQL 预生成,应用侧只读或只经并发原语认领,不新建 |
| `adapter`(`snb-activity-adapter`) | 单一入口 `ActivityController`,路径 `/activity/v1/*`;`rest/response/` 下只有 `DrawResponse` 一个响应 DTO——其余只读端点直接把 `domain/model/read` 的读视图当响应体返回,不再包一层 |
| `api`(`snb-activity-api`) | 空壳预留,当前零跨上下文调用;第一笔出现时,本上下文对外的契约类型进这里 |

## 核心设计

### 抽奖:三层并发防护,缺一不可

一次抽奖(`DrawAdapter.doDraw`)在一个事务内依次做三件事,移植自这个上下文的前身 `activity-svc`(Python)已经在生产验证过的并发方案:

1. **`pg_advisory_xact_lock(userId)` 串行化同一用户**——锁的维度是用户 id,不是全局。全局锁会把所有人的抽奖请求排成一条队列,吞吐灾难;锁用户维度只序列化"同一个人的并发重复点击",不同用户之间完全并行。锁挂在事务上,随提交或回滚自动释放,不用手动 unlock。没这道锁的后果:同一用户狂点按钮时,并发请求会同时读到同一个"剩余次数"旧值,一起通过校验,导致超额领奖。

2. **`FOR UPDATE SKIP LOCKED` 原子领槽**——从 `prize_slot` 里随机挑一个 `status='available'` 的槽加行锁;这行已经被别的并发事务锁住就直接跳过找下一行,不是排队等它。两个不同用户的并发请求因此不会互相卡在同一行上,整体吞吐不会因为偶尔选中同一行就退化成串行。这条 native 查询返回的是受管实体(`SELECT *` 带出全部审计列),`DrawAdapter` 拿到后直接调 `slot.claim(userId, now)` 改字段——这行已经被行锁保护,不存在并发版本冲突,不用等乐观锁校验,也不用显式调 `save()`,提交时随 dirty checking 自动落库。

3. **池空 → 安慰奖占位**——`SKIP LOCKED` 领不到槽不是异常,是产品定义的正常分支:落一条 `consolation=true`、`redeemCode=null` 的抽奖记录,金额取 `Campaign.consolationAmount()`,不调用发码接口,等运营人工发放。`DrawResult.consolation(amount)` 与 `DrawResult.prize(amount, code)` 是这条分支在返回类型上的唯一区别。

### 抽奖资格:充值门槛与 sub2api 只读端口

`DrawEligibility` 是一个不依赖任何框架的纯函数:应得次数 = 充值总额除以 ¥100 向下取整,剩余 = `max(0, 应得 - 已抽)`。充值总额从哪来是这段的重点——activity 自己的库完全不存充值流水,那是 sub2api(主站)的地盘。`RechargeReadPort` 的实现 `RechargeReadAdapter` 是一层薄适配,委托 `snb-sub2api` starter 的 `RechargeReadModel`(JdbcTemplate 读模型,直读上游库,SQL 与连接细节全收在 starter 里,activity 不感知)。这次读不在同一个事务、也不在同一个数据库里,不可能强一致——设计上接受这一点:现查现比,允许存在极短窗口的不一致,不为了这个搭跨库分布式事务。

同一个 `RechargeReadPort` 也是充值榜、近期充值流水、兑换码状态、脱敏邮箱的唯一数据来源;敏感列(`redeem_code`、`claimed_by`)在 starter 那一层就被过滤掉,从未出现在 activity 的 domain/app 里。

### 充值榜与奖池实况:统一的降级口径,一个例外

`LeaderboardQueryService`(Top10)、`RecentRechargesQueryService`(Top20)、`PoolQueryService`(按档位余量)三个查询用例结构一致:先取当前进行中活动,再按活动窗口委托对应读端口取数——没有进行中活动就返回空列表,不当异常处理。`PoolReadPort` 只出份数(`PoolTier(amount, total, available)`),`redeem_code`/`claimed_by` 这类字段在 `PoolReadAdapter` 里从未出现过,不存在"忘记脱敏"的问题。

这个"无活动 → 空列表"的口径只对公开只读端点成立。抽奖资格状态查询反过来是 `CampaignNotActiveException` → 404:同样是"没有进行中的活动",公开榜单类接口降级成空列表是为了让页面照常渲染,个人化的资格查询找不到活动本质是一次错误的调用时机,更适合当异常处理,让前端明确知道现在不能抽。两种口径的差异不是疏漏,是按端点的使用场景分别决定的。

## 依赖方向

```
commons-core(patra,纯 Java)
      │ api
      ▼
snb-activity-domain
      │ api
      ▼
snb-activity-app
      │ api                        │ api
      ▼                            ▼
snb-activity-adapter          snb-activity-infra
(+ snb-common, snb-sub2api)   (+ snb-sub2api)
      │                            │
      └─────────────┬──────────────┘
                     ▼
                 snb-boot
      (implementation adapter + infra + api)
```

`adapter` 与 `infra` 是两条平行分支,彼此之间没有任何依赖声明——一行代码也不会从 `adapter` import `infra`,反过来也一样。两者都经 `app` 传递拿到 `domain` 类型,`infra` 另外直接实现 `domain/port` 下的接口。`snb-boot` 是全仓库唯一同时依赖 `adapter`+`infra`+`api` 的组装点,把两条分支的 Bean 一起扫描进同一个 `ApplicationContext`——`adapter` 经 `CommandBus`/`{View}QueryService` 接口找到 `app` 层的 Bean,`app` 层经 `domain` 定义的端口接口找到 `infra` 层的实现,全程没有一处编译期的 `adapter`↔`infra` 耦合。

`infra` 比 `adapter` 多依赖一样东西:`snb-sub2api`。两处依赖同一个 starter,目的却不同——`infra` 的 `RechargeReadAdapter` 消费它的充值只读读模型,`adapter` 只用它解析 `@CurrentUser` 登录态,不碰充值数据。上下文之间(activity ↔ gallery)目前零依赖,ArchUnit 门禁 `domainDoesNotDependOnOtherContexts` 钉住这一条。

---

配套阅读:仓库根 [`ARCHITECTURE.md`](../ARCHITECTURE.md) 讲平台级的"为什么"(依赖方向的强制机制、雪花 id 与对外契约、PG 并发原语的完整清单、事务边界选型);精确的开发规范在 `.claude/rules/`。
