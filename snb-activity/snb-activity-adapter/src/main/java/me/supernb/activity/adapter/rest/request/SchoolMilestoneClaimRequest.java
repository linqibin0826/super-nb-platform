package me.supernb.activity.adapter.rest.request;

/// 领取开学季里程碑卡的请求体。tier=人数档(1/3/6),白名单校验在 handler
/// (KFC 档 10 无 claim 路,人工私聊发放)。
public record SchoolMilestoneClaimRequest(Integer tier) {
}
