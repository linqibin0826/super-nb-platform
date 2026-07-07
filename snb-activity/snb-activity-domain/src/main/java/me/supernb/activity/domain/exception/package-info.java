/// 业务异常:继承 commons 的 DomainException,构造时携带 StandardErrorTrait 语义特征,
/// 由 commons 统一错误处理自动映射为 RFC 9457 problem+json 响应。
///
/// 当前两个:`CampaignNotActiveException`(NOT_FOUND → 404)、`NoDrawsLeftException`
/// (QUOTA_EXCEEDED → 409)。app 层拿到后直接传播,不包装不吞;不自建错误码体系,trait
/// 本身就是错误分类。
package me.supernb.activity.domain.exception;
