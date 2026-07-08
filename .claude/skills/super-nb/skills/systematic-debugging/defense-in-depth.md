# 纵深防御校验

## 概述

当你修复了一个由无效数据引起的 bug 时，在一个地方加校验似乎就够了。但这个单点检查可能会被不同的代码路径、重构或 mock 绕过。

**核心原则：** 在数据经过的每一层都做校验。让这个 bug 在结构上不可能发生。

## 为什么需要多层校验

单层校验："我们修了这个 bug"
多层校验："我们让这个 bug 不可能再发生"

不同层级能捕获不同问题：
- **入口校验**（adapter 层）捕获大多数 bug，最早拦截
- **业务逻辑校验**（app 层）捕获边界情况
- **VO / Domain 校验**（domain 层）保证聚合不变式
- **运行时守卫 / 测试守卫** 防止特定上下文的危险操作
- **调试日志** 在其他层级失效时提供取证

## 分层设防：以 gallery 生成"防空 prompt"为例（设计示意）

> **说明：** 本仓 gallery 生成接口**当前未**对空 `prompt` 分层校验——`CreateGenerationRequest` 无 `@NotBlank`、`GalleryController.createGeneration` 无 `@Valid`、`GenerationRepository.SaveGeneration` 是纯 record 无紧凑构造器、`CreateGenerationHandler` 也不做非空校验。下面用真实类名演示：**若要**给这条路径加纵深防御，四层该怎么布局。以下代码块均为**设计示意**，不是对现有实现的引用。

### 第 1 层：入口校验（adapter）
**目的：** 在 HTTP 边界拒绝明显无效的输入

```java
// CreateGenerationRequest record（DTO）
public record CreateGenerationRequest(
    @NotBlank @Size(max = 4000) String prompt,
    String size, int n, String quality, String status,
    Double cost, int elapsedMs, String groupName, Long keyId, String error,
    List<ImagePayload> outputImages, List<RefPayload> refImages) {
}

// Controller 显式加 @Valid 才会触发校验
@PostMapping("/me/generations")
public Created createGeneration(@Valid @RequestBody CreateGenerationRequest body, @CurrentUser UserProfile user) {
    return commandBus.handle(new CreateGenerationCommand(user.id(), body.prompt(), body.size(), body.n(), ...));
}
```

Spring 在反序列化后自动校验，违规返回 `400 Bad Request` + 字段级错误，不进 app 层。

### 第 2 层：业务逻辑校验（app）
**目的：** 确保数据对当前 use case 是合理的，处理领域特有约束

```java
@Override
public Created handle(CreateGenerationCommand cmd) {
    Objects.requireNonNull(cmd, "command cannot be null");

    // 用例约束：声明的张数必须与实际输出图数量一致
    if (cmd.outputImages() != null && cmd.n() != cmd.outputImages().size()) {
        throw new IllegalArgumentException("n 与 outputImages 数量不一致");
    }

    long id = repo.nextId();
    // ...（上传输出图 / 首图缩略图尽力而为 / 参考图内容寻址去重——真实 handle 的落库编排步骤）
    Instant createdAt = repo.save(new GenerationRepository.SaveGeneration(
            id, cmd.userId(), cmd.prompt(), cmd.size(), cmd.n(), /* ... */ List.of(), List.of()));
    return new Created(String.valueOf(id), createdAt);
}
```

### 第 3 层：VO / Domain 校验
**目的：** 保证聚合内不变式，无论从哪条路径构造对象

```java
// GenerationRepository 端口内的落库载荷 record（domain 层）
public record SaveGeneration(
        long id, long userId, String prompt, /* ...其余字段省略 */) {

    public SaveGeneration {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt 不能为空，实际：" + prompt);
        }
    }
}
```

domain record 的紧凑构造器是**最后一道防线**——即使前两层被绕过（如 mock、反射、Jackson 直接反序列化到聚合），紧凑构造器校验仍然能拦截。

### 第 4 层：测试守卫 / 集成断言
**目的：** 让回归测试每次跑 `./gradlew build` 时都验证防御链有效

```java
// 契约测试（adapter，standalone MockMvc 写法见 testing/adapter-test.md）
class GalleryGenerationValidationTest {

    private final CommandBus commandBus = mock(CommandBus.class);
    // ...（其余查询用例 mock 省略；GalleryController 的真实装配方式见现有 GalleryControllerTest）
    private final Sub2apiIntrospectClient introspect = mock(Sub2apiIntrospectClient.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new GalleryController(
                        commandBus, promptQueryService, interactionQueryService, generationQueryService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
                .build();
    }

    @Test
    void rejectsEmptyPromptAtAdapter() throws Exception {
        mvc.perform(post("/gallery/v1/me/generations")
                        .header("Authorization", "Bearer T")
                        .contentType(APPLICATION_JSON)
                        .content("{\"prompt\":\"\",\"n\":1}"))
                .andExpect(status().isBadRequest());
    }
}
```

```java
// 单测（app，零 Spring，直接 new + handle，见 testing/unit-test.md）
class CreateGenerationHandlerTest {

    private final GenerationRepository repo = mock(GenerationRepository.class);
    private final ImageStoragePort storage = mock(ImageStoragePort.class);
    private final ThumbnailPort thumbnails = mock(ThumbnailPort.class);
    private final CreateGenerationHandler handler = new CreateGenerationHandler(repo, storage, thumbnails);

    @Test
    void propagatesBlankPromptFromDomainRecord() {
        // adapter 校验被绕过时（如直接单测 Handler），domain record 仍能拦截
        when(repo.nextId()).thenReturn(1L);

        assertThatThrownBy(() -> handler.handle(new CreateGenerationCommand(
                7L, "", "1024x1024", 1, "medium", "done",
                null, 0, null, null, null, List.of(), List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

## 应用模式

当你发现一个 bug 时：

1. **追踪数据流** —— 错误值从哪里产生的？在哪里被使用？（参见 `root-cause-tracing.md`）
2. **标注所有检查点** —— 列出数据从 adapter 到 domain 经过的每一层
3. **在每一层添加校验** —— Bean Validation / 用例断言 / domain record 紧凑构造器 / 测试守卫
4. **测试每一层** —— 故意只发送绕过 adapter 校验的请求（直接调 Handler），验证下一层能否捕获

## 小结：这套分层若落地能挡住什么

假想 bug：空 `prompt` 若无人拦截，会一路穿到落库层触发 `500`（而非入口就 `400`）。上面四层布局各挡一类绕过：

- 第 1 层（入口）：`@NotBlank` + `@Valid` → 直接 400，多数请求在此拦下
- 第 2 层（应用）：Handler 兜底校验 → 挡住绕过 adapter 直调 Handler 的路径
- 第 3 层（domain）：落库 record 紧凑构造器 → 挡住 mock / 反射 / 反序列化绕过前两层的情况
- 第 4 层（测试）：契约测试 + 单测 → 让防御链每次 `./gradlew build` 都被验证、不退化

四层各挡一类，空字符串无论从哪条路径都被拦在最合适的层。（**再次强调：以上为设计示意，本仓当前尚未加这套校验**。）

## 关键洞察

四个层级缺一不可。每一层都捕获了其他层遗漏的 bug：
- adapter 校验防住了大量 HTTP 入参问题
- app 层断言防住了绕过 adapter 直调 Handler 的情况
- domain record 的紧凑构造器在 Jackson 直接反序列化或测试直接 `new` 对象时仍然守护
- 契约测试 + 单测守住整条防御链不退化

**不要止步于一个校验点。** 在每一层都添加检查，让 bug 在结构上不可能再发生。
