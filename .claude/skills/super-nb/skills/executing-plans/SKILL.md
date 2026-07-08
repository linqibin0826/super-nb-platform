---
name: executing-plans
description: 当你有一份书面实现计划需要在单独的会话中执行，并设有审查检查点时使用
---

# 执行计划

## 概述

加载计划，批判性审查，执行所有任务，完成后报告。

**开始时宣布：** "我正在使用 executing-plans 技能来实现此计划。"

**注意：** 告诉你的人类伙伴，本套技能在有子代理支持时效果好得多。如果在支持子代理的平台上运行（如 Claude Code 或 Codex），其工作质量会显著提高。如果子代理可用，请使用 `super-nb:subagent-driven-development` 而非此技能。

## 进度追踪机制

writing-plans 写出的 plan 是 Markdown 文件，每个任务的步骤都是复选框 `- [ ]`。**唯一**的进度追踪方式是勾选这些复选框——不使用 TodoWrite，避免双源真相漂移。

| 表示 | 时机 |
|---|---|
| `- [ ]` | 步骤未开始（plan 初始状态） |
| `- [x]` | 步骤完成 |
| 步骤行尾追加 `**[BLOCKED: 原因]**` | 遇到阻塞，需要外部澄清 / 解决（同时在"实施笔记"追一条 OTHER） |
| 步骤行尾追加 `**[SKIPPED: 原因]**` | 该步骤因前置变更不再需要 |

用 Edit 直接改 plan.md 的复选框。任务标题下所有步骤都 `- [x]` 即任务完成。

## 实施笔记维护机制（偏离日志）

plan.md 末尾维护一个 `## 实施笔记` 章节——**实施期的偏离日志**：记录"plan 没规定、但实施时不得不做的决定 / 更改 / 权衡，以及任何用户应知事项"。这是给人类伙伴看的，**也是给未来回看这份 plan 的你看的**——半年后没人记得当时为什么 V12 改成 V13。

### 四类条目

| 前缀 | 何时用 | 例 |
|---|---|---|
| `[DECISION]` | plan 留白处你做了选择 | "plan 未指定缓存 key 前缀，采用 `super-nb:{context}:{entity}`" |
| `[CHANGE]` | 偏离了 plan 的明文要求 | "plan 要求 Flyway V12，但已存在 V12，改为 V13" |
| `[TRADEOFF]` | 知情让步，未来可能要还的债 | "ReadPort 暂未抽缓存装饰器，因当前 QPS 低；量上来再补" |
| `[OTHER]` | 阻塞原因 / 环境怪事 / 给用户的备忘 | "本地未跑 bootstrap-commons，commons 产物缺失，先补跑" |

### 何时追加

- **即时追加，不要等任务结束**——遇到时立即用 Edit 改 plan.md。等任务收尾再回忆，细节就丢了。
- **阻塞**：除了在步骤行尾标 `**[BLOCKED: 原因]**`，必须同时追加一条 `[OTHER]` 或 `[TRADEOFF]` 说明阻塞原因和建议。
- **任务完成前最后自审**：勾最后一个 `- [x]` 之前过一遍——本任务中所有"plan 没说我做了"的事，是否都已追加？没追加的现在补。

### 格式

plan.md 末尾：

```markdown
## 实施笔记

- **[CHANGE]** `2026-07-08 14:30` §任务 3 — plan 要求 Flyway V12，但已存在 V12（来自另一分支），改为 V13。理由：避免编号冲突，不影响业务语义。
- **[DECISION]** `2026-07-08 15:10` §任务 5 — plan 未指定 repair 的 dry-run 默认值，采用 dry-run=true（保守默认）。
```

## 流程

### 步骤 1：加载并审查计划

1. 读取计划文件（路径形如 `<git-root>/docs/plans/YYYY-MM-DD-<feature>.md`）
2. 批判性审查——识别计划中的任何问题或疑虑
3. 如果有疑虑：在开始之前向你的人类伙伴提出
4. 如果没有疑虑：直接进入步骤 2 开始执行

**审查时重点检查：**
- 步骤之间是否有依赖遗漏？（A 依赖 B，但 B 排在 A 之后）
- 验证条件是否明确？（"确认可用"不算，"运行 `./gradlew :snb-gallery:snb-gallery-app:test` 全部通过"才算）
- 是否有隐含的环境假设？（JDK 25、commons 已 bootstrap 到 mavenLocal、Testcontainers 需 Docker、R2 env）
- 架构约束是否被尊重？（写走 CommandBus、读走 QueryService、domain 零框架、JSON id 用 String）

**审查示例：**
```
计划文件：<git-root>/docs/plans/2026-07-08-prompt-favorite.md
任务清单：5 个任务

审查发现：
- 任务 3（添加 Flyway 迁移）应在任务 2（编写 JPA Entity）之后，顺序正确 ✓
- 任务 4 的验证条件写的是"确认功能正常"→ 需澄清：具体跑什么测试？
- 计划未提及 commons 是否已 bootstrap → 已确认本地 mavenLocal 有产物

向伙伴提出：
"计划整体可执行。有一个问题：任务 4 的验证条件不够具体，建议改为
'运行 ./gradlew :snb-gallery:snb-gallery-app:test --tests *Favorite* 全部通过'。"
```

### 步骤 2：执行任务

对于每个任务：

1. **理解目标** — 重读任务描述，明确完成标准
2. **执行实现** — 严格按计划的 5 步 TDD 循环执行（写测试 → 跑测试失败 → 写实现 → 跑测试通过 → commit）。每做完一步把对应 `- [ ]` 改为 `- [x]`。**实施中若遇到 plan 未规定的决定 / 必要更改 / 不可避免的权衡，立即按上方"实施笔记维护机制"追加条目，不要等。**
3. **运行验证** — 按要求运行测试或检查
4. **偏离自审** — 勾完任务最后一步前最后扫一遍：本任务中所有"plan 没说我做了"的事，是否都在 plan.md 末尾"实施笔记"有对应条目？没追加的现在补。
5. **提交变更** — 每完成一个任务提交一次，commit message 引用任务编号；**plan.md 的复选框勾选与实施笔记追加一并 commit**（plan 是单源真相）

**每个任务的节奏：**
```
--- 任务 2/5：（逐任务节奏的格式示例）改动某交互用例 Handler ---
目标：在 snb-gallery-app 调整交互用例的 Command + Handler
完成标准：所有测试通过，重复操作走唯一约束幂等（不抛给用户）

[实现]
- 改 TogglePromptFavoriteHandler（走 CommandBus，命令 TogglePromptFavoriteCommand）
- 在 InteractionHandlersTest 补/改断言
- 每做完一步勾 - [x]

[验证]
$ ./gradlew :snb-gallery:snb-gallery-app:test --tests "*InteractionHandlersTest*"
  InteractionHandlersTest > likeReturnsCountAndFlag PASSED
  InteractionHandlersTest > likeOnMissingPromptThrows PASSED
  InteractionHandlersTest > favoriteReturnsCountAndFlag PASSED
  BUILD SUCCESSFUL

[提交]
$ git add snb-gallery/snb-gallery-app/...
$ git commit -m "test(gallery): 调整交互用例 Handler 单测（任务 2/5）"
--- 任务 2/5 完成（步骤全 - [x]）---
```

**批量审查检查点：**
- 每完成 3 个任务后，暂停回顾：整体方向还对吗？有没有偏离计划？
- 如果发现前面的实现有问题，先修复再继续，不要带着问题往下走

### 步骤 3：处理常见异常

**测试失败：**
1. 读错误信息，定位失败原因
2. 区分：是实现 bug？还是测试本身有问题？还是计划描述有误？
3. 实现 bug → 修复并重跑
4. 测试有问题 → 修复测试，向伙伴说明
5. 计划有误 → 停下来，向伙伴报告并建议修正

**依赖缺失：**
```
任务 3 需要 sub2api introspect，但计划中没有提及 snb-sub2api 依赖。
→ 在任务 3 对应步骤行尾标 **[BLOCKED: 缺 snb-sub2api 依赖]**
→ 在 plan.md 末尾"实施笔记"追加一条 [OTHER]，描述阻塞原因 + 建议
  （示例：任务 3 需 sub2api introspect，plan 未含 snb-sub2api 依赖；建议在任务 3 前插入"引入 snb-sub2api starter"步骤）
→ 停止执行
→ 向伙伴报告
```

**指令不清：**
- 不要猜测意图，不要"合理推断"
- 列出你的理解和困惑，让伙伴澄清
- 等待回复后再继续

### 步骤 4：完成开发

所有任务完成并验证后：
- 宣布："我正在使用 finishing-a-development-branch 技能来完成此工作。"
- **必需子技能：** 使用 `super-nb:finishing-a-development-branch`
- 按照该技能的指引验证测试、展示选项、执行选择

**完成报告模板：**
```
## 执行报告　（以下为格式示例，任务/类名为假想演示、非本仓真实待办）

**计划：** <git-root>/docs/plans/2026-07-08-prompt-tag-filter.md
**分支：** feat/gallery-prompt-tag-filter
**任务：** 5/5 已完成

### 完成的任务
1. ✅ 添加 prompt_tag 表 Flyway 迁移（V3，全局唯一版本号）
2. ✅ 标签筛选读投影 ReadPort + ReadAdapter
3. ✅ 列表查询用例接标签过滤（QueryService）
4. ✅ REST 端点 `GET /gallery/v1/prompts?tags=...`
5. ✅ 集成测试（Testcontainers）

### 验证结果
- ./gradlew :snb-gallery:snb-gallery-app:test 全部通过
- ./gradlew build 全绿（含 ArchUnit HexagonalBoundaryTest）
- plan.md 所有任务步骤已全部 - [x]

### 偏离计划的地方
详见 plan.md 末尾"实施笔记"（共 N 条）。
摘要示例：任务 1 - Flyway 编号从 V12 改为 V13（CHANGE，避开已存在的 V12）。

### 下一步
按 super-nb:finishing-a-development-branch 技能处理合并 / PR
```

## 何时停下来求助

**在以下情况立即停止执行：**
- 遇到阻塞（缺少依赖、测试失败、指令不清）
- 计划有严重缺陷导致无法开始
- 你不理解某条指令
- 验证反复失败（同一测试失败 2 次以上）

**不确定时就问，不要猜测。**

## 何时回到之前的步骤

**回到审查（步骤 1）当：**
- 伙伴根据你的反馈更新了计划
- 根本性的方案需要重新考虑

**不要硬闯阻塞** — 停下来问。

## 注意事项
- 先批判性审查计划
- 严格按照计划步骤执行
- 不要跳过验证
- 每个任务单独提交，commit message 引用任务编号
- 计划要求时引用相应技能
- 遇到阻塞时停下来，不要猜测
- 未经用户明确同意，绝不在 main 分支上开始实现
- 进度追踪**只用** plan.md 的复选框，不使用 TodoWrite（避免双源真相漂移）
- 偏离记录**即时**追加到 plan.md 末尾"实施笔记"，遇到决定 / 更改 / 权衡当下就 Edit，不要等任务结束补写
- **发布 / 部署 / 割接不在本流程内**——完成 = `./gradlew build` 绿，不等于上线；上线是生产操作、等站长点头（见 CLAUDE.md 铁律）

## 集成

**必需的工作流技能：**
- **super-nb:using-git-worktrees** - 必需：开始前建立隔离的工作空间
- **super-nb:writing-plans** - 创建此技能要执行的 Markdown 计划
- **super-nb:finishing-a-development-branch** - 所有任务完成后收尾开发
