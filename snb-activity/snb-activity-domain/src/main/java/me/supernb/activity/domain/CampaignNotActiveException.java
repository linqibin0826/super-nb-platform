package me.supernb.activity.domain;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 当前无进行中的活动。映射 HTTP 404(NOT_FOUND)。
public class CampaignNotActiveException extends DomainException {

    public CampaignNotActiveException() {
        super("当前无进行中的活动", StandardErrorTrait.NOT_FOUND);
    }
}
