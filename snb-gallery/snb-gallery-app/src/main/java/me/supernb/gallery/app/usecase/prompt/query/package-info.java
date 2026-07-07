/// 提示词子域的只读查询用例:`PromptQueryService`,被 adapter 直接注入,不经 CommandBus。
///
/// 无接口(按 tech/port-service.md 的 QueryService 选型),内部委托 domain/port 的
/// `PromptReadPort` 完成列表分页、详情、三轴类目树三类查询,domain/app 两层均不感知持久化技术。
package me.supernb.gallery.app.usecase.prompt.query;
