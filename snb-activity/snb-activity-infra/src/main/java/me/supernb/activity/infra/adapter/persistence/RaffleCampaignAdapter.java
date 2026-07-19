package me.supernb.activity.infra.adapter.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.exception.RaffleNotFoundException;
import me.supernb.activity.domain.model.raffle.GateType;
import me.supernb.activity.domain.model.raffle.RaffleCampaign;
import me.supernb.activity.domain.model.raffle.WeightMode;
import me.supernb.activity.domain.port.raffle.RaffleCampaignPort;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleCampaignJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleCampaignEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// RaffleCampaignPort 实现:查询为纯映射;管理端写路径经 TransactionTemplate(照 RaffleEntryAdapter 的 house style)。
@Repository
public class RaffleCampaignAdapter implements RaffleCampaignPort {

    private final RaffleCampaignJpaRepository campaigns;
    private final TransactionTemplate txTemplate;

    /// 构造:注入期仓储与事务管理器。
    public RaffleCampaignAdapter(RaffleCampaignJpaRepository campaigns, PlatformTransactionManager txManager) {
        this.campaigns = campaigns;
        this.txTemplate = new TransactionTemplate(txManager);
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

    @Override
    public List<RaffleCampaign> listAll() {
        return campaigns.findAll(Sort.by(Sort.Direction.DESC, "entryOpenAt")).stream()
                .map(RaffleCampaignAdapter::toDomain)
                .toList();
    }

    @Override
    public long create(String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            GateType gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
            WeightMode weightMode) {
        return txTemplate.execute(status -> campaigns.save(new RaffleCampaignEntity(name, entryOpenAt,
                entryCloseAt, drawAt, gateType.name(), gateAmount, gateFrom, minAccountAgeDays,
                weightMode.name())).getId());
    }

    @Override
    public void update(long id, String name, Instant entryOpenAt, Instant entryCloseAt, Instant drawAt,
            GateType gateType, BigDecimal gateAmount, Instant gateFrom, Integer minAccountAgeDays,
            WeightMode weightMode) {
        txTemplate.executeWithoutResult(status -> {
            RaffleCampaignEntity e = campaigns.findById(id).orElseThrow(RaffleNotFoundException::new);
            e.update(name, entryOpenAt, entryCloseAt, drawAt, gateType.name(), gateAmount, gateFrom,
                    minAccountAgeDays, weightMode.name());
        });
    }

    @Override
    public void cancel(long id) {
        txTemplate.executeWithoutResult(status -> {
            RaffleCampaignEntity e = campaigns.findById(id).orElseThrow(RaffleNotFoundException::new);
            e.cancel();
        });
    }

    /// 实体 -> 领域记录(枚举列存 TEXT,这里收敛成强类型)。
    static RaffleCampaign toDomain(RaffleCampaignEntity e) {
        return new RaffleCampaign(e.getId(), e.getName(), e.getEntryOpenAt(), e.getEntryCloseAt(),
                e.getDrawAt(), GateType.valueOf(e.getGateType()), e.getGateAmount(), e.getGateFrom(),
                e.getMinAccountAgeDays(), WeightMode.valueOf(e.getWeightMode()), e.getStatus(),
                e.getDrawnAt(), e.getEntrantCountAtDraw(), e.getDisqualifiedCount());
    }
}
