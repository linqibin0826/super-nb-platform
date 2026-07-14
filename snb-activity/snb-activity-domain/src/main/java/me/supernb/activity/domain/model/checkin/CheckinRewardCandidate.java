package me.supernb.activity.domain.model.checkin;

import java.time.LocalDate;

/// 待处理发放候选(月度结算批处理内部流转,不对外暴露)。
public record CheckinRewardCandidate(long grantId, long userId, LocalDate grantMonth, String tier,
        long groupId, String notes, int attempts) {
}
