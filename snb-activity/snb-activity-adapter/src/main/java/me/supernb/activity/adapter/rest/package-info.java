/// activity 上下文的 REST Controller 所在包:单一入口 `ActivityController`,路径 `/activity/v1/*`——
/// 一个上下文一个 Controller,子域划分体现在注入的查询用例与派发的命令上,不体现在 Controller 数量上。
///
/// - `response/`:响应 DTO(record,一文件一类,当前只有 `DrawResponse`)
///
/// Controller 只做协议转换:写操作组装命令经 `CommandBus` 派发、结果映射进 `response/` 下的响应 DTO,
/// 读操作直接调用注入的查询用例、把 domain/model/read 的读视图原样当响应体返回,不另建 DTO;
/// 鉴权靠方法参数 `@CurrentUser UserProfile`,解析逻辑收在 snb-sub2api starter,本包不重复实现。
package me.supernb.activity.adapter.rest;
