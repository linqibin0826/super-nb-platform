package me.supernb.activity.app.usecase.raffle.command;

import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.domain.exception.RaffleAdminValidationException;
import me.supernb.activity.domain.exception.RaffleCampaignNotEditableException;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;

/// 管理端命令共用的校验:标量字段规则、"期存在且可编辑(status==active)"守卫。
final class RaffleAdminValidation {

    private RaffleAdminValidation() {}

    static void validateScalars(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            BigDecimal gateAmount, Integer minAccountAgeDays) {
        if (name == null || name.isBlank()) {
            throw new RaffleAdminValidationException("名称不能为空");
        }
        if (!entryOpenAt.isBefore(entryCloseAt)) {
            throw new RaffleAdminValidationException("报名截止时间必须晚于开放时间");
        }
        if (!entryCloseAt.isBefore(drawAt)) {
            throw new RaffleAdminValidationException("开奖时间必须晚于报名截止时间");
        }
        if (gateAmount == null || gateAmount.signum() <= 0) {
            throw new RaffleAdminValidationException("门槛金额必须大于 0");
        }
        if (minAccountAgeDays != null && minAccountAgeDays < 0) {
            throw new RaffleAdminValidationException("账龄门槛不能为负数");
        }
    }

    static RaffleCampaign requireEditableCampaign(RaffleCampaignPort campaignPort, long campaignId) {
        RaffleCampaign c = campaignPort.byId(campaignId).orElseThrow(RaffleNotFoundException::new);
        if (!"active".equals(c.status())) {
            throw new RaffleCampaignNotEditableException(c.status());
        }
        return c;
    }
}
