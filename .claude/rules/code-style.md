# 代码规范

1. 遵循 Google Java 开发规范（格式化、命名、注释、类组织等）
2. 命名必须准确反映意图和抽象层次：抽象概念用抽象命名（Port、Repository），具体实现用具体命名（`ImageIoThumbnailAdapter`、`JdbcRechargeReadModel`）；禁止 Manager、Helper、Util 等模棱两可的业务类名
3. JavaDoc 用 `///` 风格（而非 `/** */`），内容用 markdown 语法：**公共类型必须有类级 `///` 说明职责**（一两句，含关键约束/坑）；方法级按需——行为不显然、有并发/事务/幂等语义、有踩坑约束的必须写，自明的 getter 式方法不强制
4. 注释、文档、commit 用中文；代码标识符用英文
5. 禁止在代码中使用全类名（FQN），必须 `import`（仅类名冲突时例外）
6. Lombok 克制使用：domain 与 DTO 一律 record；JPA 实体按需 `@Getter`、`@NoArgsConstructor(access = PROTECTED)` 等，**实体禁用 `@Data`**（全字段 equals/hashCode/toString 与懒加载、@Id 语义冲突）

## Record 设计规范

- **参数 ≤ 4 个**：静态工厂 `of()`，禁止 `@Builder`
- **参数 ≥ 5 个**：`@Builder`，不再同时提供 `of()`
- 语义化场景可用 `success()`、`failure()` 等命名
- 含集合字段时，紧凑构造器做防御性拷贝：

```java
public record ValidationResult(boolean isValid, List<String> errors) {
    public ValidationResult {
        errors = errors != null ? List.copyOf(errors) : List.of();
    }
}
```

## 不可变集合规范

1. 空集合用 `List.of()` / `Map.of()` / `Set.of()`，禁止 `Collections.emptyXxx()`
2. 防御性拷贝用 `List.copyOf()` 等
3. 仅需要实时只读视图时才用 `Collections.unmodifiableXxx()`

## README / 文档

- README 记录架构概览与核心设计，不逐个列举 API 端点
- 本仓库将开源：文档、注释、commit 里禁止出现真实凭据、服务器 IP、内网拓扑
