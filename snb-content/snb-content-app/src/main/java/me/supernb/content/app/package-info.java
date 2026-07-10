/// content 上下文的 app 层：用例编排，不感知持久化技术（ArchUnit `appIsPersistenceFree`）。
///
/// 唯一子包 `usecase/`，按子域分 `article/`、`category/`；写用例是 CommandHandler（经 CommandBus 派发），
/// 读用例是被 Controller 直接注入的查询服务。
package me.supernb.content.app;
