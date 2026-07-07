package me.supernb.gallery.adapter.rest.response;

/// 删除结果响应。
///
/// @param ok 删除是否成功;失败(记录不存在或不归属本人)在 Handler 内直接抛异常走 404 响应,
///           不会构造出 ok=false 的这个响应体
public record DeleteResponse(boolean ok) {
}
