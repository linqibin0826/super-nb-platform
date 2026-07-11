package me.supernb.activity.infra.adapter.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.model.raffle.RaffleEntrant;
import me.supernb.activity.domain.model.raffle.RaffleEntryTicket;
import me.supernb.activity.domain.port.raffle.RaffleEntryPort;
import me.supernb.activity.infra.adapter.persistence.dao.RaffleEntryJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.RaffleEntryEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// RaffleEntryPort 实现:报名的幂等与取号原子性收在这里。
/// 事务内先 campaign 级 advisory lock 串行化取号(count+1),双唯一约束兜底;
/// 已报过直接返回既有参会证(already=true)。TransactionTemplate 显式包裹(禁 @Transactional 惯例)。
@Repository
public class RaffleEntryAdapter implements RaffleEntryPort {

    private final RaffleEntryJpaRepository entries;
    private final TransactionTemplate txTemplate;

    /// 构造:注入报名仓储与事务管理器。
    public RaffleEntryAdapter(RaffleEntryJpaRepository entries, PlatformTransactionManager txManager) {
        this.entries = entries;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    @Override
    public RaffleEntryTicket enter(long campaignId, long userId, BigDecimal gateValue,
            String clientIp, String userAgent) {
        return txTemplate.execute(status -> {
            entries.acquireCampaignXactLock(campaignId);
            Optional<RaffleEntryEntity> existing = entries.findByCampaignIdAndUserId(campaignId, userId);
            if (existing.isPresent()) {
                return new RaffleEntryTicket(existing.get().getEntryNo(), true);
            }
            int entryNo = entries.countByCampaignId(campaignId) + 1;
            entries.save(new RaffleEntryEntity(campaignId, userId, entryNo, gateValue, clientIp, userAgent));
            return new RaffleEntryTicket(entryNo, false);
        });
    }

    @Override
    public Optional<RaffleEntrant> find(long campaignId, long userId) {
        return entries.findByCampaignIdAndUserId(campaignId, userId)
                .map(e -> new RaffleEntrant(e.getUserId(), e.getEntryNo()));
    }

    @Override
    public int count(long campaignId) {
        return entries.countByCampaignId(campaignId);
    }

    @Override
    public List<RaffleEntrant> recent(long campaignId, int limit) {
        // DAO 固定 Top12(列席名单滚动上限);limit 只收窄,超过 12 也只回 12
        return entries.findTop12ByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .limit(limit)
                .map(e -> new RaffleEntrant(e.getUserId(), e.getEntryNo()))
                .toList();
    }

    @Override
    public List<RaffleEntrant> entrants(long campaignId) {
        return entries.findByCampaignId(campaignId).stream()
                .map(e -> new RaffleEntrant(e.getUserId(), e.getEntryNo()))
                .toList();
    }
}
