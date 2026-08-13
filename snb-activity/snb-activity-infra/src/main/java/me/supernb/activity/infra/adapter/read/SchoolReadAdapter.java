package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.port.read.SchoolReadPort;
import me.supernb.sub2api.recharge.RechargeReadModel;
import me.supernb.sub2api.referral.ReferralReadModel;
import org.springframework.stereotype.Component;

/// SchoolReadPort 实现:薄委托 sub2api 防腐层的 Recharge/Referral 两读模型——
/// 复用 Sub2apiRechargeAutoConfiguration 既有只读池,零新增装配条件、零新增连接
/// (照 ThursdayBucketReadAdapter / CheckinRechargeReadAdapter 惯例)。
@Component
public class SchoolReadAdapter implements SchoolReadPort {

    private final RechargeReadModel recharge;
    private final ReferralReadModel referral;

    /// 构造:注入两只读模型。
    public SchoolReadAdapter(RechargeReadModel recharge, ReferralReadModel referral) {
        this.recharge = recharge;
        this.referral = referral;
    }

    @Override
    public Optional<FirstCharge> firstCharge(long userId) {
        return recharge.firstCompletedOrder(userId)
                .map(o -> new FirstCharge(o.amountCny(), o.completedAt()));
    }

    @Override
    public int qualifiedInviteeCount(long inviterId, Instant start, Instant end, BigDecimal minAmountCny) {
        return referral.qualifiedInviteeCount(inviterId, start, end, minAmountCny);
    }

    @Override
    public List<InviterRank> topInviters(Instant start, Instant end, BigDecimal minAmountCny, int limit) {
        return referral.topInviters(start, end, minAmountCny, limit).stream()
                .map(r -> new InviterRank(r.name(), r.count()))
                .toList();
    }
}
