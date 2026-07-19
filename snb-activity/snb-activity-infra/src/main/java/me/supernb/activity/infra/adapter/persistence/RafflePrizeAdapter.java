package me.supernb.activity.infra.adapter.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.exception.RafflePrizeNotFoundException;
import me.supernb.activity.domain.model.raffle.RafflePrize;
import me.supernb.activity.domain.model.read.raffle.PersonWinsView;
import me.supernb.activity.domain.port.raffle.RafflePrizePort;
import me.supernb.activity.infra.adapter.persistence.dao.RafflePrizeJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RafflePrizeEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// RafflePrizePort 实现:查询为纯映射;管理端写路径经 TransactionTemplate。payload 随记录流出,消费侧红线见端口注释。
@Repository
public class RafflePrizeAdapter implements RafflePrizePort {

    private final RafflePrizeJpaRepository prizes;
    private final TransactionTemplate txTemplate;

    /// 构造:注入奖品仓储与事务管理器。
    public RafflePrizeAdapter(RafflePrizeJpaRepository prizes, PlatformTransactionManager txManager) {
        this.prizes = prizes;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public List<RafflePrize> byCampaign(long campaignId) {
        return prizes.findByCampaignIdOrderBySortOrderAscIdAsc(campaignId).stream()
                .map(RafflePrizeAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<RafflePrize> wonBy(long campaignId, long userId) {
        return prizes.findFirstByCampaignIdAndWinnerUserId(campaignId, userId)
                .map(RafflePrizeAdapter::toDomain);
    }

    @Override
    public List<PersonWinsView.Win> winsOf(long userId) {
        return prizes.findWinsOf(userId).stream()
                .map(r -> new PersonWinsView.Win(((Number) r[0]).longValue(), (String) r[1],
                        (Instant) r[2], (String) r[3], (String) r[4]))
                .toList();
    }

    @Override
    public long create(long campaignId, String tier, String displayName, String kind, String payload, int sortOrder) {
        return txTemplate.execute(status -> prizes.save(
                new RafflePrizeEntity(campaignId, tier, displayName, kind, payload, sortOrder)).getId());
    }

    @Override
    public void update(long prizeId, String tier, String displayName, String kind, String payload, int sortOrder) {
        txTemplate.executeWithoutResult(status -> {
            RafflePrizeEntity e = prizes.findById(prizeId).orElseThrow(RafflePrizeNotFoundException::new);
            e.update(tier, displayName, kind, payload, sortOrder);
        });
    }

    @Override
    public void updatePayload(long prizeId, String payload) {
        txTemplate.executeWithoutResult(status -> {
            RafflePrizeEntity e = prizes.findById(prizeId).orElseThrow(RafflePrizeNotFoundException::new);
            e.updatePayload(payload);
        });
    }

    @Override
    public void delete(long prizeId) {
        txTemplate.executeWithoutResult(status -> prizes.deleteById(prizeId));
    }

    @Override
    public List<Long> createBatch(long campaignId, String tier, String displayName, String kind,
            List<String> payloads, int sortOrderStart) {
        return txTemplate.execute(status -> {
            List<Long> ids = new ArrayList<>(payloads.size());
            int sortOrder = sortOrderStart;
            for (String payload : payloads) {
                ids.add(prizes.save(new RafflePrizeEntity(campaignId, tier, displayName, kind, payload, sortOrder)).getId());
                sortOrder++;
            }
            return ids;
        });
    }

    /// 实体 -> 领域记录。
    static RafflePrize toDomain(RafflePrizeEntity e) {
        return new RafflePrize(e.getId(), e.getTier(), e.getDisplayName(), e.getKind(), e.getPayload(),
                e.getSortOrder(), e.getWinnerUserId(), e.getAssignedAt());
    }
}
