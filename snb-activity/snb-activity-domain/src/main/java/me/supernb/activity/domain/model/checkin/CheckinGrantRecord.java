package me.supernb.activity.domain.model.checkin;

import java.time.Instant;
import java.time.LocalDate;

/// 已成功发放的补给记录(仅 `status=success`)。
///
/// @param grantMonth 发放所属自然月(该月第一天)
/// @param tier       档位("A"/"B"/"C")
/// @param grantedAt  实际发放时刻(状态变为 success 的时刻,取该行 `updated_at`)
public record CheckinGrantRecord(LocalDate grantMonth, String tier, Instant grantedAt) {
}
