package me.supernb.activity.infra.adapter.read;

import java.util.List;
import java.util.Map;
import me.supernb.activity.domain.port.read.ReferralAchievementSignalPort;
import me.supernb.sub2api.referral.ReferralReadModel;
import org.springframework.stereotype.Component;

/// ReferralAchievementSignalPort 实现:零逻辑薄委托既有 ReferralReadModel。
@Component
public class ReferralAchievementSignalAdapter implements ReferralAchievementSignalPort {

    private final ReferralReadModel readModel;

    public ReferralAchievementSignalAdapter(ReferralReadModel readModel) {
        this.readModel = readModel;
    }

    @Override
    public Map<Long, List<Long>> allInviteeIdsByInviter() {
        return readModel.allInviteeIdsByInviter();
    }
}
