package me.supernb.activity.infra.adapter.read;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import me.supernb.activity.domain.model.read.ReferralInviteEntry;
import me.supernb.activity.domain.model.read.ReferralRechargeEntry;
import me.supernb.activity.domain.port.read.ReferralReadPort;
import me.supernb.sub2api.referral.ReferralReadModel;
import org.springframework.stereotype.Component;

/// ReferralReadPort 实现:薄适配,委托 snb-sub2api 的 ReferralReadModel,把上游行映射为
/// activity 自己的读视图(name 已在 starter 层脱敏)。
@Component
public class ReferralReadAdapter implements ReferralReadPort {

    private final ReferralReadModel readModel;

    /// 构造:注入 starter 提供的拉新双榜读模型。
    public ReferralReadAdapter(ReferralReadModel readModel) {
        this.readModel = readModel;
    }

    /// 委托 starter 读模型取充值榜,映射为 [ReferralRechargeEntry]。
    @Override
    public List<ReferralRechargeEntry> rechargeBoard(Instant start, Instant end, BigDecimal cap, int limit) {
        return readModel.rechargeBoard(start, end, cap, limit).stream()
                .map(r -> new ReferralRechargeEntry(r.name(), r.total(), r.capped()))
                .toList();
    }

    /// 委托 starter 读模型取人数榜,映射为 [ReferralInviteEntry]。
    @Override
    public List<ReferralInviteEntry> inviteBoard(long newcomerGroupId, int limit) {
        return readModel.inviteBoard(newcomerGroupId, limit).stream()
                .map(r -> new ReferralInviteEntry(r.name(), r.count()))
                .toList();
    }
}
