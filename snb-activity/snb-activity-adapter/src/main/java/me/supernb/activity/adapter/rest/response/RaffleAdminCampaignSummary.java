package me.supernb.activity.adapter.rest.response;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;

/// 管理端期列表行(不含奖品明细,只带计数)。id 转 String,理由同 RaffleAdminPrize。
public record RaffleAdminCampaignSummary(String id, String name, String status, Instant entryOpenAt,
        Instant entryCloseAt, Instant drawAt, String gateType, BigDecimal gateAmount, int prizeCount) {

    public static RaffleAdminCampaignSummary of(RaffleCampaign c, int prizeCount) {
        return new RaffleAdminCampaignSummary(String.valueOf(c.id()), c.name(), c.status(), c.entryOpenAt(),
                c.entryCloseAt(), c.drawAt(), c.gateType().name(), c.gateAmount(), prizeCount);
    }
}
