package me.supernb.activity.adapter.rest.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import me.supernb.activity.domain.model.checkin.CheckinRewardSummary;

/// 我的补给发放记录响应(前端接线计划契约):`{"grants":[...]}` 包裹,只含历史已成功发放行。
public record CheckinRewardsResponse(List<GrantLine> grants) {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    public static CheckinRewardsResponse of(List<CheckinRewardSummary> summaries) {
        return new CheckinRewardsResponse(summaries.stream().map(GrantLine::of).toList());
    }

    /// 单条发放记录。grantedAt 转 Asia/Shanghai 偏移量而非 Instant 默认 UTC "Z"
    /// (前端契约示例 "2026-06-30T23:58:00+08:00" 带 +08:00 偏移,2026-07-14 控制器裁决)。
    public record GrantLine(String month, String tier, String label, OffsetDateTime grantedAt) {
        static GrantLine of(CheckinRewardSummary s) {
            return new GrantLine(s.month(), s.tier(), s.label(), s.grantedAt().atZone(ZONE).toOffsetDateTime());
        }
    }
}
