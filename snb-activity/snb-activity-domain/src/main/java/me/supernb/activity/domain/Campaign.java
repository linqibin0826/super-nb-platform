package me.supernb.activity.domain;

import java.math.BigDecimal;
import java.time.Instant;

/// 活动期(一等公民,支持多期)。窗口 [startsAt, endsAt) 是抽奖资格与榜单的唯一口径。
///
/// @param id                活动 id
/// @param name              活动名
/// @param startsAt          起始(含)
/// @param endsAt            结束(不含,排他上界)
/// @param status            active | ended
/// @param consolationAmount 池空时安慰奖金额(元)
public record Campaign(
        long id,
        String name,
        Instant startsAt,
        Instant endsAt,
        String status,
        BigDecimal consolationAmount) {
}
