# 根因追踪

## 概述

Bug 通常表现在调用栈深处（在错误目录执行写入、用错误条件查 DB、构造对象时拿到 null 字段）。你的本能是在错误出现的地方修复，但那只是治标。

**核心原则：** 沿着调用链反向追踪，直到找到最初的触发点，然后在源头修复。

## 何时使用

```dot
digraph when_to_use {
    "Bug 出现在调用栈深处？" [shape=diamond];
    "能反向追踪吗？" [shape=diamond];
    "在症状处修复" [shape=box];
    "追踪到最初的触发点" [shape=box];
    "更好的做法：同时添加纵深防御" [shape=box];

    "Bug 出现在调用栈深处？" -> "能反向追踪吗？" [label="是"];
    "能反向追踪吗？" -> "追踪到最初的触发点" [label="是"];
    "能反向追踪吗？" -> "在症状处修复" [label="否——死胡同"];
    "追踪到最初的触发点" -> "更好的做法：同时添加纵深防御";
}
```

**适用场景：**
- 错误发生在执行深处（不在入口点）
- 堆栈跟踪显示很长的调用链
- 不清楚无效数据从哪里来
- 需要找到是哪个测试/代码触发了问题

## 追踪流程

### 1. 观察症状
```
java.lang.IllegalArgumentException: prompt 不能为空
    at me.supernb.gallery.domain.port.repository.GenerationRepository$SaveGeneration.<init>(GenerationRepository.java:20)
    at me.supernb.gallery.app.usecase.generation.CreateGenerationHandler.handle(CreateGenerationHandler.java:60)
    at dev.linqibin.commons.cqrs.CommandBus.handle(CommandBus.java:38)
    at me.supernb.gallery.adapter.rest.GalleryController.createGeneration(GalleryController.java:130)
    ...
```

### 2. 找到直接原因
**哪段代码直接抛出了这个错误？**
```java
// GenerationRepository 端口内的落库载荷 record（domain 层）
public record SaveGeneration(
        long id, long userId, String prompt, /* ...其余字段省略 */) {
    public SaveGeneration {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt 不能为空");
        }
    }
}
```

### 3. 问：谁调用了它？
```
CreateGenerationHandler.handle(cmd)
  → new GenerationRepository.SaveGeneration(id, cmd.userId(), cmd.prompt(), ...)   ← 这里传入了空字符串
  → 被 CommandBus.handle(CreateGenerationCommand) 调用
  → 被 GalleryController.createGeneration(@RequestBody CreateGenerationRequest) 调用
```

### 4. 继续向上追踪
**传入了什么值？**
- `cmd.prompt() = ""`（空字符串！）
- `cmd` 由 Controller 从 `CreateGenerationRequest` 组装成 `CreateGenerationCommand`——请求体是 JSON 反序列化的结果
- 那 JSON 里 `prompt` 是空字符串

### 5. 找到最初的触发点
**空字符串从哪里来的？**
```java
// Controller
@PostMapping("/me/generations")
public Created createGeneration(@RequestBody CreateGenerationRequest body, @CurrentUser UserProfile user) {
    return commandBus.handle(new CreateGenerationCommand(user.id(), body.prompt(), body.size(), body.n(), ...));
}

// CreateGenerationRequest record
public record CreateGenerationRequest(String prompt, String size, int n, ...) {
    // ❌ 方法没有 @Valid、字段没有 @NotBlank，Bean Validation 完全没被触发，"" 直接放行
}
```

**根本原因：** 入口既没有 `@Valid` 触发校验，DTO 字段也没有 `@NotBlank`，空字符串穿透到 domain record 的紧凑构造器才被发现，但此时调用栈已经深 4 层。

**修复（源头）：**
```java
@PostMapping("/me/generations")
public Created createGeneration(@Valid @RequestBody CreateGenerationRequest body, @CurrentUser UserProfile user) {
    return commandBus.handle(new CreateGenerationCommand(user.id(), body.prompt(), body.size(), body.n(), ...));
}

public record CreateGenerationRequest(
    @NotBlank String prompt,
    String size, int n,
    ...
) {}
```
Spring 在反序列化后会返回 `400 Bad Request` + 清晰的字段级错误。

## 添加堆栈跟踪

当无法手动追踪时，添加诊断埋点：

```java
// 在有问题的操作之前
public SaveGeneration {
    if (prompt == null || prompt.isBlank()) {
        // 临时埋点：捕获完整调用栈
        log.warn("[debug] empty prompt received, value={}, caller stack:",
                 prompt,
                 new Throwable("stack trace for empty input"));
        throw new IllegalArgumentException("prompt 不能为空");
    }
}
```

**重要：**
- 用 `new Throwable("...")` 当 log 第 N 个参数，SLF4J 会自动打印 stack trace
- 不要 `e.printStackTrace()`（不走 logger，CI 抓不到）
- 临时埋点用完**立即删除**——不要污染生产日志

**运行并捕获：**
```bash
./gradlew :snb-gallery:snb-gallery-app:test 2>&1 | grep -A 30 "empty prompt received"
```

**分析堆栈跟踪：**
- 找测试方法名 / Controller 入口
- 找触发调用的行号
- 识别模式（同一个 endpoint？同一个 DTO 字段？）

## 找出导致污染的测试

如果某些现象在测试套件运行期间出现，但不知道是哪个测试造成的（如：测试后 `.gitignored` 目录里多了文件、某些静态状态被改了）：

使用本目录下的二分查找脚本 `find-polluter.sh`：

```bash
./find-polluter.sh '.cache/test-tmp' '*Test'
```

它会按 Gradle 测试模式逐批运行，二分定位污染来源测试类。详见脚本内使用说明。

## 真实案例：空 prompt 穿透 4 层

**症状：** boot 守门测试 `GalleryWiringTest`（真实装配 + Testcontainers，写请求真经 `CommandBus` 派发到 Handler）期望 `400`，实际返回 `500 IllegalArgumentException`。注意：`GalleryControllerTest` 这类 adapter 契约测试 mock 了 `CommandBus`，请求根本到不了 domain record，看不到这个症状——只有真派发到 Handler 的测试才能复现。

**追踪链：**
1. `SaveGeneration` 紧凑构造器抛 `IllegalArgumentException` ← domain record 校验
2. `CreateGenerationHandler.handle(cmd)` 构造 `SaveGeneration` 时触发
3. `CommandBus.handle(command)` 没捕获，直接抛出
4. `GalleryController.createGeneration(@RequestBody CreateGenerationRequest)` —— 没有 `@Valid`
5. `CreateGenerationRequest` 字段没注解 ← **最初触发点**

**根本原因：** DTO 字段缺少 `@NotBlank`、Controller 方法缺少 `@Valid`，校验完全没触发，空字符串穿透到 domain record。

**修复：** 给 Controller 方法加 `@Valid`，给 `CreateGenerationRequest.prompt` 加 `@NotBlank`。

**同时添加了纵深防御：**
- 第 1 层：`CreateGenerationRequest.prompt` 字段 `@NotBlank`，Controller 加 `@Valid`（入口校验）
- 第 2 层：`CreateGenerationHandler` 用 `Objects.requireNonNull` 兜底（应用层）
- 第 3 层：`SaveGeneration` 紧凑构造器自身校验（domain record 层，保留）
- 第 4 层：契约测试 `GalleryControllerTest` 覆盖 `@NotBlank` 触发路径

## 关键原则

```dot
digraph principle {
    "找到了直接原因" [shape=ellipse];
    "能向上追踪一层吗？" [shape=diamond];
    "反向追踪" [shape=box];
    "这就是源头吗？" [shape=diamond];
    "在源头修复" [shape=box];
    "在每一层添加校验" [shape=box];
    "Bug 不可能再发生" [shape=doublecircle];
    "绝不只修症状" [shape=octagon, style=filled, fillcolor=red, fontcolor=white];

    "找到了直接原因" -> "能向上追踪一层吗？";
    "能向上追踪一层吗？" -> "反向追踪" [label="是"];
    "能向上追踪一层吗？" -> "绝不只修症状" [label="否"];
    "反向追踪" -> "这就是源头吗？";
    "这就是源头吗？" -> "反向追踪" [label="否——继续追踪"];
    "这就是源头吗？" -> "在源头修复" [label="是"];
    "在源头修复" -> "在每一层添加校验";
    "在每一层添加校验" -> "Bug 不可能再发生";
}
```

**绝不只在错误出现的地方修复。** 反向追踪，找到最初的触发点。

## 堆栈跟踪技巧

**在测试中：** 用 SLF4J `log.warn(... , new Throwable("..."))` 让 stack trace 走 logger，CI 能抓到
**操作之前：** 在危险操作之前记录日志，而不是在失败之后
**包含上下文：** 输入参数、当前事务状态、用户身份（userId）/ 请求上下文、时间戳
**捕获堆栈：** `new Throwable("debug").getStackTrace()` 能显示完整调用链；或在 Spring 异常处理器里统一打 `e.getCause()` 链

## 实际效果

按系统化追踪流程，4 层调用栈深处的 bug 平均能在 15-30 分钟内定位到源头并加上 4 层纵深防御，避免后续复发。
