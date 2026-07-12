package me.supernb.activity.adapter.rest.response;

/// 报名回执:参会证号 + 是否幂等命中既有报名(already=true 未新增)。
/// 页面报名成功后自行重拉 current 刷新列席人数(响应不带 entrantCount——spec §6 勘误见 runbook 26)。
public record RaffleEnterResponse(int entryNo, boolean already) {}
