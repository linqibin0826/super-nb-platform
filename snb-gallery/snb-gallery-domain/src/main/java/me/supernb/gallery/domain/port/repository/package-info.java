/// 聚合持久化端口(`{Entity}Repository`),写为主,由 infra 的 `{Entity}RepositoryAdapter` 实现。
///
/// - [GenerationRepository]:生成历史聚合(4 表一事务),身份即雪花 id,经 `nextId()` 预分配
/// - [InteractionRepository]:点赞/收藏成员关系 + 反规范化计数(±1)
package me.supernb.gallery.domain.port.repository;
