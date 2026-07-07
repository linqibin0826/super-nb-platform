package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 当前无进行中的活动。映射 HTTP 404(NOT_FOUND)。
public class CampaignNotActiveException extends DomainException {

    /// 404:当前无进行中活动。
    public CampaignNotActiveException() {
        super("当前无进行中的活动", StandardErrorTrait.NOT_FOUND);
    }
}
