package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 当前无进行中的活动。携带 NOT_FOUND 语义特征,经 commons 统一错误处理映射为 HTTP 404。
public class CampaignNotActiveException extends DomainException {

    /// 404:当前无进行中的活动。
    public CampaignNotActiveException() {
        super("当前无进行中的活动", StandardErrorTrait.NOT_FOUND);
    }
}
