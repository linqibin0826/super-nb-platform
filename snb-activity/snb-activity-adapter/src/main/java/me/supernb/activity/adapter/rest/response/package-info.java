/// activity 上下文的响应 DTO:record,一文件一类,专给 Controller 序列化成 JSON 响应体。
///
/// 当前只有 `DrawResponse`,对应写端点 `/draw` 的结果映射——只读端点不进这个包,
/// 它们直接把 domain/model/read 的读视图当响应体返回,不需要再包一层 DTO。
package me.supernb.activity.adapter.rest.response;
