# 代码规范

1. 遵循 Google Java 开发规范（格式化、命名、注释、类组织等）
2. 变量、类、接口命名必须准确反映意图和抽象层次：抽象概念用抽象命名（Repository、Port、QueryService），具体实现用具体命名（`R2StorageAdapter`、`ImageIoThumbnailAdapter`、`JdbcRechargeReadModel`）；禁止 Manager、Helper、Util 等模棱两可的业务类名
3. **所有方法（任何访问级别）必须编写 JavaDoc**，使用 `///` 风格（而非 `/** */`），内容用 markdown 语法（而非 HTML 标签）。例外：Lombok 生成的方法；测试方法以方法名即规格代替（见 testing/overview.md）
4. 禁止在代码中使用全类名（FQN），必须 `import` 导入（仅类名冲突时用 FQN 消歧义，如 Spring Data `Page` 与读视图 `Page` 同现的文件）
5. **优先使用 Lombok 注解**（`@Getter`、`@Setter`、`@Builder`、`@NoArgsConstructor` 等）生成样板代码，仅需要自定义逻辑时才手写。两条既定例外：domain 与 DTO 一律 record（不需要 Lombok）；**JPA 实体禁用 `@Data`**（全字段 equals/hashCode/toString 与懒加载、`@Id` 语义冲突，按需 `@Getter`、`@NoArgsConstructor(access = PROTECTED)`）
6. 注释、文档、commit 用中文；代码标识符用英文

## Record 类设计规范

### 工厂方法 vs @Builder

- **参数 ≤ 4 个**：使用静态工厂方法 `of()`，禁止使用 `@Builder`
- **参数 ≥ 5 个**：使用 `@Builder`，禁止同时提供 `of()`（API 一致性）
- 工厂方法统一命名 `of()`，语义化场景可用 `success()`、`failure()` 等（本仓先例：`DrawResult.prize()` / `consolation()`、`Page.of()`）
- 本仓落地口径：命令/读视图 record 目前多为 canonical 构造器直构（构造点单一、record 构造器即工厂）；**新增对外/跨层构造 API 的 record 按本条选型**

```java
// ✅ 简单 Record：使用 of()
public record PublishResult(String storageKey, int publishedCount) {
    public static PublishResult of(String storageKey, int publishedCount) {
        return new PublishResult(storageKey, publishedCount);
    }
}
```

### 防御性拷贝

Record 包含集合字段时，在紧凑构造器中做防御性拷贝：

```java
public record ValidationResult(boolean isValid, List<String> errors) {
    public ValidationResult {
        errors = errors != null ? List.copyOf(errors) : List.of();
    }
}
```

## 不可变集合规范

1. **空集合**：使用 `List.of()`、`Map.of()`、`Set.of()`，禁止 `Collections.emptyXxx()`
2. **防御性拷贝**：使用 `List.copyOf()`、`Map.copyOf()`、`Set.copyOf()`
3. **只读视图**：仅当需要返回内部集合的实时视图时使用 `Collections.unmodifiableXxx()`

## README 文档规范

- README 记录模块的**架构概览和核心设计**，不需要列举每个 API 端点
- 本仓库将开源：文档、注释、commit 里禁止出现真实凭据、服务器 IP、内网拓扑
