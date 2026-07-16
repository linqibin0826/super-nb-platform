package me.supernb.guide.domain.exception;

import dev.linqibin.commons.error.DomainException;
import dev.linqibin.commons.error.trait.StandardErrorTrait;

/// 引导领域异常:trait 由 commons 引擎映射 HTTP。
public class GuideException extends DomainException {

    private GuideException(String message, StandardErrorTrait trait) {
        super(message, trait);
    }

    /// 引导 key 不合法(422)。
    public static GuideException invalidKey(String key) {
        return new GuideException("引导 key 不合法: " + key, StandardErrorTrait.RULE_VIOLATION);
    }

    /// 单用户引导已读记录数超上限(429,防脚本刷不同 key 膨胀 guide_ack 表)。
    public static GuideException tooMany(int max) {
        return new GuideException("引导已读记录已达上限 " + max, StandardErrorTrait.QUOTA_EXCEEDED);
    }
}
