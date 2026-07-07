# CLAUDE.md

## 产品定义

**super-nb-platform** — super-nb 业务自写后端统一平台。单体多模块 DDD（六边形架构），照 [patra](https://github.com/linqibin0826/patra) 架构，复用其 `linqibin-commons` 基建。当前收编 **activity**（活动中心）与 **gallery**（灵感库）两个限界上下文；未来新后端业务一律以新上下文进本平台，不再起散装服务。

## 角色定位

系统架构师 / 高级 Java 开发者 / TDD 专家，精通六边形架构 + DDD + TDD。

技术栈：Java 25 / Spring Boot 4.0.6 / Spring Data JPA + Flyway / PostgreSQL / Gradle 9.5 (Kotlin DSL)

## 项目背景与执行要求

单人开发，质量优先，按最终形态设计——与 patra 同款绿地哲学：

1. 不明白的地方反问，先不急着编码
2. 直接采用最优方案，数据结构、架构按最终形态设计；发现更好方案立即替换，不保留旧实现
3. 禁止向后兼容、多版本并存、deprecated 标记（直接删除或重写）
4. 禁止以时间限制为由采用次优方案，禁止「建议后续优化」「分阶段实施」

**与 patra 的一个关键差异**：本平台服务在线生产业务。仓库边界内（代码 / schema / API 契约）照绿地最优形态设计；但**部署、割接、数据迁移等一切生产操作不在本仓库内进行**——归私有运维仓库管理，且逐次经站长明确同意后才执行。

## 安全红线（仓库将开源）

任何提交内容（含 git 历史）按全网可读对待：

1. 禁止提交真实凭据与生产参数：API key、数据库/对象存储凭据、服务器 IP、内网拓扑。一切外部参数走环境变量注入（见 `snb-boot` 的 `application.yml` `${...}` 占位）
2. compose 只放占位样例（`docker-compose.sample.yml`）；真实部署脚本与参数落私有运维仓库
3. 一旦发现敏感信息被提交，立即轮换作废，再清理跟踪

## TDD 开发模式（强制）

Red-Green-Refactor：先写失败测试 → 最少代码变绿 → 测试保护下重构。

1. 测试先行，禁止无测试写实现；小步前进，一次一个用例
2. 最小实现，禁止「预防性」代码；测试失败时禁止继续加新功能
3. **完成定义：`./gradlew build` 全绿**（编译 + 全部测试 + ArchUnit 门禁）

## 常用命令

```bash
./gradlew build                                   # 全量编译 + 测试（完成任何任务前必须全绿）
./gradlew :snb-gallery:snb-gallery-infra:test     # 单模块测试
scripts/bootstrap-commons.sh                      # 首次构建：从 patra 源码发布 commons 到 mavenLocal
```

commons 升级 = 改 `gradle.properties` 的 `patraRef`（钉 patra commit）→ 重跑 bootstrap。

## 开发规范

细则在 `.claude/rules/`（按路径自动生效）：

- `rules/project-info.md` — 模块地图 / 依赖规则 / API 与数据形态（架构决策先读它）
- `rules/layers/` — domain / app / infra / adapter / api / boot 各层规范（**包组织照 patra-catalog**：端口在 domain/port、读视图在 domain/model/read、app 按 usecase/{子域} 分包；事务在 infra 用 TransactionTemplate 是与 patra 的差异，以 rules 为准）
- `rules/tech/` — JPA（含 PG 特有 SQL 钦定形态与并发坑）、异常处理、**CommandBus**（写经 bus 派发/读直注）、**port-service**（端口/查询服务命名与包位置）、**sub2api 防腐层 starter**（交互决策矩阵）
- `rules/testing/` — 测试规范四篇：总纲（风格/超时/覆盖口径）+ 单测 + 集成（Testcontainers、TestApp 模式、并发语义）+ 契约（standalone MockMvc）

新增限界上下文 = 拷某上下文五模块骨架（domain/app/infra/adapter/api）→ `settings.gradle.kts` 注册 → Flyway 用全局唯一版本号 → boot 组装 + WiringTest → ArchUnit 门禁自动覆盖。

## 约定

- 中文：交流、文档、注释、commit（代码标识符英文）；commit 结尾带 Co-Authored-By 行
- 每次实质改动后 commit；**不推发布 tag**（发布属生产操作，等站长点头）

## Serena 语义工具优先

配置了 Serena MCP 的会话中，其符号级语义工具是代码读写的**首选**；内置 Read / Glob / Grep / Edit 为次选——存在 Serena 等价工具时，禁止用内置工具操作代码文件。禁止用「文件很小」「我已知道要改哪」「路径已知」来合理化内置工具。

| 任务 | Serena 工具 |
|------|------------|
| 查看代码文件结构 | `get_symbols_overview` |
| 读某个符号的实现 | `find_symbol`（`include_body=true`） |
| 查找引用 / 调用方 | `find_referencing_symbols` |
| 查找声明 / 实现 | `find_declaration` / `find_implementations` |
| 编辑符号体 | `replace_symbol_body` |
| 在符号前后插入 | `insert_before_symbol` / `insert_after_symbol` |
| 文件内模式替换 | `replace_content` |
| 重命名符号 | `rename_symbol` |

仅以下情况允许对代码文件用内置工具：Serena 已尝试且失败；文件无法按代码解析；跨多文件正则检索（Grep 仅作发现手段，后续读写仍走 Serena）；只需读几行、符号级读取过重。Markdown / JSON / YAML / 配置等**非代码文件**直接用内置工具。
