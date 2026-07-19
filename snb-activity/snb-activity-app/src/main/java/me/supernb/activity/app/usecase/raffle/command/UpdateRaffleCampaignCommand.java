package me.supernb.activity.app.usecase.raffle.command;

import dev.linqibin.commons.cqrs.Command;
import java.math.BigDecimal;
import java.time.Instant;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.WeightMode;

/// 管理端编辑标量字段,仅限开奖前(status=="active")的期。
public record UpdateRaffleCampaignCommand(long campaignId, String name, Instant entryOpenAt, Instant entryCloseAt,
        Instant drawAt, GateType gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
        WeightMode weightMode) implements Command<Void> {}
