# super-nb-platform

super-nb 业务自写后端统一平台。单体多模块 DDD(六边形架构),照 [patra](https://github.com/linqibin0826/patra) 架构,复用其 `linqibin-commons` 基建。

收编两个限界上下文:

- **activity** — 活动中心(抽奖 / 充值榜 / 奖池)
- **gallery** — 灵感库(提示词库 / 点赞收藏 / 生成历史)

## 技术栈

Java 25 · Spring Boot 4 · Gradle 9.5 · JPA + Flyway · PostgreSQL · AWS SDK v2(R2)。

## 模块

```
snb-common      纯 web 横切(限流等)
snb-sub2api     sub2api 防腐层(introspect 鉴权 + 充值只读读模型)
snb-activity/   {domain,app,infra,adapter}
snb-gallery/    {domain,app,infra,adapter}
snb-boot        唯一 Spring Boot 组装入口 + Flyway 迁移
```

依赖方向:`adapter → app → domain`;`infra → app + domain`;上下文之间不互相依赖。

## 构建

`linqibin-commons` 不在 Maven Central,需先从 patra 源码 publishToMavenLocal:

```bash
bash scripts/bootstrap-commons.sh    # 从 patra@<gradle.properties:patraRef> 构建 commons
./gradlew build
```

本地若已 publishToMavenLocal 过 commons,可直接 `./gradlew build`。

## 运行

```bash
SNB_FLYWAY_ENABLED=false ./gradlew :snb-boot:bootRun    # 无 DB 冒烟
# 或带 PG:配置 SNB_DB_URL / SNB_DB_USER / SNB_DB_PASSWORD 等环境变量
```

`GET /actuator/health` 健康检查。

## 许可

Apache-2.0(见 LICENSE)。
