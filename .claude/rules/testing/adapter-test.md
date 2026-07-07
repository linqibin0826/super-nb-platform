---
paths: snb-*/snb-*-adapter/**/src/test/**/*.java
---

# 契约测试规范（adapter）

## 形态

standalone MockMvc（**不起 Spring 上下文**，比 `@WebMvcTest` 更快更可控）：controller `new` 出来、app 用例全 mock、鉴权解析器手动挂载。`@Timeout` ≤ 2s。

```java
mvc = MockMvcBuilders.standaloneSetup(new GalleryController(
                commandBus, promptQueries, interactionQueries, generationQueries))
        .setCustomArgumentResolvers(new CurrentUserArgumentResolver(introspect))
        .build();
```

## 验证要点

| 维度 | 怎么验 |
|------|--------|
| 路由与参数绑定 | 真实路径发请求（`get("/gallery/v1/prompts").param("category", "style")`） |
| JSON 契约形状 | `jsonPath` 断言字段名与值（`$.items[0].likeCount`、`$.total`）——字段名是前端契约，改名即破坏 |
| 鉴权语义 | mock `Sub2apiIntrospectClient` 给两路：有效 token → 200；解析失败路径由 WiringTest 在真栈里验 401 |
| 400/401 次序 | `@RequestBody` 在 `@CurrentUser` 前：坏 JSON 先 400、再谈 401（该参数次序是契约，见 layers/adapter.md） |
| 写端点派发 | mock `CommandBus`；命令是 record，`when(bus.handle(new XxxCommand(精确参数)))` 用 equals 匹配即断言了派发参数 |

## 写端点样板（真实代码 `GalleryControllerTest`）

```java
@Test
void likeRequiresTokenAndDispatchesCommand() throws Exception {
    when(introspect.introspect("Bearer T")).thenReturn(Optional.of(new UserProfile(7, "user", "active")));
    when(commandBus.handle(new TogglePromptLikeCommand(5L, 7L, true)))
            .thenReturn(new LikeResult(4, true));
    mvc.perform(post("/gallery/v1/prompts/5/like").header("Authorization", "Bearer T"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.likeCount").value(4));
}
```

## 边界

- 只验协议转换：路由、绑定、DTO 形状、鉴权注入——**业务行为归 app 单测**，别在这层重复测
- problem+json 错误体的完整形状由 commons 保证，WiringTest 抽验即可，契约测试不逐个断言
