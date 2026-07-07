/// gallery CQRS 读投影层:`{Entity}ReadPort` 的适配器实现 + Entity → 读视图的手写映射。
///
/// `PromptReadAdapter` 实现 domain/port/read 的 `PromptReadPort`,标 `@Repository`,直接查
/// `gallery.prompt`/`gallery.category`;列表分页靠注入的 `EntityManager` 现拼动态 HQL——
/// 过滤(类目/关键字)与排序的组合不是固定形状,套不进派生方法或静态 `@Query`。`PromptMapper`
/// 把 `PromptEntity` 转成 domain/model/read 的读视图,本包内的 `PromptReadAdapter` 与
/// `persistence` 包的 `InteractionRepositoryAdapter`(「我的收藏」投影)共用同一份映射。
///
/// 手写映射是本仓刻意选择,不引 MapStruct——仓库规模不值得为映射分摊生成式框架的维护成本,
/// 手写换来编译期报错更直接、IDE 跳转不会落进生成代码,命名/选型细则见 tech/port-service.md。
package me.supernb.gallery.infra.adapter.read;
