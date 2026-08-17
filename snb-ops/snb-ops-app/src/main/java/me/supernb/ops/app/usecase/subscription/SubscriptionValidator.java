package me.supernb.ops.app.usecase.subscription;

import me.supernb.ops.domain.exception.OpsException;
import me.supernb.ops.domain.model.RefundStatus;
import me.supernb.ops.domain.model.SubStatus;
import me.supernb.ops.domain.port.repository.OpsSubscriptionRepository.SubscriptionData;

/// 订阅数据校验(建/改共用):service/status 必填;BANNED 必带封号时间;refundStatus 空默认 NONE。
final class SubscriptionValidator {

    private SubscriptionValidator() {
    }

    static SubscriptionData validate(SubscriptionData d) {
        if (d.service() == null) {
            throw OpsException.invalidInput("服务必填(CHATGPT/CLAUDE)");
        }
        if (d.status() == null) {
            throw OpsException.invalidInput("订阅状态必填");
        }
        if (d.status() == SubStatus.BANNED && d.bannedAt() == null) {
            throw OpsException.invalidInput("状态为已封时封号时间必填");
        }
        if (d.refundStatus() == null) {
            return new SubscriptionData(d.accountId(), d.service(), d.tier(), d.region(), d.cardPlatform(),
                    d.cardLast4(), d.registerIp(), d.currentIp(), d.registeredAt(), d.startedAt(),
                    d.nextBillingAt(), d.priceUsd(), d.status(), d.sub2apiAccountId(), d.sub2apiAccountName(),
                    d.bannedAt(), d.bannedWhilePaid(), RefundStatus.NONE, d.refundAmountUsd(), d.appealedAt(),
                    d.refundResolvedAt(), d.refundFollowUpAt(), d.refundNotes(), d.notes());
        }
        return d;
    }
}
