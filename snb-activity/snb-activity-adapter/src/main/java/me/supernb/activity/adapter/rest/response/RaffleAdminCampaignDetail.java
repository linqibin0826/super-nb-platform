package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.RafflePrize;

/// 管理端期详情:标量字段全量 + 奖品明细(payload 明文)。id 转 String,理由同 RaffleAdminPrize。
public record RaffleAdminCampaignDetail(String id, String name, String status, Instant entryOpenAt,
        Instant entryCloseAt, Instant drawAt, String gateType, BigDecimal gateAmount, Instant gateFrom,
        Integer minAccountAgeDays, String weightMode, Instant drawnAt, Integer entrantCountAtDraw,
        Integer disqualifiedCount, List<RaffleAdminPrize> prizes) {

    public static RaffleAdminCampaignDetail of(RaffleCampaign c, List<RafflePrize> prizes) {
        return new RaffleAdminCampaignDetail(String.valueOf(c.id()), c.name(), c.status(), c.entryOpenAt(),
                c.entryCloseAt(), c.drawAt(), c.gateType().name(), c.gateAmount(), c.gateFrom(),
                c.minAccountAgeDays(), c.weightMode().name(), c.drawnAt(), c.entrantCountAtDraw(),
                c.disqualifiedCount(), prizes.stream().map(RaffleAdminPrize::of).toList());
    }
}
