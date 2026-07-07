package me.supernb.activity.infra.adapter.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import me.supernb.activity.domain.exception.NoDrawsLeftException;
import me.supernb.activity.domain.model.Campaign;
import me.supernb.activity.domain.model.DrawEligibility;
import me.supernb.activity.domain.model.DrawResult;
import me.supernb.activity.domain.model.read.RawDraw;
import me.supernb.activity.domain.model.read.RawWinner;
import me.supernb.activity.domain.port.DrawPort;
import me.supernb.activity.domain.port.RechargeQueryPort;
import me.supernb.activity.infra.adapter.persistence.dao.DrawJpaRepository;
import me.supernb.activity.infra.adapter.persistence.dao.PrizeSlotJpaRepository;
import me.supernb.activity.infra.adapter.persistence.entity.DrawEntity;
import me.supernb.activity.infra.adapter.persistence.entity.PrizeSlotEntity;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/// DrawPort 实现:抽奖的原子事务在此。
///
/// 移植 activity-svc draw.py 的并发安全语义:事务内先 pg_advisory_xact_lock(userId) 串行化该用户,
/// 再现查剩余次数(充值总额走只读端口,弱一致可接受),然后 FOR UPDATE SKIP LOCKED 原子领槽;
/// 池空则记 $5 安慰奖占位(不发码)。用 TransactionTemplate 而非 @Transactional 注解,免受自调用代理坑。
@Repository
public class DrawAdapter implements DrawPort {

    private final DrawJpaRepository draws;
    private final PrizeSlotJpaRepository slots;
    private final TransactionTemplate txTemplate;
    private final RechargeQueryPort rechargePort;

    public DrawAdapter(DrawJpaRepository draws, PrizeSlotJpaRepository slots,
                       PlatformTransactionManager txManager, RechargeQueryPort rechargePort) {
        this.draws = draws;
        this.slots = slots;
        this.txTemplate = new TransactionTemplate(txManager);
        this.rechargePort = rechargePort;
    }

    @Override
    public DrawResult drawFor(Campaign campaign, long userId) {
        return txTemplate.execute(status -> doDraw(campaign, userId));
    }

    private DrawResult doDraw(Campaign campaign, long userId) {
        // 事务级 advisory lock:随事务结束自动释放,防并发超额
        draws.acquireUserXactLock(userId);

        BigDecimal total = rechargePort.totalRecharge(userId, campaign.startsAt(), campaign.endsAt());
        int used = countDraws(campaign.id(), userId);
        if (DrawEligibility.remainingDraws(total, used) <= 0) {
            throw new NoDrawsLeftException();
        }

        Optional<PrizeSlotEntity> claimed = slots.lockRandomAvailable(campaign.id());
        if (claimed.isPresent()) {
            PrizeSlotEntity slot = claimed.get();
            slot.claim(userId, Instant.now());
            draws.save(new DrawEntity(campaign.id(), userId, slot.getId(), slot.getAmount(),
                    slot.getRedeemCode(), false));
            return DrawResult.prize(slot.getAmount(), slot.getRedeemCode());
        }

        // 池空 → 安慰奖占位(不调 admin、不发码,人工发放)
        BigDecimal consolation = campaign.consolationAmount();
        draws.save(new DrawEntity(campaign.id(), userId, null, consolation, null, true));
        return DrawResult.consolation(consolation);
    }

    @Override
    public int countDraws(long campaignId, long userId) {
        return draws.countByCampaignIdAndUserId(campaignId, userId);
    }

    @Override
    public List<RawDraw> myRawDraws(long campaignId, long userId) {
        return draws.findTop100ByCampaignIdAndUserIdOrderByCreatedAtDesc(campaignId, userId).stream()
                .map(d -> new RawDraw(d.getAmount(), d.getRedeemCode(), d.isConsolation(),
                        d.getCreatedAt()))
                .toList();
    }

    @Override
    public List<RawWinner> recentRealWinners(long campaignId, int limit) {
        return draws.findByCampaignIdAndConsolationFalseOrderByCreatedAtDesc(campaignId, Limit.of(limit)).stream()
                .map(d -> new RawWinner(d.getUserId(), d.getAmount()))
                .toList();
    }
}
