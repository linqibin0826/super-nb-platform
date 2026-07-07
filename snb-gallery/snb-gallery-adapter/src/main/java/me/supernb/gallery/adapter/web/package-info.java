/// gallery 上下文级 web 横切:`RateLimitFilter`(按 IP 的令牌桶限流,状态载体 `TokenBucket`)。
///
/// 只作用于 `/gallery/` 路径前缀,绝不把限流逻辑套到 activity 或付费模型 API 路径上
/// (那是上游 sub2api 的域)——这条边界由 Filter 内部的路径前缀判断保证。
package me.supernb.gallery.adapter.web;
