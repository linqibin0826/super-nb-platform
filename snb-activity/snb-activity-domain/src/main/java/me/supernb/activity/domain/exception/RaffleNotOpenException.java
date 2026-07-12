package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 不在报名期(未开放/已截止/已开奖) → 404(与 CampaignNotActiveException 同语义家族)。
public class RaffleNotOpenException extends DomainException {

    /// 固定文案构造。
    public RaffleNotOpenException() {
        super("当前不在报名期", StandardErrorTrait.NOT_FOUND);
    }
}
