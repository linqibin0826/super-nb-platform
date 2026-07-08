---
name: writing-plans
description: 当你有规格说明或需求用于多步骤任务时使用，在动手写代码之前
---

# 编写计划

> **路径约定**：本 skill 中 `<git-root>` 是占位符，指 git 仓库根目录（用 `git rev-parse --show-toplevel` 解析）。所有 `<git-root>/docs/` 子路径都从那里出发。AI 写文件前要么 cd 到 git root，要么用 git-root 解析后的绝对路径，避免 cwd 在子模块时写错位置。

## 概述

编写全面的实现计划，假设工程师对我们的代码库零上下文，且品味存疑。记录他们需要知道的一切：每个任务要修改哪些文件、代码、测试、可能需要查阅的规则（`.claude/rules/`）、如何测试。将整个计划拆成小步骤任务。DRY。YAGNI。TDD。频繁 commit。

假设他们是有经验的 Java 开发者，但对我们的六边形架构约定、模块划分和问题领域几乎一无所知。假设他们不太擅长测试设计。

**开始时宣布：** "我正在使用 writing-plans 技能创建实现计划。"

**上下文：** 此技能应在专用 worktree 中运行（由 `super-nb:using-git-worktrees` 在执行期创建）。

**计划保存位置：** `<git-root>/docs/plans/YYYY-MM-DD-<feature-name>.md`
- 用户对计划位置的偏好优先于此默认值
- **产物是 Markdown**——每个步骤用复选框 `- [ ]` 语法，执行时勾选 `- [x]` 追踪进度（单一真源，不另开 TodoWrite）

## 范围检查

如果规格涵盖了多个独立子系统，它应该在头脑风暴阶段就被拆分为子项目规格。如果没有，建议将其拆分为独立的计划——每个子系统一个。每个计划应该能独立产出可工作、可测试的软件。

## 文件结构

在定义任务之前，先列出将要创建或修改的文件以及每个文件的职责。这是锁定分解决策的地方。

- 设计边界清晰、接口定义良好的单元。每个文件应有一个明确的职责。遵循本仓六边形分层：domain（零框架）/ app（用例、CommandBus 写、QueryService 读）/ infra（持久化、端口适配）/ adapter（REST/web）/ api（跨上下文契约）。
- 你对能一次放入上下文的代码推理得最好，文件越专注你的编辑越可靠。优先选择小而专注的文件，而非承担过多功能的大文件。
- 一起变更的文件应放在一起。按职责拆分，而非按技术层级拆分。
- 遵循已有模式（端口/查询服务命名见 `rules/tech/port-service.md`、包组织照 patra-catalog 见 `rules/layers/`）。如果你正在修改的文件已经变得难以管理，在计划中包含拆分是合理的。

此结构决定了任务分解。每个任务应产出独立的、有意义的变更。

## 任务粒度（Right-Sizing）

一个任务是能携带自己测试循环、值得一个全新审查者把关的最小单元。划分任务边界时：把配置、脚手架、文档步骤折叠进它们所服务的那个任务；只在"审查者可能通过一个任务却驳回它邻居"的地方切分。每个任务以一个可独立测试的交付物收尾。

## 小步骤任务粒度

**每步是一个操作（2-5 分钟）：**
- "编写失败的测试" - 一步
- "运行它确认失败" - 一步
- "实现最少代码让测试通过" - 一步
- "运行测试确认通过" - 一步
- "Commit" - 一步

## 计划文档结构（Markdown）

写到 `<git-root>/docs/plans/YYYY-MM-DD-<feature-name>.md`。**计划必须以下面这个头部开始：**

````markdown
# [功能名] 实现计划

> **给执行者：** 必需子技能——用 `super-nb:subagent-driven-development`（推荐）或 `super-nb:executing-plans` 逐任务实现本计划。步骤用复选框 `- [ ]` 语法追踪进度（单一真源，不另开 TodoWrite）。

**目标：** [一句话描述这个计划构建什么]

**架构：** [2-3 句：落在哪个限界上下文（activity/gallery）、涉及哪几层、关键手法]

**技术栈：** [关键技术/库，如 Java 25 / Spring Boot 4 / Spring Data JPA / commons CommandBus]

**关联 spec：** [docs/specs/ 对应设计文档路径]

## 全局约束

[规格中项目级的要求——版本下限、依赖限制、命名与文案规则、平台要求——每条一行，从规格逐字抄来精确值。每个任务的要求隐含包含本节。本仓固定项：完成定义 = `./gradlew build` 全绿（含 ArchUnit 门禁）；JSON 实体 id 一律字符串；写经 CommandBus / 读经 QueryService。]

---
````

### 任务结构

每个任务用二级/三级标题 + 复选框步骤：

````markdown
### 任务 N：[组件名]

**文件：**
- 创建：`snb-gallery/snb-gallery-domain/src/main/java/me/supernb/gallery/domain/model/Xxx.java`
- 修改：`snb-gallery/snb-gallery-app/src/main/java/.../XxxHandler.java:123-145`
- 测试：`snb-gallery/snb-gallery-domain/src/test/java/.../XxxTest.java`

**接口：**
- 消费：[本任务用到的、前面任务产出的东西——精确签名]
- 产出：[后续任务依赖的东西——精确的方法名、参数与返回类型。实现者只看到自己的任务；这一块是他们得知邻居任务名称与类型的唯一途径。]

- [ ] **步骤 1：编写失败的测试**

```java
@Test
void rejectsInvalidInput() {   // 测试方法名即规格、camelCase，不写 /// JavaDoc（见 rules/testing/overview.md）
    assertThatThrownBy(() -> Xxx.of(null))
        .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **步骤 2：运行测试确认失败**

运行：`./gradlew :snb-gallery:snb-gallery-domain:test --tests "*XxxTest*"`
预期：FAIL（Xxx 尚不存在 / 断言不满足）

- [ ] **步骤 3：写最少实现**

```java
public record Xxx(String value) {
    public Xxx {
        if (value == null) throw new IllegalArgumentException("value 不能为空");
    }
    public static Xxx of(String value) { return new Xxx(value); }
}
```

- [ ] **步骤 4：运行测试确认通过**

运行：`./gradlew :snb-gallery:snb-gallery-domain:test --tests "*XxxTest*"`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add snb-gallery/snb-gallery-domain/src/main/java/.../Xxx.java \
        snb-gallery/snb-gallery-domain/src/test/java/.../XxxTest.java
git commit -m "feat(gallery): 添加 Xxx 值对象（任务 N）"
```
````

### 末尾预留「实施笔记」章节（必需）

所有 `### 任务 N` 之后，在计划文件**末尾**留一个初始为空的偏离日志占位章节——执行期由 executing-plans / subagent-driven-development 按需追加，写计划时必须先建好这个空壳：

```markdown
## 实施笔记

<!-- 实施期由执行者追加：每条前缀 [DECISION] / [CHANGE] / [TRADEOFF] / [OTHER] + 时间 + §任务 N。计划阶段留空。 -->
```

## 绿地 YAGNI 强化

super-nb-platform 仓库边界内照绿地最优形态设计（无历史包袱），编计划时主动剔除以下维度——它们都是 YAGNI 反模式：

- ❌ "向后兼容步骤" / "多版本并存" / "deprecated 标记"（直接删除或重写，单一版本切换）
- ❌ "团队协作妥协"维度（如"先用兼容方案，等团队达成共识后再重构"）
- ❌ 任何"如果时间允许 / 建议后续优化 / 分阶段实施"的语义包装

**但注意与 patra 的关键差异**：本平台服务在线生产业务。**部署 / 割接 / 数据迁移是生产操作，不在本仓库内进行**（归私有运维仓库 + 逐次经站长明确同意）。计划里只写仓库边界内的代码 / schema / 契约变更——**不要**写"生产割接""线上数据迁移""灰度切流"这类步骤（那不归本仓库、也不归本计划）。schema 变更给出 Flyway 迁移脚本（版本号全局唯一，从 V3 起）即可。

## 禁止占位符

每个步骤都必须包含工程师需要的实际内容。以下是**计划缺陷**——绝不要写出来：
- "待定"、"TODO"、"后续实现"、"补充细节"
- "添加适当的错误处理" / "添加验证" / "处理边界情况"
- "为上述代码编写测试"（没有实际测试代码）
- "类似任务 N"（重复代码——工程师可能不按顺序阅读任务）
- 只描述做什么而不展示怎么做的步骤（代码步骤必须有代码块）
- 引用了未在任何任务中定义的类型、函数或方法

## 注意事项
- 始终使用精确的文件路径（含完整模块前缀，如 `snb-activity/snb-activity-infra/...`）
- 每个步骤都包含完整代码——如果步骤涉及代码变更，就展示代码
- 精确的命令和预期输出（模块级测试：`./gradlew :snb-{svc}:snb-{svc}-{layer}:test`；完成定义：`./gradlew build`）
- DRY、YAGNI、TDD、频繁 commit
- 每个任务标题用 `### 任务 N：…`；每个步骤用 `- [ ]` 复选框起头

## 自检

编写完整计划后，以全新视角审视规格并对照检查计划。这是你自己执行的检查清单——不是子代理调度。

**1. 规格覆盖度：** 浏览规格中的每个章节/需求。你能指出实现它的任务吗？列出所有遗漏。

**2. 占位符扫描：** 搜索计划中的红旗——上方"禁止占位符"章节中的任何模式。修复它们。

**3. 类型一致性：** 后续任务中使用的类型、方法签名和属性名是否与前面任务中定义的一致？任务 3 中叫 `save()` 但任务 7 中叫 `saveAll()` 就是 bug。

**4. Markdown 结构完整性：**
- 头部包含目标 / 架构 / 技术栈 / 关联 spec / 全局约束五项？
- 每个任务都有 `### 任务 N：…` 标题？
- 每个步骤都以 `- [ ]` 复选框起头（初始全未勾）？
- 计划**末尾已预留空的 `## 实施笔记` 章节**（执行期偏离日志的落点，下游 executing-plans / subagent-driven-development 依赖它存在）？
- 架构约束在计划里被尊重（写走 CommandBus、读走 QueryService、端口命名合规、JSON id 用 String）？

如果发现问题，直接内联修复。无需重新审查——修好继续推进。如果发现规格中的需求没有对应任务，就添加任务。

## 执行交接

保存计划后，提供执行选项：

**"计划已完成并保存到 `<git-root>/docs/plans/<filename>.md`。两种执行方式：**

**1. 子代理驱动（推荐）** - 每个任务调度一个新的子代理，任务间进行审查，快速迭代

**2. 内联执行** - 在当前会话中使用 executing-plans 执行任务，批量执行并设有检查点

**选哪种方式？"**

**如果选择子代理驱动：**
- **必需子技能：** 使用 `super-nb:subagent-driven-development`
- 每个任务一个新子代理 + 两阶段审查

**如果选择内联执行：**
- **必需子技能：** 使用 `super-nb:executing-plans`
- 批量执行并设有检查点供审查
