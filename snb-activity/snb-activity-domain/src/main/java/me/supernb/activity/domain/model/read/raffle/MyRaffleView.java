package me.supernb.activity.domain.model.read.raffle;

import java.math.BigDecimal;

/// 本人视图(登录 me 端点专用):资质进度+参会证+我的奖品。
/// myPrize.payload 是全系统唯一允许吐出机密的位置——仅已开奖且本人中奖时非 null。
public record MyRaffleView(boolean entered, Integer entryNo, BigDecimal gateValue,
        BigDecimal gateAmount, boolean eligible, MyPrize myPrize) {

    /// 我的奖品(含机密 payload)。
    public record MyPrize(String tier, String displayName, String kind, String payload) {}
}
