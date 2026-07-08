# super-nb-platform

super-nb-platform 是 super-nb 中转站生意的自写后端统一平台:一个部署单元(单体),内部按限界上下文(bounded context)与六边形架构(hexagonal / ports-and-adapters)+ DDD 分层。架构照 [patra](https://github.com/linqibin0826/patra)(同一作者的另一个项目)落地,复用其 `linqibin-commons` 基建——CQRS 总线、JPA 审计基座、统一错误处理这些横切能力不在本仓重新发明。patra 是微服务形态,本仓库把同一套六边形骨架改造成单体,这层取舍见 [ARCHITECTURE.md](ARCHITECTURE.md) 第 11 节。

仓库当前收编两个此前各自独立部署的自写业务服务;未来任何新的自写后端业务都在这里长出新的限界上下文,不再起新服务。前端 `snb-portal`(统一 React 前端)也在同仓、松耦合并存,详见下文「前端 snb-portal」节。

## 能力

### activity —— 活动中心

- **抽奖**:高并发发奖场景下的库存正确性——PostgreSQL 事务级 advisory lock 按用户维度串行化重复点击,`FOR UPDATE SKIP LOCKED` 原子领取奖槽,不同用户之间完全并行、互不阻塞
- **充值榜**:按用户充值总额排行;充值数据来自 sub2api(super-nb 独立部署的用户账号/鉴权/充值主站)的只读读模型,现查现比、接受极短窗口的不一致
- **奖池实况**:公开可查询的奖池档位余量——每个金额档位的总槽数与当前可领(未认领)数

### gallery —— 灵感库

- **提示词库**:公开浏览、关键字搜索、三轴类目筛选与分页;内容由运维收录管线维护,应用侧只读
- **点赞收藏**:登录用户对提示词点赞/收藏,计数走原子 UPDATE 增减(不走读-改-写),并发重复点击靠数据库唯一约束兜底幂等
- **studio 生成历史**:登录用户的 AI 生成记录持久化,图片存 R2(S3 协议兼容对象存储)、下发 presigned URL,列表接口用 256px 缩略图省流量;匿名/登录混合访问场景下有令牌桶限流保护

## 技术栈

Java 25 · Spring Boot 4(4.0.6)· Spring Data JPA(Hibernate 7)+ Flyway · PostgreSQL · AWS SDK v2(R2 对象存储走 S3 协议)· Gradle 9.5(Kotlin DSL + 自定义 convention plugin)

## 模块地图

| 模块 | 层 | 职责 |
|---|---|---|
| `snb-common` | 横切 | 纯 web 横切能力(如 `UnauthorizedException`),不属于任何限界上下文 |
| `snb-sub2api` | 横切 | sub2api 防腐层 starter:AutoConfiguration 自注册、即插即用;鉴权 introspect 客户端(`@CurrentUser` 参数解析器随 web 环境自动装配)+ 充值只读读模型(按配置项条件装配) |
| `snb-activity-domain` | domain | 活动业务规则与不变量(`Campaign`、`DrawResult`、`DrawEligibility`)与端口定义;零框架依赖,编译期强制 |
| `snb-activity-app` | app | 抽奖(draw)、充值榜与奖池(campaign)两个子域的用例编排(Command + Handler、QueryService);不感知持久化技术 |
| `snb-activity-infra` | infra | 端口实现:PG 并发原语落地抽奖、奖池 JPA 持久化、sub2api 只读读模型薄适配 |
| `snb-activity-adapter` | adapter | 入站 REST(`ActivityController`,`/activity/v1/*`),只做协议转换 |
| `snb-activity-api` | api | 对外契约,当前空壳,预留跨上下文调用 |
| `snb-gallery-domain` | domain | 灵感库业务规则与不变量、端口定义(仓储/读投影/存储/缩略图) |
| `snb-gallery-app` | app | 提示词(prompt)、互动(interaction)、生成历史(generation)三个子域的用例编排 |
| `snb-gallery-infra` | infra | 端口实现:JPA 持久化、R2 对象存储、缩略图生成、sub2api 薄适配 |
| `snb-gallery-adapter` | adapter | 入站 REST(`GalleryController`,`/gallery/v1/*`)+ 令牌桶限流 filter |
| `snb-gallery-api` | api | 对外契约,当前空壳 |
| `snb-boot` | boot | 唯一 Spring Boot 组装入口(composition root):装配全部模块、聚合 Flyway 迁移,ArchUnit 门禁与装配冒烟测试住这里 |
| `build-logic` | 构建 | Gradle convention plugin,编译期锁定各层可声明的依赖坐标 |

## 前端 snb-portal

`snb-portal/` 是本平台的统一前端(React 19 + Vite + TypeScript 单页应用),与后端同仓、松耦合——照 [patra](https://github.com/linqibin0826/patra) 的 `patra-portal` 模式:

- **独立构建**:自带 pnpm/vite 工具链,不进 Gradle、不打进后端镜像。`pnpm build` 出静态产物,与后端 `./gradlew build` 互不相干。
- **独立部署**:静态产物 rsync 上线,与后端镜像发布完全解耦——前端改动不触发后端发版。
- **设计系统**:UI 组件 vendor 自品牌设计系统仓 `super-nb-ui`(见 `snb-portal/src/ui/`),该仓独立服务多站,portal 持组件快照、与其手动同步。
- **配置外置**:域名等经 `VITE_*` 环境变量注入(见 `snb-portal/.env.example`),缺省即生产值,fork 自部署改这里即可、无需动源码。

首块承载 studio 生图创作工坊;后续前端能力都在此生长。开发起步见 [`snb-portal/README.md`](snb-portal/README.md)。

## 快速开始

前置:JDK 25(仓库 `mise.toml` 已钉版本,用 [mise](https://mise.jdx.dev) 可直接拿)、git。

```bash
# 1. linqibin-commons 不在 Maven Central,先从 patra 源码(gradle.properties 的 patraRef 钉 commit)
#    现场 build 并发布到本地 Maven 仓库——这一步不是可选项,CI 和本地开发都要先跑一次
bash scripts/bootstrap-commons.sh

# 2. 完成定义:编译 + 全部测试 + ArchUnit 门禁全绿
./gradlew build

# 3. 起服务:需要一个可连接的 PostgreSQL(SNB_DB_URL / SNB_DB_USER / SNB_DB_PASSWORD)+ gallery 模块
#    必装的 R2 端口配置(GALLERY_R2_ENDPOINT / GALLERY_R2_BUCKET / GALLERY_R2_ACCESS_KEY / GALLERY_R2_SECRET_KEY,
#    冒烟可填占位值,S3Client 构建不校验真实连通性)——两者缺一都会在装配阶段启动失败,不存在"无 DB 冒烟"这回事。
#    Flyway 默认开启,连一个全新空库会自动建 schema、跑迁移;SNB_FLYWAY_ENABLED=false 是给 schema 已经
#    在别处迁移好的库跳过重复迁移用的,不是"免 DB"开关。完整变量清单见 snb-boot/src/main/resources/application.yml
./gradlew :snb-boot:bootRun
```

另开一个终端验证:

```bash
curl -s localhost:8080/actuator/health
# {"status":"UP"}
```

commons 只需 `publishToMavenLocal` 一次,本地已有产物缓存后可跳过第 1 步直接 `./gradlew build`。commons 升级 = 改 `patraRef` 指向新 commit → 重跑 bootstrap 脚本,不是手改本仓代码。本仓库到"起服务、连本地库"为止,生产部署/割接/数据迁移归私有运维仓库管理,不在这里。

## 依赖方向

标准 ports-and-adapters,依赖方向单向、不可逆(以 activity 为例,gallery 对称):

```
commons-core(纯 Java,来自 patra)
        │ api
        ▼
snb-activity-domain   (零框架依赖,定义端口)
        │ api
        ▼
snb-activity-app      (用例编排:Command/Handler、QueryService)
        │ api                              │ api
        ▼                                  ▼
snb-activity-adapter                 snb-activity-infra
(REST 入站,只做协议转换)          (端口实现:JPA / sub2api,框架细节全在这层)
        │                                  │
        └────────────────┬─────────────────┘
                          ▼
                      snb-boot
        (唯一同时依赖 adapter + infra + api 的组装点;
         adapter 与 infra 互不依赖,靠 Spring 按 domain 端口接口在此 DI 接通)
```

依赖方向不是君子协定:Gradle convention plugin 在编译期锁死每层可声明的依赖坐标(domain 层还有 `enforceDomainPurity` 任务扫描 classpath,防止框架类型经间接路径混进来),`snb-boot` 里的 `HexagonalBoundaryTest` 在全部模块编译完、组成 `ApplicationContext` 之后用 ArchUnit 再查一遍——两层配合防的是两种不同的违规:一种编译都通不过,一种编译能过但设计上不允许。完整的分层职责表、门禁规则清单、DDD 包组织依据(为什么端口和读模型都放在 domain)、CommandBus 写路径、sub2api 防腐层交互矩阵、雪花 id 与对外契约、PG 并发原语、与 patra 的刻意差异,都在 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 许可

Apache-2.0,见 [LICENSE](LICENSE)。
