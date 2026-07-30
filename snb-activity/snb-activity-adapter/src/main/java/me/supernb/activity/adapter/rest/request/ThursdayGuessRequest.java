package me.supernb.activity.adapter.rest.request;

/// 猜桶竞猜提交请求。
///
/// @param guess 猜的份数;范围合法性在 handler 里按桶上限重算,不信客户端
public record ThursdayGuessRequest(Integer guess) {
}
