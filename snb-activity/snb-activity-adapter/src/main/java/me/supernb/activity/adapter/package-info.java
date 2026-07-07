/// activity 上下文的入站适配层:REST 契约与协议转换,不含业务判断。
///
/// - `rest/`:`ActivityController`(路径 `/activity/v1/*`)
/// - `rest/response/`:响应 DTO(record,一文件一类)
///
/// 六边形边界上的入站适配器:写操作把 HTTP 请求组装成命令经 `CommandBus` 派发,
/// 读操作直接调用注入的查询用例;鉴权靠方法参数 `@CurrentUser UserProfile`,
/// 由 snb-sub2api starter 的解析器完成,本包不写一行鉴权样板代码。
package me.supernb.activity.adapter;
