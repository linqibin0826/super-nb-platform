package me.supernb.activity.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/// 活动期。显式建模为独立记录(不是写死的配置),表结构支持多期共存。窗口
/// [startsAt, endsAt) 是抽奖资格与榜单统计的唯一口径。
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
