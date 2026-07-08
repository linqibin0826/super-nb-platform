---
name: verification-before-completion
description: 在宣称工作完成、已修复或测试通过之前使用，在提交或创建 PR 之前——必须运行验证命令并确认输出后才能声称成功；始终用证据支撑断言
---

# 完成前验证

## 概述

在没有验证的情况下宣称工作完成，这不是高效，而是不诚实。

**核心原则：** 始终用证据支撑结论。

**对这条规则敷衍了事，就等于违背了它的精神。**

## 铁律

```
没有新鲜的验证证据，不许宣称完成
```

如果你在这条消息中没有运行验证命令，就不能声称测试通过。

## 完成 ≠ 上线

本仓「完成定义」= `./gradlew build` 全绿（编译 + 全部测试 + ArchUnit 门禁）。**这是代码层面的完成，不等于发布上线**——部署 / 割接 / 数据迁移是生产操作，不在本仓库内、也不由本技能触发，等站长明确点头（见 CLAUDE.md 铁律）。别把"build 绿"说成"上线了"。

## 绿地项目无借口

super-nb-platform 是单人维护、质量优先的项目（见 `CLAUDE.md`），不存在以下"合理化"借口：

- ❌ "时间不够" —— 项目明确"质量优先，可投入任何必要时间"
- ❌ "团队要 deadline 先合并" —— 没有团队，没有外部 deadline
- ❌ "先合并稍后补测试" —— 禁止分阶段交付（CLAUDE.md 明令禁止）
- ❌ "只跑单元测试就够了" —— 集成测试 + ArchUnit 架构门禁 + 编译，全栈门控才算门控
- ❌ "向后兼容/多版本所以验证可以放宽" —— 绿地项目没有历史包袱，不存在这类妥协

绿地项目的特殊之处：**没有外部压力来逼你妥协，所以也没有借口可以躲在背后**。

## 门控函数

```
在宣称任何状态或表达满意之前：

1. 确定：什么 Gradle/JUnit 命令能证明这个结论？
2. 运行：执行完整命令（重新运行，完整执行）
3. 阅读：完整输出，检查 BUILD SUCCESSFUL/FAILED，统计失败数
4. 验证：输出是否支持这个结论？
   - 如果否：用证据说明实际状态
   - 如果是：带证据陈述结论
5. 只有这时：才能做出结论

跳过任何一步 = 说谎，不是验证
```

## 常见失败模式

| 结论 | 需要 | 本仓实际命令 | 不够格 |
|------|------|---------------|--------|
| 单元测试通过 | JUnit 输出：tests successful，failed: 0 | `./gradlew :snb-{svc}:snb-{svc}-{layer}:test` | 之前的运行结果、"应该会通过" |
| 集成测试通过 | Testcontainers 起容器后 BUILD SUCCESSFUL | `./gradlew :snb-{svc}:snb-{svc}-infra:test` | 单元测试通过 ≠ 集成测试通过 |
| 编译成功 | javac exit 0，零错误 | `./gradlew compileJava compileTestJava` | IDE 标红消失、"代码看起来没问题" |
| 六边形架构纯净 | ArchUnit 门禁通过 | `./gradlew :snb-boot:test --tests "*HexagonalBoundaryTest*"` | "我没引入 Spring 注解吧" |
| 全栈门控（完成定义） | build 全绿（编译+全部 test+ArchUnit 门禁） | `./gradlew build` | 部分检查、单模块通过 |
| Bug 已修复 | 复现 bug 的测试现在通过 | `./gradlew test --tests "*BugRegressionTest*"` | 代码改了，假设已修复 |
| 回归测试有效 | 红-绿循环已验证 | 见下文"关键模式"红-绿示例 | 测试只通过了一次 |
| Subagent 已完成 | `git diff` 显示真实变更 + plan.md 复选框 | `git diff HEAD~1 --stat` | Subagent 报告"成功" |
| 需求已满足 | 逐项核对 plan.md 的 `- [ ]` 步骤 | 重读 plan.md，对照实现 | 测试通过 ≠ 需求完整 |

## 红线——停下来

- 使用"应该"、"大概"、"似乎"
- 验证前就表达满意（"太好了！"、"完美！"、"搞定！"等）
- 即将 `git push` / 创建 PR 却没跑过 `./gradlew build`
- 信任 subagent 的成功报告
- 依赖部分验证（只跑了 unit 没跑集成测试）
- 想着"就这一次"
- 累了想赶紧收工
- 把 "build 绿" 说成 "已上线"
- **任何暗示成功但实际未运行验证的措辞**

## 防止合理化

| 借口 | 现实 |
|------|------|
| "应该能行了" | 运行 `./gradlew build` |
| "我有信心" | 信心 ≠ JUnit 输出 |
| "就这一次" | 没有例外 |
| "IDE 没标红" | IDE ≠ Gradle 编译 |
| "Linter 通过了" | Linter ≠ 编译器 ≠ 测试 |
| "Subagent 说成功了" | 独立验证 `git diff` + plan.md 复选框 |
| "我累了" | 疲劳 ≠ 借口 |
| "部分检查就够了" | 单模块 `:test` 通过 ≠ `build` 通过 |
| "Testcontainers 太慢，跳过集成测试" | 慢 ≠ 可以跳过 |
| "换个说法这条规则就不适用了" | 精神大于字面 |

## 关键模式

### 单元测试

```
✅ $ ./gradlew :snb-activity:snb-activity-app:test --tests "PerformDrawHandlerTest"
   > Task :snb-activity:snb-activity-app:test
   PerformDrawHandlerTest > noActiveCampaignIsNotActive() PASSED
   PerformDrawHandlerTest > delegatesToDrawPort() PASSED
   PerformDrawHandlerTest > propagatesNoDrawsLeft() PASSED
   BUILD SUCCESSFUL in 8s
   "全部测试通过（3/3）"

❌ "应该能通过了" / "看起来对了"
```

### 回归测试（TDD 红-绿验证）

绿地项目里，写完测试不仅要"通过"，还要**确认它真的能抓住 bug**——否则你只是写了一个永远不会失败的测试。

```
✅ 真正的红-绿循环（以真实类 DrawEligibility.remainingDraws 的空值优雅降级为例）：

   1. 先写测试（针对"totalRecharge 为 null 时剩余次数应为 0"这条不变式）：
      @Test
      void nullTotalIsZero() {
        assertThat(DrawEligibility.remainingDraws(null, 0)).isZero();
      }

   2. 运行 → 必须红（实现里对 null 直接调 .signum() 会 NPE）：
      $ ./gradlew :snb-activity:snb-activity-domain:test --tests "DrawEligibilityTest.nullTotalIsZero"
      > FAILED: NullPointerException

   3. 加最小实现（入口空值守卫）：
      if (totalRecharge == null || totalRecharge.signum() <= 0) {
        return 0;
      }

   4. 运行 → 必须绿：
      > PASSED

   5. 临时回退实现（注释掉守卫）→ 重跑必须红：
      > FAILED: NullPointerException
      ↑ 证明这个测试真的能抓到 bug

   6. 恢复实现 → 重跑必须绿：
      > PASSED

❌ "我写了回归测试"（没有经过红-绿验证 → 可能是个永远通过的死测试）
```

### 构建 / 编译

```
✅ $ ./gradlew compileJava compileTestJava
   BUILD SUCCESSFUL in 12s
   "编译通过"

❌ "IDE 没标红了"（IDE 不等于 Gradle，且 IDE 可能用 incremental cache）
❌ "代码看起来对"（人眼 ≠ javac）
```

### 六边形架构纯净性

本仓用 `snb-boot` 的 ArchUnit `HexagonalBoundaryTest` 强制层依赖（domain 零框架、adapter 只注 CommandBus、`me.supernb.sub2api` 不进 domain/app 等），违规 `./gradlew build` 失败。

```
✅ $ ./gradlew :snb-boot:test --tests "*HexagonalBoundaryTest*"
   BUILD SUCCESSFUL
   "六边形层依赖纯净（ArchUnit 全绿）"

❌ 在 domain 层加了 @Component / @Entity 还说"应该没问题"
```

### 全栈门控（完成定义 / PR 前必跑）

```
✅ $ ./gradlew build
   > Task :snb-gallery:snb-gallery-domain:test PASSED
   > Task :snb-gallery:snb-gallery-app:test PASSED
   > Task :snb-gallery:snb-gallery-infra:test PASSED（Testcontainers 起 PG）
   > Task :snb-boot:test PASSED（含 HexagonalBoundaryTest / WiringTest）
   BUILD SUCCESSFUL in 2m 14s
   "全栈门控通过，可提 PR（注意：合并 ≠ 上线）"

❌ "我只跑了单元测试" / "Testcontainers 太慢就跳过了"
```

### 需求满足（基于 Markdown plan）

writing-plans 输出的 plan 是 Markdown，每个任务下挂 `- [ ]` 步骤。声称任务完成前：

```
✅ 重读 plan.md → 对照 ### 任务 N 下每个 - [ ] 步骤 → 全部 - [x]
   → 任务的验收标准对照实际代码与测试 → 通过
   → 报告 "任务 N 完成（n/n 步骤 - [x]，验收标准 m/m 满足）"

❌ "测试通过了，阶段完成"（没对照 plan 的验收条目，也没勾复选框）
```

### Subagent 委派验证

信任 subagent 是高风险行为，详见 `super-nb:subagent-driven-development` 控制者复审章节。最小验证：

```
✅ Subagent 报告"任务 3 完成" → 执行：
   1. git diff HEAD --stat                              # 确认有真实代码变更
   2. ./gradlew :snb-{svc}:snb-{svc}-{layer}:test        # 确认测试真的能跑通
   3. 打开 plan.md 看任务 3 的步骤复选框是否已勾到位
   → 全部通过才能进入规格合规审查

❌ "Subagent 说成功了" → 直接勾 - [x]
```

## 为什么这很重要

来自历史失败记录：
- 搭档说"我不信你"——信任被破坏
- 未定义的函数被交付——会直接崩溃
- 遗漏需求被交付——功能不完整
- 虚假完成浪费的时间 → 返工 → 重做
- 违反原则："诚实是核心价值。如果你说谎，就会被替换。"

绿地项目里，**这些代价由你独自承担**——没人帮你擦屁股。

## 何时使用

**以下情况之前必须使用：**
- 任何形式的成功/完成声明
- 任何满意的表达
- 任何关于工作状态的正面陈述
- `git commit` / `git push` / 创建 PR
- 把 plan.md 的任务步骤勾成 `- [x]`
- 进入下一个任务
- 委派给 subagent（前置）+ subagent 返回后（后置复审）

**本规则适用于：**
- 准确措辞
- 同义词和换一种说法
- 暗示成功
- 任何传达完成/正确性的沟通

## 底线

**验证没有捷径。**

运行 `./gradlew build`。阅读输出。然后才能宣称结果。

这没有商量余地。
