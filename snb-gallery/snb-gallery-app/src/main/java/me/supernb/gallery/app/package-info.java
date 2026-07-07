/// gallery 上下文的用例编排层:校验入参 → 调用 domain/port 定义的端口 → 组装读视图/写结果,
/// 全部内容收在唯一子包 `usecase/`(按子域 `prompt`/`interaction`/`generation` 继续分包,见该包说明)。
///
/// 六边形架构里 adapter 与 infra 之间的编排层:向下只依赖 domain(端口接口 + 读视图),
/// 不感知持久化技术(禁止 import jakarta.persistence / hibernate / spring-data,ArchUnit 门禁
/// `appIsPersistenceFree`);向上被 adapter 经 `CommandBus`(写)或直接注入(读)消费,
/// 本层不写任何 `@Transactional`——事务边界收在 infra 用 `TransactionTemplate` 显式管理。
package me.supernb.gallery.app;
