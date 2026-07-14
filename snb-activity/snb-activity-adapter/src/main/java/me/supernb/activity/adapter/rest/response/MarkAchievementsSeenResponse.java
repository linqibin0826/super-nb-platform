package me.supernb.activity.adapter.rest.response;

/// POST /activity/v1/checkin/achievements/seen 响应体:`{"acknowledged":1}`。
public record MarkAchievementsSeenResponse(int acknowledged) {
}
