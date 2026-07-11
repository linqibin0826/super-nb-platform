package me.supernb.activity.infra.adapter.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleCampaignJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleCampaignEntity;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;

/// RaffleCampaignPort 实现:纯查询映射,无事务语义。
@Repository
public class RaffleCampaignAdapter implements RaffleCampaignPort {

    private final RaffleCampaignJpaRepository campaigns;

    /// 构造:注入期仓储。
    public RaffleCampaignAdapter(RaffleCampaignJpaRepository campaigns) {
        this.campaigns = campaigns;
    }

    @Override
    public Optional<RaffleCampaign> current() {
        return campaigns.findFirstByStatusNotOrderByEntryOpenAtDesc("cancelled").map(RaffleCampaignAdapter::toDomain);
    }

    @Override
    public Optional<RaffleCampaign> byId(long id) {
        return campaigns.findById(id).map(RaffleCampaignAdapter::toDomain);
    }

    @Override
    public List<RaffleCampaign> dueForDraw(Instant now) {
        return campaigns.findByStatusAndDrawAtLessThanEqual("active", now).stream()
                .map(RaffleCampaignAdapter::toDomain)
                .toList();
    }

    @Override
    public List<RaffleCampaign> drawnHistory(int limit) {
        return campaigns.findByStatusOrderByDrawnAtDesc("drawn", Limit.of(limit)).stream()
                .map(RaffleCampaignAdapter::toDomain)
                .toList();
    }

    /// 实体 -> 领域记录(枚举列存 TEXT,这里收敛成强类型)。
    static RaffleCampaign toDomain(RaffleCampaignEntity e) {
        return new RaffleCampaign(e.getId(), e.getName(), e.getEntryOpenAt(), e.getEntryCloseAt(),
                e.getDrawAt(), GateType.valueOf(e.getGateType()), e.getGateAmount(), e.getGateFrom(),
                e.getMinAccountAgeDays(), WeightMode.valueOf(e.getWeightMode()), e.getStatus(),
                e.getDrawnAt(), e.getEntrantCountAtDraw(), e.getDisqualifiedCount());
    }
}
