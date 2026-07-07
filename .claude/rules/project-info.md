# 项目信息

## 概览

**super-nb-platform** (0.1.0-SNAPSHOT) — super-nb 业务自写后端统一平台，收编原本散装的自写服务，未来新后端业务一律以新限界上下文进本平台。

**架构**: 单体 + 六边形架构 + DDD（照 [patra](https://github.com/linqibin0826/patra)，适配单体形态：全局唯一 boot，无 per-context api/boot 模块）
**技术栈**: Java 25 | Spring Boot 4.0.6 | Spring Data JPA (Hibernate 7) + Flyway | PostgreSQL | AWS SDK v2 (R2)
**构建**: Gradle 9.5 (Kotlin DSL) + Convention Plugins；基建复用 patra 的 `dev.linqibin.commons`（mavenLocal 产物，`gradle.properties` 的 `patraRef` 钉 patra commit）

## 限界上下文

- `snb-activity` — 活动中心：抽奖（advisory lock + `FOR UPDATE SKIP LOCKED` 并发发奖）/ 充值榜 / 奖池实况
- `snb-gallery` — 灵感库：提示词库 / 点赞收藏 / studio 生成历史（R2 + presigned + 256px 缩略图）/ 令牌桶限流

## 模块结构

```
snb-platform/
├── build-logic/            # 约定插件（各层依赖在插件里锁定）
├── snb-common/             # 纯 web 横切（UnauthorizedException）
├── snb-sub2api/            # sub2api 防腐层 starter（唯一知道 sub2api 细节的模块）
├── snb-activity/           # 包组织照 patra-catalog（2026-07-07 验收意见③）
│   ├── snb-activity-domain # model/(不变量) model/read/(读视图) port/{repository,read,功能}/ exception/
│   ├── snb-activity-app    # usecase/{子域}/{Handler, command/, dto/, query/}
│   ├── snb-activity-infra  # adapter/{persistence(+entity,dao), read, …} 按能力分包
│   ├── snb-activity-adapter# rest/{Controller, request/, response/} + web/
│   └── snb-activity-api    # 对外契约(空壳预留,跨上下文调用契约进这里)
├── snb-gallery/            # 同上四模块（子域 prompt/interaction/generation）
└── snb-boot/               # 唯一 @SpringBootApplication + application.yml + 守门测试
```

**依赖规则（ArchUnit 编译产物级门禁，`snb-boot` 的 `HexagonalBoundaryTest`）**:

- 上下文内：adapter→app→domain；infra→app+domain；domain 零框架依赖
- **上下文之间禁止直接互相依赖**；跨上下文调用只经对方 **api 契约模块**（空壳已建），消费方 infra 薄适配（见 layers/api.md）
- app 不感知持久化技术（jakarta.persistence / hibernate / spring-data 都不许碰）
- `me.supernb.sub2api` 类型只进 infra/adapter，不进 domain/app
- 写操作经 commons `CommandBus` 派发（命令/Handler 在 app，adapter 禁依赖 Handler 实现；读操作直接注入查询用例）——见 tech/commandbus.md

**build-logic 约定插件**: `snb.java-base`（Lombok/JUnit/编码）、`snb.java-library`、`snb.spring-library`、`snb.hexagonal-{domain,app,infra,adapter,api,boot}`。⚠️ Gradle 9.5 预编译插件脚本只能用 `//` 注释（块注释会炸）。

## API 形态

- 路径：`/activity/v1/*`、`/gallery/v1/*`（版本进路径；裸 `/api/*`、`/v1/*` 被上游反代占用）
- 契约：错误统一 RFC 9457 problem+json；成功 200 裸 DTO；分页 `page`/`size` + `{items, total}`
- 鉴权：`Authorization: Bearer <token>` → snb-sub2api introspect；公开端点不要求头

## 数据

- 单库 `snb`，双 PG schema `activity` + `gallery`，**单 Flyway 历史 → 版本号全局唯一**（V1 已用于 activity、V2 已用于 gallery，新迁移从 V3 起）
- 迁移脚本放各 infra 模块 `resources/db/migration/{context}/`，boot 靠 classpath 聚合
- sub2api 库 = 第二只读数据源，仅 snb-sub2api 模块消费（见 tech/sub2api.md）

## 常用命令

```bash
./gradlew build                                   # 全量编译+测试（完成定义）
./gradlew :snb-gallery:snb-gallery-infra:test     # 单模块测试
scripts/bootstrap-commons.sh                      # 首次构建：发布 linqibin-commons 到 mavenLocal
```
