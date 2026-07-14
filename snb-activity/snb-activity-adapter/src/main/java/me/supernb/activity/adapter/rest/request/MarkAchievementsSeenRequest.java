package me.supernb.activity.adapter.rest.request;

import java.util.List;

/// POST /activity/v1/checkin/achievements/seen 请求体:`{"codes":["api_calls_2"]}`。
public record MarkAchievementsSeenRequest(List<String> codes) {
}
