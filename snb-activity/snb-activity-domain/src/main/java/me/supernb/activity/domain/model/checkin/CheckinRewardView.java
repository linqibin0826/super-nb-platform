package me.supernb.activity.domain.model.checkin;

import java.time.LocalDate;

/// 用户本人视角的发放记录(GET /checkin/rewards)。
public record CheckinRewardView(LocalDate grantMonth, String tier, String status) {
}
