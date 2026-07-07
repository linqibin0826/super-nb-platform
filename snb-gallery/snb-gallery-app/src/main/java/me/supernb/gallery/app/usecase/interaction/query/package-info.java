/// 互动子域的只读查询用例:`InteractionQueryService`,被 adapter 直接注入,不经 CommandBus。
///
/// 无接口(按 tech/port-service.md 的 QueryService 选型:纯查询编排、无副作用,
/// 不需要抽象;真要 mock,直接 mock 它调用的 `InteractionRepository`),内部委托该端口
/// 完成「我的收藏」分页与批量互动态回填。
package me.supernb.gallery.app.usecase.interaction.query;
