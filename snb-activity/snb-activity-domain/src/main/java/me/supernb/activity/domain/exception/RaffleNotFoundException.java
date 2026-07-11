package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 期不存在/未开奖/已作废 → 404。
public class RaffleNotFoundException extends DomainException {

    /// 固定文案构造。
    public RaffleNotFoundException() {
        super("活动不存在或尚未开奖", StandardErrorTrait.NOT_FOUND);
    }
}
