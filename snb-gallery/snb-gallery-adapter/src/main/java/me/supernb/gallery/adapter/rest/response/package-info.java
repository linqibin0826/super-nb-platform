/// gallery 上下文的响应 DTO:record,一文件一类,专给 Controller 序列化成 JSON 响应体。
///
/// 当前只有 `DeleteResponse`,对应写端点 `DELETE /me/generations/{generationId}` 的结果映射——
/// 其余写端点直接把 app 层的写结果(`LikeResult`/`FavResult`/`Created`)当响应体返回,
/// 只读端点则直接把 domain/model/read 的读视图当响应体返回,都不需要再包一层适配层 DTO。
package me.supernb.gallery.adapter.rest.response;
