/// gallery 库 8 张表的 Spring Data 仓储接口(`{Entity}JpaRepository`),一表一接口,均无 native SQL。
///
/// 派生方法与 JPQL 为主(含 theta join 内容寻址联查、`@Modifying` 批量 UPDATE/DELETE、接口投影);
/// PG 特有并发原语(advisory lock、`FOR UPDATE SKIP LOCKED`)是 activity 上下文的场景,本包不涉及——
/// 这里的并发语义靠 `@Modifying` 批量 DELETE(避开派生 delete 的 StaleStateException)与
/// 计数列原子 UPDATE(避开读-改-写丢更新)兜底,细则见 tech/jpa.md。
///
/// 只被 persistence 包下的两个聚合仓储适配器与 read 包下的 PromptReadAdapter 注入消费。
package me.supernb.gallery.infra.adapter.persistence.dao;
