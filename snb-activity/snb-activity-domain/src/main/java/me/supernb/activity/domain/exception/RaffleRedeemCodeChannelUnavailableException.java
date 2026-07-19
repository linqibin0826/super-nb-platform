package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// sub2api admin 通道未配置(缺 SUB2API_ADMIN_KEY),兑换码签发不可用 → 503。
public class RaffleRedeemCodeChannelUnavailableException extends DomainException {
    public RaffleRedeemCodeChannelUnavailableException() {
        super("sub2api admin 通道未配置(缺 SUB2API_ADMIN_KEY)", StandardErrorTrait.DEP_UNAVAILABLE);
    }
}
