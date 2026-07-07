# snb-gallery

super-nb-platform 的灵感库限界上下文。六边形分层的平台级约定(依赖方向的两道强制机制、雪花 id 与对外契约、事务边界选型等)见仓库根 [`ARCHITECTURE.md`](../ARCHITECTURE.md);这篇文档只讲这一个上下文自己的模块分工与设计取舍。

灵感库面向两类使用者:匿名/登录用户都能浏览的公开提示词库(浏览、搜索、按类目筛选),以及登录用户私有的两块资产——对提示词的点赞收藏、studio 独立站产生的 AI 生成历史(图片存 R2、下发限时签名的下载地址、列表配 256px 缩略图省流量)。三块能力数据模型互不相关,放进同一个上下文是因为它们共享同一套鉴权入口与写路径基建,没必要拆成独立部署单元。

## 五模块分工

| 模块 | 内容 |
|---|---|
| `domain`(`snb-gallery-domain`) | `model/enums/`:`SortMode`(提示词排序模式);`model/read/`:九个读视图 record——类目三种形态(`Category`/`CategoryNode`/`CategoryTree`)、提示词瘦身列表与详情(`PromptSummary`/`PromptDetail`)、生成历史瘦身列表/详情/单图(`GenerationSummary`/`GenerationDetail`/`Image`)、批量互动态回填(`MyInteractions`)、统一分页信封(`Page`);`port/`:`repository/`(`GenerationRepository`、`InteractionRepository`)、`read/PromptReadPort`、`storage/ImageStoragePort`、`thumbnail/ThumbnailPort`;`exception/`:`GalleryException` 一个类按场景开静态工厂(`promptNotFound`、`generationNotFound`),目前只有"资源找不到"一种失败形状,没必要拆成多个异常类 |
| `app`(`snb-gallery-app`) | 三个子域,均在 `usecase/` 下:`prompt/` 只有 `query/`(`PromptQueryService`),没有写路径;`interaction/` 是点赞收藏两个 toggle 命令 + Handler + 查询用例;`generation/` 是创建/删除生成记录两个命令 + Handler + 查询用例 |
| `infra`(`snb-gallery-infra`) | `adapter/persistence/`:`GenerationRepositoryAdapter`(4 表一事务)、`InteractionRepositoryAdapter`(成员表 + 反规范化计数),`entity/`+`dao/` 下是 gallery 库全部 8 张表(`category`/`prompt`/`prompt_like`/`prompt_favorite`/`generation`/`generation_image`/`generation_ref`/`ref_image`);`adapter/read/`:`PromptReadAdapter`(动态 HQL)+ 手写 `PromptMapper`;`adapter/storage/`:`R2StorageAdapter`(AWS SDK v2 对接 R2);`adapter/thumbnail/`:`ImageIoThumbnailAdapter`(JDK ImageIO) |
| `adapter`(`snb-gallery-adapter`) | 单一入口 `GalleryController`,路径 `/gallery/v1/*`;`rest/request/`:`CreateGenerationRequest`;`rest/response/`:`DeleteResponse`;`web/`:`RateLimitFilter`+`TokenBucket`,按 IP 的令牌桶限流,只挂本上下文路径前缀 |
| `api`(`snb-gallery-api`) | 空壳预留,当前零跨上下文调用 |

## 核心设计

### 提示词库:公开只读,内容不归应用层管

`prompt`、`category` 两张表由收录管线以纯 SQL 直接写入维护,应用层从未创建或修改过它们——两个实体都只留 JPA 需要的受保护无参构造器,没有业务构造器。列表查询(`PromptReadAdapter.list`)的过滤与排序是拼出来的动态 HQL:类目、关键字两个条件按是否传入决定要不要拼进 `WHERE`,排序分支是一个 `switch`。动态拼接的语句在应用启动期不会被校验(`@Query` 标注的语句才会),写错一个排序分支只会在真正走到那条排序时才炸——所以每个分支都必须有测试真正跑一遍;拼接内容全是常量,参数一律走绑定,没有拼 SQL 注入面。

### 点赞收藏:toggle 幂等 + 计数原子加减

点赞、收藏是同一套模式的两份实现。一次 toggle 先做一次 `exists` 检查决定要不要插入成员行——这一步不是幂等保证本身,只是常态路径下省一次注定失败的插入。真正兜底并发的是成员表的唯一约束(`UNIQUE(prompt_id, user_id)`):两个并发请求都可能通过 `exists` 检查后一起尝试插入,后一个撞约束,整个事务(含它那次计数增量 `UPDATE prompt SET like_count = like_count + 1`)一起回滚。`toggleLike`/`toggleFavorite` 在事务**外**捕获这个 `DataIntegrityViolationException`,直接回读当前计数返回——不重试插入,因为唯一约束已经保证"结果状态是对的",这次请求只是没有促成那次状态转移,不需要再抢一次。

计数列绝不读出来加一再存回去——`read-modify-write` 在并发下会丢更新,一律靠 `@Modifying` 的原子 `UPDATE ... SET like_count = like_count + :delta` 语句,`delta` 正负两用。退出成员的一侧同样绕开一个并发陷阱:用 `@Modifying` 批量 `DELETE` 而不是 Spring Data 派生的 `deleteBy...`,因为后者是"先查再删",并发下第二个请求会查到这行已经被删过而抛 `StaleStateException`;批量 `DELETE` 直接返回影响行数,0 行就说明已经被别的并发请求删过,不再联动减计数。

### studio 生成历史:先取号、再落地

`GenerationRepository` 对应的聚合是整个平台唯一一个"必须先有 id 才能开始干活"的场景。R2 对象键按 `gen/{userId}/{id}/{idx}.png` 命名,上传图片时就要知道最终的 `id`——不可能先存图片再回头补 id。这个仓储端口专门开了一个 `nextId()` 方法预分配雪花 id(平台上其余聚合都是构造器内部自己领号),`CreateGenerationHandler.handle` 第一行就是取号,再逐张把输出图传到 R2、尽力生成一张 256px 的首图缩略图(转码失败不阻断创建,`thumbKey` 留空,列表查询侧对空 `thumbKey` 的存量记录批量回退取首张输出图键,整页一次查询、不逐行 N+1)、参考图按 `sha256` 内容寻址去重(同一用户传过的参考图不重复占 R2 空间;真撞了并发上传的 `UNIQUE(user_id, sha256)` 约束,就把整个落库事务重试一轮,重试轮里 `exists` 命中就跳过那张图的物理上传、只建关联行),最后一个事务把 `generation`/`generation_image`/`generation_ref`/`ref_image` 四张表一起落库。

R2 对象一律私有,查询侧(`GenerationQueryService`)现签 10 分钟有效期的下载地址,过期后前端要重新拉列表或详情换新链接。删除时先删数据库行拿回全部对象键(输出图 + 缩略图),再逐个调 R2 删除——参考图内容库因为可能被该用户其他生成记录复用,不随这次删除一起清理。

### 令牌桶限流:只护自己的路径,不碰付费 API

`RateLimitFilter` 是移植自这个上下文前身 `gallery-svc`(`ratelimit.py`)的令牌桶限流,只挂 `/gallery/` 路径前缀——绝不作用到 `/activity/` 或上游 sub2api 的付费模型 API 路径,那不是这个 Filter 的职责范围。桶按客户端 key(`X-Forwarded-For` 最后一跳,生产环境只经一层 Caddy 反代)分片存在进程内 `ConcurrentHashMap` 里,恒速补充令牌、有容量上限——人类手动滚动列表够不到限速线,脚本狂拉全库会被 429 拒绝并带 `Retry-After`。桶数量超过阈值时清理长期不活跃的 key,防止内存随访问过的 IP 数量无限增长。

## 依赖方向

```
commons-core(patra,纯 Java)
      │ api
      ▼
snb-gallery-domain
      │ api
      ▼
snb-gallery-app
      │ api                        │ api
      ▼                            ▼
snb-gallery-adapter           snb-gallery-infra
(+ snb-common, snb-sub2api)
      │                            │
      └─────────────┬──────────────┘
                     ▼
                 snb-boot
      (implementation adapter + infra + api)
```

`adapter` 与 `infra` 是两条平行分支,彼此之间没有任何依赖声明——一行代码也不会从 `adapter` import `infra`,反过来也一样。两者都经 `app` 传递拿到 `domain` 类型,`infra` 另外直接实现 `domain/port` 下的接口。`snb-boot` 是全仓库唯一同时依赖 `adapter`+`infra`+`api` 的组装点,把两条分支的 Bean 一起扫描进同一个 `ApplicationContext`——`adapter` 经 `CommandBus`/`{View}QueryService` 接口找到 `app` 层的 Bean,`app` 层经 `domain` 定义的端口接口找到 `infra` 层的实现,全程没有一处编译期的 `adapter`↔`infra` 耦合。

gallery 对 `snb-sub2api` 的**实际消费**只在 `adapter`——`@CurrentUser` 解析登录态;`infra` 虽按模块惯例也声明了 `snb-sub2api` 依赖,但当前不 import 其任何类型——不像 activity 的 `infra` 那样消费 sub2api 的充值读模型。本上下文的业务读写(提示词、互动、生成历史)全部落在 `snb` 库自己的 `gallery` schema,不经 sub2api。上下文之间(activity ↔ gallery)目前零依赖,ArchUnit 门禁 `domainDoesNotDependOnOtherContexts` 钉住这一条。

---

配套阅读:仓库根 [`ARCHITECTURE.md`](../ARCHITECTURE.md) 讲平台级的"为什么"(依赖方向的强制机制、雪花 id 与对外契约、PG 并发原语的完整清单、事务边界选型);精确的开发规范在 `.claude/rules/`。
