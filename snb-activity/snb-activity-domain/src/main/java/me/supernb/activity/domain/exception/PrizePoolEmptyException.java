package me.supernb.activity.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 奖池当日已抽空:拒抽、事务回滚不落记录、用户次数保留到补货后(🪦 取代「$5 安慰奖占位
/// +人工发放」,站长 2026-07-29 拍板退役)。携带 CONFLICT 语义特征;与同为冲突语义的
/// [NoDrawsLeftException] 由前端凭 message 中的「抽空」字样区分。
public class PrizePoolEmptyException extends DomainException {

    /// 固定文案构造;message 含「抽空」供前端分流,改动须与活动页 catch 分支同步。
    public PrizePoolEmptyException() {
        super("今日奖池已抽空,补货后再来", StandardErrorTrait.CONFLICT);
    }
}
