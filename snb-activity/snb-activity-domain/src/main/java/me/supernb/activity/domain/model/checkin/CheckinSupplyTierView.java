package me.supernb.activity.domain.model.checkin;

import java.math.BigDecimal;

/// 补给资格单档展示状态(GET /checkin/status.supply.tiers[] 的领域来源)。
///
/// @param tier          档位("A"/"B"/"C")
/// @param label         展示标签
/// @param conditionText 达标条件说明文案
/// @param thresholdCny  阈值(元)
/// @param state         "armed"(已达标)|"progress"(下一个目标档)|"dim"(远未达标)
/// @param statusText    成品状态文案
public record CheckinSupplyTierView(String tier, String label, String conditionText, BigDecimal thresholdCny,
        String state, String statusText) {
}
