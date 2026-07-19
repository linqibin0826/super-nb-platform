package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 期已开奖或已作废,拒绝编辑/增删改奖品 → 409。
public class RaffleCampaignNotEditableException extends DomainException {
    public RaffleCampaignNotEditableException(String status) {
        super("当前状态为 " + status + ",不可编辑", StandardErrorTrait.CONFLICT);
    }
}
