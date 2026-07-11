package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.util.Locale;
import me.supernb.activity.domain.model.read.raffle.MyRaffleView;

/// 本人视图响应(登录端点专用):myPrize.payload 是全系统唯一吐机密的位置,
/// 仅已开奖且本人中奖时非 null。
public record RaffleMeResponse(boolean entered, Integer entryNo, BigDecimal gateValue,
        BigDecimal gateAmount, boolean eligible, MyPrizeView myPrize) {

    /// 组装。
    public static RaffleMeResponse of(MyRaffleView v) {
        MyPrizeView prize = v.myPrize() == null ? null
                : new MyPrizeView(v.myPrize().tier(), v.myPrize().displayName(),
                        v.myPrize().kind().toLowerCase(Locale.ROOT), v.myPrize().payload());
        return new RaffleMeResponse(v.entered(), v.entryNo(), v.gateValue(), v.gateAmount(),
                v.eligible(), prize);
    }

    /// 我的奖品(含机密 payload)。
    public record MyPrizeView(String tier, String displayName, String kind, String payload) {}
}
