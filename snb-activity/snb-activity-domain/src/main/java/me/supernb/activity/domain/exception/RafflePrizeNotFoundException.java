package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 奖品件不存在或不属于指定期 → 404。
public class RafflePrizeNotFoundException extends DomainException {
    public RafflePrizeNotFoundException() {
        super("奖品件不存在", StandardErrorTrait.NOT_FOUND);
    }
}
